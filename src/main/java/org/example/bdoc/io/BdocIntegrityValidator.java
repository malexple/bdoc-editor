package org.example.bdoc.io;

import org.example.bdoc.model.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class BdocIntegrityValidator {

    private static final Set<String> KNOWN_SHAPE_TYPES = Arrays.stream(ShapeType.values())
            .map(ShapeType::toJsonValue)
            .collect(Collectors.toSet());

    private static final Set<String> KNOWN_REFERENCE_TARGET_TYPES = Set.of("url", "object", "page");

    private Set<String> collectCharacterStyleIds(StylesCatalog styles) {
        Set<String> ids = new HashSet<>();
        for (CharacterStyle style : styles.getCharacterStyles()) {
            ids.add(style.getId());
        }
        return ids;
    }

    private Set<String> collectSwatchIds(StylesCatalog styles) {
        Set<String> ids = new HashSet<>();
        for (Swatch swatch : styles.getSwatches()) {
            ids.add(swatch.getId());
        }
        return ids;
    }

    private Set<String> collectColorProfileIds(Manifest manifest) {
        Set<String> ids = new HashSet<>();
        for (ColorProfile profile : manifest.getColorProfiles()) {
            ids.add(profile.getId());
        }
        return ids;
    }

    public void validate(DocumentHandle document) {
        List<String> errors = new ArrayList<>();

        Set<String> storyIds = document.getStoryIds();
        Set<String> knownParagraphStyleIds = collectParagraphStyleIds(document.getStyles());
        Set<String> knownSwatchIds = collectSwatchIds(document.getStyles());
        Set<String> knownColorProfileIds = collectColorProfileIds(document.getManifest());

        List<PageModel> allPages = loadAllPages(document, errors);
        Set<String> allObjectIds = collectAllObjectIds(allPages, document);
        Set<String> allPageIds = allPages.stream().map(PageModel::getId).collect(Collectors.toSet());
        Map<String, TextFrame> allTextFramesById = collectAllTextFrames(allPages);

        validateManifestColorReferences(document, knownColorProfileIds, errors);
        validateStyleSwatchReferences(document.getStyles(), knownSwatchIds, errors);
        validateStories(document, storyIds, knownParagraphStyleIds, allObjectIds, allPageIds, errors);
        validatePages(document, storyIds, allPages, knownSwatchIds, knownColorProfileIds, errors);
        validateTextThreading(allTextFramesById, errors);

        if (!errors.isEmpty()) {
            throw new BdocValidationException(
                    "BDoc document failed integrity validation", errors);
        }
    }

    /** Вопрос 8: outputIntentProfileRef манифеста должен указывать на существующий ColorProfile. */
    private void validateManifestColorReferences(DocumentHandle document, Set<String> knownColorProfileIds,
                                                 List<String> errors) {
        String outputIntentRef = document.getManifest().getOutputIntentProfileRef();
        if (outputIntentRef != null && !knownColorProfileIds.contains(outputIntentRef)) {
            errors.add("Manifest outputIntentProfileRef '" + outputIntentRef
                    + "' does not match any ColorProfile in manifest.json");
        }
    }

    /** Вопрос 8: colorSwatchRef у ParagraphStyle/CharacterStyle должен указывать на существующий Swatch. */
    private void validateStyleSwatchReferences(StylesCatalog styles, Set<String> knownSwatchIds, List<String> errors) {
        for (ParagraphStyle ps : styles.getParagraphStyles()) {
            if (ps.getColorSwatchRef() != null && !knownSwatchIds.contains(ps.getColorSwatchRef())) {
                errors.add("ParagraphStyle '" + ps.getId() + "' has colorSwatchRef '"
                        + ps.getColorSwatchRef() + "' which does not match any Swatch in styles.json");
            }
        }
        for (CharacterStyle cs : styles.getCharacterStyles()) {
            if (cs.getColorSwatchRef() != null && !knownSwatchIds.contains(cs.getColorSwatchRef())) {
                errors.add("CharacterStyle '" + cs.getId() + "' has colorSwatchRef '"
                        + cs.getColorSwatchRef() + "' which does not match any Swatch in styles.json");
            }
        }
    }

    private List<PageModel> loadAllPages(DocumentHandle document, List<String> errors) {
        List<PageModel> pages = new ArrayList<>();
        for (int index : new TreeSet<>(document.getPageIndices())) {
            try {
                pages.add(document.loadPage(index));
            } catch (IOException e) {
                errors.add("Page index " + index + ": failed to load page file: " + e.getMessage());
            }
        }
        return pages;
    }

    private Set<String> collectAllObjectIds(List<PageModel> pages, DocumentHandle document) {
        Set<String> ids = new HashSet<>();
        Set<String> processedTemplates = new HashSet<>();
        for (PageModel page : pages) {
            for (BdocObject object : page.getObjects()) {
                ids.add(object.getId());
            }
            if (page.getTemplateRef() != null && processedTemplates.add(page.getTemplateRef())) {
                MasterPage masterPage = document.getMasterPage(page.getTemplateRef());
                if (masterPage != null) {
                    for (BdocObject object : masterPage.getObjects()) {
                        ids.add(object.getId());
                    }
                }
            }
        }
        return ids;
    }

    private Map<String, TextFrame> collectAllTextFrames(List<PageModel> pages) {
        Map<String, TextFrame> map = new HashMap<>();
        for (PageModel page : pages) {
            for (BdocObject object : page.getObjects()) {
                if (object instanceof TextFrame tf) {
                    map.put(tf.getId(), tf);
                }
            }
        }
        return map;
    }

    private void validateStories(DocumentHandle document, Set<String> storyIds,
                                 Set<String> knownParagraphStyleIds, Set<String> allObjectIds,
                                 Set<String> allPageIds, List<String> errors) {

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
                    Span span = paragraph.getSpans().get(j);
                    String spanLabel = "Story '" + storyId + "', paragraph[" + i + "], span[" + j + "]";

                    if (span.getCharacterStyleRef() != null && !knownCharacterStyleIds.contains(span.getCharacterStyleRef())) {
                        errors.add(spanLabel + ": characterStyleRef '" + span.getCharacterStyleRef()
                                + "' does not match any CharacterStyle in styles.json");
                    }

                    if (span.getInlineAssetRef() != null && !document.hasResource(span.getInlineAssetRef())) {
                        errors.add(spanLabel + ": inlineAssetRef '" + span.getInlineAssetRef()
                                + "' does not exist in resources/");
                    }

                    validateFootnote(span.getFootnote(), spanLabel, errors);
                    validateReference(span.getReference(), spanLabel, allObjectIds, allPageIds, errors);
                }
            }
        }
    }

    private void validateFootnote(FootnoteModel footnote, String spanLabel, List<String> errors) {
        if (footnote == null) return;
        if (footnote.getNumber() == null || footnote.getNumber().isBlank()) {
            errors.add(spanLabel + ": footnote is present but has no number");
        }
    }

    private void validateReference(ReferenceModel reference, String spanLabel,
                                   Set<String> allObjectIds, Set<String> allPageIds, List<String> errors) {
        if (reference == null) return;

        String targetType = reference.getTargetType();
        String target = reference.getTarget();

        if (targetType == null || !KNOWN_REFERENCE_TARGET_TYPES.contains(targetType)) {
            errors.add(spanLabel + ": reference has unknown targetType '" + targetType + "'");
            return;
        }
        if (target == null || target.isBlank()) {
            errors.add(spanLabel + ": reference has empty target for targetType '" + targetType + "'");
            return;
        }

        switch (targetType) {
            case "object" -> {
                if (!allObjectIds.contains(target)) {
                    errors.add(spanLabel + ": reference targetType=object points to '" + target
                            + "' which does not match any object in the document");
                }
            }
            case "page" -> {
                if (!allPageIds.contains(target)) {
                    errors.add(spanLabel + ": reference targetType=page points to '" + target
                            + "' which does not match any page id in the document");
                }
            }
            case "url" -> { /* внешние URL не проверяются на достижимость на этапе валидации */ }
        }
    }

    private void validateTextThreading(Map<String, TextFrame> textFramesById, List<String> errors) {
        for (TextFrame frame : textFramesById.values()) {
            if (frame.getNextFrameRef() != null) {
                TextFrame next = textFramesById.get(frame.getNextFrameRef());
                if (next == null) {
                    errors.add("TextFrame '" + frame.getId() + "' has nextFrameRef '" + frame.getNextFrameRef()
                            + "' which does not match any TextFrame in the document");
                } else {
                    if (!Objects.equals(next.getStoryRef(), frame.getStoryRef())) {
                        errors.add("TextFrame '" + frame.getId() + "' is threaded to '" + next.getId()
                                + "' but they reference different stories ('" + frame.getStoryRef()
                                + "' vs '" + next.getStoryRef() + "')");
                    }
                    if (!frame.getId().equals(next.getPreviousFrameRef())) {
                        errors.add("TextFrame '" + frame.getId() + "' points to nextFrameRef '" + next.getId()
                                + "' but that frame's previousFrameRef does not point back (broken chain)");
                    }
                }
            }
            if (frame.getPreviousFrameRef() != null && !textFramesById.containsKey(frame.getPreviousFrameRef())) {
                errors.add("TextFrame '" + frame.getId() + "' has previousFrameRef '" + frame.getPreviousFrameRef()
                        + "' which does not match any TextFrame in the document");
            }
        }
    }

    private void validatePages(DocumentHandle document, Set<String> storyIds, List<PageModel> allPages,
                               Set<String> knownSwatchIds, Set<String> knownColorProfileIds, List<String> errors) {
        Set<Integer> pageIndices = document.getPageIndices();
        if (pageIndices.isEmpty()) {
            errors.add("Document has no pages: a document must contain at least one page");
            return;
        }
        validatePageIndexSequence(pageIndices, errors);

        Set<String> pageIdsSeen = new HashSet<>();
        for (PageModel page : allPages) {
            if (!pageIdsSeen.add(page.getId())) {
                errors.add("Duplicate page id: " + page.getId() + ": page ids must be unique across the document");
            }

            if (page.getTemplateRef() != null && document.getMasterPage(page.getTemplateRef()) == null) {
                errors.add("Page " + page.getId() + " has templateRef " + page.getTemplateRef()
                        + " which does not match any MasterPage in templates.json");
            }

            validateSinglePage(page, storyIds, document, knownSwatchIds, knownColorProfileIds, errors);
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

    private void validateSinglePage(PageModel page, Set<String> storyIds, DocumentHandle document,
                                    Set<String> knownSwatchIds, Set<String> knownColorProfileIds, List<String> errors) {
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
                ? masterPage.getObjects().stream().map(BdocObject::getId).collect(Collectors.toSet())
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

            validateObjectSpecifics(object, pageLabel, storyIds, document, knownSwatchIds, knownColorProfileIds, errors);
            validateObjectStyleRef(object, document.getStyles(), pageLabel, errors);
            validateAnchoredSettings(object, storyIds, document, pageLabel, errors);
            validateObjectSpecifics(object, pageLabel, storyIds, document, knownSwatchIds, knownColorProfileIds, errors);
            validateObjectStyleRef(object, document.getStyles(), pageLabel, errors);
            validateAnchoredSettings(object, storyIds, document, pageLabel, errors);
            validatePrepressGeometry(object, page, masterPage, document, pageLabel, errors);
        }

        Set<String> availableObjectIds = new HashSet<>(objectIds);
        availableObjectIds.addAll(masterObjectIds);
        validateReadingOrder(page, availableObjectIds, pageLabel, errors);
        validateGroups(page, objectIds, errors, pageLabel);
        validateTableFrames(page, storyIds, errors, pageLabel);
        validateMaskReferences(page, objectIds, errors, pageLabel);

    }

    private void validateObjectSpecifics(BdocObject object, String pageLabel, Set<String> storyIds,
                                         DocumentHandle document, Set<String> knownSwatchIds,
                                         Set<String> knownColorProfileIds, List<String> errors) {
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
            if (imageFrame.getProfileRef() != null && !knownColorProfileIds.contains(imageFrame.getProfileRef())) {
                errors.add(pageLabel + ": imageFrame '" + imageFrame.getId() + "' has profileRef '"
                        + imageFrame.getProfileRef() + "' which does not match any ColorProfile in manifest.json");
            }
        } else if (object instanceof VectorShape shape) {
            validateShapeGeometry(shape, pageLabel, errors);
            if (shape.getFillColorSwatchRef() != null && !knownSwatchIds.contains(shape.getFillColorSwatchRef())) {
                errors.add(pageLabel + ": vectorShape '" + shape.getId() + "' has fillColorSwatchRef '"
                        + shape.getFillColorSwatchRef() + "' which does not match any Swatch in styles.json");
            }
            if (shape.getStrokeColorSwatchRef() != null && !knownSwatchIds.contains(shape.getStrokeColorSwatchRef())) {
                errors.add(pageLabel + ": vectorShape '" + shape.getId() + "' has strokeColorSwatchRef '"
                        + shape.getStrokeColorSwatchRef() + "' which does not match any Swatch in styles.json");
            }
        } else if (object instanceof LineObject line) {
            if (line.getStrokeColorSwatchRef() != null && !knownSwatchIds.contains(line.getStrokeColorSwatchRef())) {
                errors.add(pageLabel + ": lineObject '" + line.getId() + "' has strokeColorSwatchRef '"
                        + line.getStrokeColorSwatchRef() + "' which does not match any Swatch in styles.json");
            }
        }
    }

    private void validateShapeGeometry(VectorShape shape, String pageLabel, List<String> errors) {
        Geometry g = shape.getGeometry();
        if (g.getWidth() < 0 || g.getHeight() < 0) {
            errors.add(pageLabel + ": vectorShape '" + shape.getId() +
                    "' has negative geometry width/height");
        }
        if (!KNOWN_SHAPE_TYPES.contains(shape.getShapeType())) {
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
            return;
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

    private void validateObjectStyleRef(BdocObject object, StylesCatalog styles, String pageLabel, List<String> errors) {
        if (object.getObjectStyleRef() == null) return;
        if (styles.findObjectStyle(object.getObjectStyleRef()) == null) {
            errors.add(pageLabel + ": object '" + object.getId() + "' has objectStyleRef '"
                    + object.getObjectStyleRef() + "' which does not match any ObjectStyle in styles.json");
        }
    }

    private void validateAnchoredSettings(BdocObject object, Set<String> storyIds, DocumentHandle document,
                                          String pageLabel, List<String> errors) {
        AnchoredObjectSettings settings = object.getAnchoredSettings();
        if (settings == null || !settings.isEnabled()) return;

        if (!storyIds.contains(settings.getStoryRef())) {
            errors.add(pageLabel + ": anchored object '" + object.getId() + "' references non-existing storyRef '"
                    + settings.getStoryRef() + "'");
            return;
        }

        StoryModel story = document.getStory(settings.getStoryRef());
        if (story != null) {
            int totalSpans = story.getParagraphs().stream()
                    .mapToInt(p -> p.getSpans().size())
                    .sum();
            if (settings.getTargetSpanIndex() < 0 || settings.getTargetSpanIndex() >= totalSpans) {
                errors.add(pageLabel + ": anchored object '" + object.getId() + "' has targetSpanIndex ["
                        + settings.getTargetSpanIndex() + "] out of bounds for story '"
                        + settings.getStoryRef() + "' (total spans: " + totalSpans + ")");
            }
        }
    }

    /**
     * Preflight-проверки Этапа 1.8 (Вопрос 7):
     * 1. Ширина/высота строго больше нуля (защита от зависания TextWrapper).
     * 2. Объект должен пересекаться с MediaBox (TrimBox + bleedMargin).
     * 3. TextFrame на слое типа "text" не может выходить за safetyMargin.
     */
    private void validatePrepressGeometry(BdocObject object, PageModel page, MasterPage masterPage,
                                          DocumentHandle document, String pageLabel, List<String> errors) {
        if (object instanceof LineObject) {
            return; // у LineObject своя геометрия точек, правило не применяется
        }

        boolean isDecorativeLineShape = object instanceof VectorShape vs && "line".equals(vs.getShapeType());

        Geometry g = object.getGeometry();

        if (!isDecorativeLineShape && (g.getWidth() <= 0.0 || g.getHeight() <= 0.0)) {
            errors.add(pageLabel + ": object '" + object.getId() + "' has non-positive geometry ("
                    + g.getWidth() + "x" + g.getHeight() + "), width and height must be strictly greater than zero");
            return;
        }

        double bleed = PrepressResolver.resolveBleedMargin(page, masterPage, document.getManifest());
        double mediaMinX = -bleed;
        double mediaMinY = -bleed;
        double mediaMaxX = page.getWidth() + bleed;
        double mediaMaxY = page.getHeight() + bleed;

        boolean intersectsMedia = g.getX() < mediaMaxX && g.getX() + g.getWidth() > mediaMinX
                && g.getY() < mediaMaxY && g.getY() + g.getHeight() > mediaMinY;
        if (!intersectsMedia) {
            errors.add(pageLabel + ": object '" + object.getId() + "' is completely outside MediaBox ("
                    + "geometry x=" + g.getX() + ", y=" + g.getY() + ", w=" + g.getWidth() + ", h=" + g.getHeight()
                    + ") — object is out of bounds and unreachable on the printed sheet");
        }

        if (object instanceof TextFrame textFrame) {
            LayerModel layer = page.getLayers().stream()
                    .filter(l -> l.getId().equals(textFrame.getLayerRef()))
                    .findFirst()
                    .orElse(null);
            if (layer != null && "text".equals(layer.getRole())) {
                double safety = PrepressResolver.resolveSafetyMargin(page, masterPage, document.getManifest());
                double safetyMinX = safety;
                double safetyMinY = safety;
                double safetyMaxX = page.getWidth() - safety;
                double safetyMaxY = page.getHeight() - safety;

                boolean withinSafety = g.getX() >= safetyMinX && g.getY() >= safetyMinY
                        && g.getX() + g.getWidth() <= safetyMaxX && g.getY() + g.getHeight() <= safetyMaxY;
                if (!withinSafety) {
                    errors.add(pageLabel + ": textFrame '" + textFrame.getId()
                            + "' crosses the safetyMargin boundary (safetyMargin=" + safety
                            + "pt) — text on a TEXT layer must stay fully within the safe area to avoid trimming damage");
                }
            }
        }
    }
}