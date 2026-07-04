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

        // Новая проверка: readingOrder ссылается только на реально существующие объекты
        // (страницы или её MasterPage), sequence и targetObjectId не повторяются.
        Set<String> availableObjectIds = new HashSet<>(objectIds);
        availableObjectIds.addAll(masterObjectIds);
        validateReadingOrder(page, availableObjectIds, pageLabel, errors);
        validateGroups(page, objectIds, errors, pageLabel);
        validateTableFrames(page, storyIds, errors, pageLabel);
        validateMaskReferences(page, objectIds, errors, pageLabel);
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

    private void validateReadingOrder(PageModel page, Set<String> availableObjectIds, String pageLabel, List<String> errors) {
        if (page.getReadingOrder().isEmpty()) {
            return; // Пустой readingOrder — валидное состояние "порядок не определён"
        }

        Set<Integer> seenSequences = new HashSet<>();
        Set<String> seenTargets = new HashSet<>();

        for (ReadingSegment segment : page.getReadingOrder()) {
            if (!seenSequences.add(segment.getSequence())) {
                errors.add(pageLabel + ": duplicate readingOrder sequence " + segment.getSequence());
            }
            if (!seenTargets.add(segment.getTargetObjectId())) {
                errors.add(pageLabel + ": object " + segment.getTargetObjectId()
                        + " appears more than once in readingOrder");
            }
            if (!availableObjectIds.contains(segment.getTargetObjectId())) {
                errors.add(pageLabel + ": readingOrder references targetObjectId "
                        + segment.getTargetObjectId() + " which does not match any object on this page or its MasterPage");
            }

            BdocObject targetObject = page.getObjects().stream()
                    .filter(o -> o.getId().equals(segment.getTargetObjectId()))
                    .findFirst()
                    .orElse(null);
            if (targetObject != null && targetObject.isArtifact()) {
                errors.add(pageLabel + ": readingOrder includes " + segment.getTargetObjectId()
                        + " which is marked as artifact (artifactType=" + targetObject.getArtifactType()
                        + ") and must be excluded from the reading flow");
            }
        }


    }

    private void validateGroups(PageModel page, Set<String> objectIds, List<String> errors, String pageLabel) {
        Map<String, Group> groupsById = new HashMap<>();
        for (BdocObject object : page.getObjects()) {
            if (object instanceof Group group) {
                groupsById.put(group.getId(), group);
            }
        }

        for (Group group : groupsById.values()) {
            for (String childId : group.getChildObjectIds()) {
                if (!objectIds.contains(childId)) {
                    errors.add(pageLabel + ": group " + group.getId() + " references childObjectId "
                            + childId + " which does not exist on this page");
                }
            }
        }

        for (Group group : groupsById.values()) {
            Set<String> visiting = new HashSet<>();
            if (hasCycle(group.getId(), groupsById, visiting)) {
                errors.add(pageLabel + ": group " + group.getId() + " participates in a circular nesting chain");
            }
        }
    }

    private boolean hasCycle(String groupId, Map<String, Group> groupsById, Set<String> visiting) {
        if (!visiting.add(groupId)) {
            return true;
        }
        Group group = groupsById.get(groupId);
        if (group != null) {
            for (String childId : group.getChildObjectIds()) {
                if (groupsById.containsKey(childId) && hasCycle(childId, groupsById, visiting)) {
                    return true;
                }
            }
        }
        visiting.remove(groupId);
        return false;
    }

    private void validateTableFrames(PageModel page, Set<String> storyIds, List<String> errors, String pageLabel) {
        for (BdocObject object : page.getObjects()) {
            if (!(object instanceof TableFrame table)) continue;

            for (TableCell cell : table.getCells()) {
                if (cell.getRowIndex() < 0 || cell.getRowIndex() >= table.getRowCount()
                        || cell.getColIndex() < 0 || cell.getColIndex() >= table.getColumnCount()) {
                    errors.add(pageLabel + ": tableFrame " + table.getId() + " has cell ["
                            + cell.getRowIndex() + "," + cell.getColIndex() + "] out of declared bounds");
                }
                if (cell.getStoryRef() != null && !storyIds.contains(cell.getStoryRef())) {
                    errors.add(pageLabel + ": tableFrame " + table.getId() + " cell ["
                            + cell.getRowIndex() + "," + cell.getColIndex() + "] has storyRef "
                            + cell.getStoryRef() + " which does not match any story");
                }
            }
        }
    }

    private void validateMaskReferences(PageModel page, Set<String> objectIds, List<String> errors, String pageLabel) {
        Map<String, BdocObject> objectsById = new HashMap<>();
        for (BdocObject object : page.getObjects()) {
            objectsById.put(object.getId(), object);
        }

        for (BdocObject object : page.getObjects()) {
            if (object.getMaskRef() == null) continue;

            BdocObject target = objectsById.get(object.getMaskRef());
            if (target == null) {
                errors.add(pageLabel + ": object " + object.getId() + " has maskRef " + object.getMaskRef()
                        + " which does not exist on this page");
            } else if (!target.isMask()) {
                errors.add(pageLabel + ": object " + object.getId() + " has maskRef " + object.getMaskRef()
                        + " but target object is not marked as mask=true");
            }
        }
    }


}