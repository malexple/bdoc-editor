package org.example.bdoc.model;

public final class SampleDocuments {
    private SampleDocuments() {
    }

    public static DocumentModel sample() {
        DocumentModel document = new DocumentModel("doc-1", "BDoc Demo", DocumentType.BOOK);

        StoryModel story = new StoryModel("story-1");
        story.addParagraph(new Paragraph(
                "body",
                "Это тестовый документ BDoc v0.1. Сначала фиксируется структура документа, а затем программа применяет отображение."
        ));
        document.addStory(story);

        PageModel page = new PageModel("page-1", 1, 595, 842);
        page.addLayer(new LayerModel("layer-bg", "Background", LayerRole.BACKGROUND, true, 0));
        page.addLayer(new LayerModel("layer-decor", "Decoration", LayerRole.DECORATION, true, 1));
        page.addLayer(new LayerModel("layer-text", "Text", LayerRole.TEXT, true, 2));

        page.addObject(new VectorShape(
                "shape-1",
                "layer-decor",
                ShapeType.ROUNDED_RECTANGLE,
                new Geometry(40, 40, 515, 762, 24.0, 24.0)
        ));

        page.addObject(new VectorShape(
                "shape-2",
                "layer-decor",
                ShapeType.LINE,
                new Geometry(70, 120, 455, 0)
        ));

        page.addObject(new TextFrame(
                "text-1",
                "layer-text",
                "story-1",
                new Geometry(70, 150, 455, 180)
        ));

        document.addPage(page);
        return document;
    }
}