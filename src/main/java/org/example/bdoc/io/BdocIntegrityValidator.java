package org.example.bdoc.io;

import org.example.bdoc.model.*;

import java.io.IOException;
import java.util.*;

/**
 * Проверяет ссылочную целостность документа: то, что раньше
 * гарантировала XSD-схема (xs:ID / xs:IDREF), теперь проверяется
 * явным кодом, потому что JSON/CBOR не имеют встроенного механизма
 * проверки ссылок.
 *
 * Валидатор проходит по всем страницам документа (принудительно
 * загружая их через loadPage), поэтому вызов validate() снимает
 * все преимущества ленивой загрузки для этого прогона — он
 * предназначен для явной проверки (например, перед экспортом
 * или в тестах), а не для вызова на каждое открытие файла.
 */
public final class BdocIntegrityValidator {

    private Set<String> collectCharacterStyleIds(StylesCatalog styles) {
        Set<String> ids = new HashSet<>();
        for (CharacterStyle style : styles.getCharacterStyles()) {
            ids.add(style.getId());
        }
        return ids;
    }

    public void validate(DocumentHandle document) {
        List<String> errors = new ArrayList<>();

        Set<String> storyIds = document.getStoryIds();
        Set<String> knownParagraphStyleIds = collectParagraphStyleIds(document.getStyles());

        validateStories(document, storyIds, knownParagraphStyleIds, errors);
        validatePages(document, storyIds, errors);

        if (!errors.isEmpty()) {
            throw new BdocValidationException(
                    "BDoc document failed integrity validation", errors);
        }
    }

    private void validateStories(DocumentHandle document, Set<String> storyIds,
                                 Set<String> knownParagraphStyleIds, List<String> errors) {

        Set<String> knownCharacterStyleIds = collectCharacterStyleIds(document.getStyles());

        for (String storyId : storyIds) {
            StoryModel story = document.getStory(storyId);

            if (story.getParagraphs().isEmpty()) {
                errors.add("Story '" + storyId + "' has no paragraphs " +
                        "(a story must contain at least one paragraph)");
            }

            for (int i = 0; i < story.getParagraphs().size(); i++) {
                Paragraph paragraph = story.getParagraphs().get(i);
                String styleRef = paragraph.getStyleRef();
                if (styleRef != null && !knownParagraphStyleIds.contains(styleRef)) {
                    errors.add("Story '" + storyId + "', paragraph[" + i + "]: " +
                            "styleRef '" + styleRef + "' does not match any ParagraphStyle in styles.json");
                }

                for (int j = 0; j < paragraph.getSpans().size(); j++) {
                    String charStyleRef = paragraph.getSpans().get(j).getCharacterStyleRef();
                    if (charStyleRef != null && !knownCharacterStyleIds.contains(charStyleRef)) {
                        errors.add("Story '" + storyId + "', paragraph[" + i + "], span[" + j + "]: " +
                                "characterStyleRef '" + charStyleRef + "' does not match any CharacterStyle in styles.json");
                    }
                }
            }
        }
    }

    private void validatePages(DocumentHandle document, Set<String> storyIds, List<String> errors) {
        Set<Integer> pageIndices = document.getPageIndices();
        if (pageIndices.isEmpty()) {
            errors.add("Document has no pages: a document must contain at least one page");
            return;
        }
        validatePageIndexSequence(pageIndices, errors);

        Set<String> pageIdsSeen = new HashSet<>();
        for (int index : new TreeSet<>(pageIndices)) {
            PageModel page;
            try {
                page = document.loadPage(index);
            } catch (IOException e) {
                errors.add("Page index " + index + ": failed to load page file: " + e.getMessage());
                continue;
            }
            if (!pageIdsSeen.add(page.getId())) {
                errors.add("Duplicate page id: " + page.getId() + ": page ids must be unique across the document");
            }

            // Новая проверка: templateRef должен указывать на существующий MasterPage
            if (page.getTemplateRef() != null && document.getMasterPage(page.getTemplateRef()) == null) {
                errors.add("Page " + page.getId() + " has templateRef " + page.getTemplateRef()
                        + " which does not match any MasterPage in templates.json");
            }

            validateSinglePage(page, storyIds, document, errors);
        }
    }

    private void validatePageIndexSequence(Set<Integer> pageIndices, List<String> errors) {
        int max = Collections.max(pageIndices);
        for (int expected = 1; expected <= max; expected++) {
            if (!pageIndices.contains(expected)) {
                errors.add("Missing page with index " + expected +
                        " (page indices must form a continuous sequence starting at 1)");
            }
        }
    }

    private void validateSinglePage(PageModel page, Set<String> storyIds, DocumentHandle document, List<String> errors) {
        String pageLabel = "Page " + page.getId() + " (index " + page.getIndex() + ")";
        if (page.getLayers().isEmpty()) {
            errors.add(pageLabel + " has no layers: a page must contain at least one layer");
        }

        Set<String> layerIds = new HashSet<>();
        for (LayerModel layer : page.getLayers()) {
            if (!layerIds.add(layer.getId())) {
                errors.add(pageLabel + ": duplicate layer id " + layer.getId());
            }
        }

        MasterPage masterPage = document.getMasterPage(page.getTemplateRef());
        Set<String> masterObjectIds = masterPage != null
                ? masterPage.getObjects().stream().map(BdocObject::getId).collect(java.util.stream.Collectors.toSet())
                : Set.of();

        Set<String> objectIds = new HashSet<>();
        for (BdocObject object : page.getObjects()) {
            if (!objectIds.add(object.getId())) {
                errors.add(pageLabel + ": duplicate object id " + object.getId());
            }
            if (!layerIds.contains(object.getLayerRef())) {
                errors.add(pageLabel + ": object " + object.getId() + " has layerRef " + object.getLayerRef()
                        + " which does not match any layer on this page");
            }

            // Новая проверка: masterSourceId должен указывать на реально существующий объект мастера
            if (object.isMasterOverride()) {
                if (masterPage == null) {
                    errors.add(pageLabel + ": object " + object.getId() + " has masterSourceId "
                            + object.getMasterSourceId() + " but page has no templateRef");
                } else if (!masterObjectIds.contains(object.getMasterSourceId())) {
                    errors.add(pageLabel + ": object " + object.getId() + " has masterSourceId "
                            + object.getMasterSourceId() + " which does not match any object on MasterPage "
                            + masterPage.getId());
                }
            }

            validateObjectSpecifics(object, pageLabel, storyIds, document, errors);
        }
    }

    private void validateObjectSpecifics(BdocObject object, String pageLabel, Set<String> storyIds,
                                         DocumentHandle document, List<String> errors) {
        if (object instanceof TextFrame textFrame) {
            if (!storyIds.contains(textFrame.getStoryRef())) {
                errors.add(pageLabel + ": textFrame '" + textFrame.getId() + "' has storyRef '" +
                        textFrame.getStoryRef() + "' which does not match any story");
            }
        } else if (object instanceof ImageFrame imageFrame) {
            if (!document.hasResource(imageFrame.getAssetRef())) {
                errors.add(pageLabel + ": imageFrame '" + imageFrame.getId() + "' has assetRef '" +
                        imageFrame.getAssetRef() + "' which does not exist in resources/");
            }
        } else if (object instanceof VectorShape shape) {
            validateShapeGeometry(shape, pageLabel, errors);
        }
    }

    private void validateShapeGeometry(VectorShape shape, String pageLabel, List<String> errors) {
        Geometry g = shape.getGeometry();
        if (g.getWidth() < 0 || g.getHeight() < 0) {
            errors.add(pageLabel + ": vectorShape '" + shape.getId() +
                    "' has negative geometry width/height");
        }
        Set<String> knownShapeTypes = Set.of("rectangle", "rounded-rectangle", "line");
        if (!knownShapeTypes.contains(shape.getShapeType())) {
            errors.add(pageLabel + ": vectorShape '" + shape.getId() +
                    "' has unknown shapeType '" + shape.getShapeType() + "'");
        }
    }

    private Set<String> collectParagraphStyleIds(StylesCatalog styles) {
        Set<String> ids = new HashSet<>();
        for (ParagraphStyle style : styles.getParagraphStyles()) {
            ids.add(style.getId());
        }
        return ids;
    }
}