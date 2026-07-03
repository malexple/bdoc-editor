package org.example.bdoc.model;

import org.example.bdoc.io.BdocContainerSerializer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class SampleDocuments {

    private SampleDocuments() {
    }

    /**
     * Записывает демонстрационный .bdoc-документ в указанный файл.
     * В новой архитектуре документ не существует "в памяти" сам по себе —
     * он всегда сохраняется как контейнер и затем открывается через DocumentHandle.
     */
    public static void writeSample(File target) throws IOException {
        BdocContainerSerializer serializer = new BdocContainerSerializer();

        try (BdocContainerSerializer.Writer writer = serializer.beginWrite(target)) {
            ParagraphStyle bodyStyle = new ParagraphStyle(
                    "body-text", null, "Georgia", 16.0, 1.4, "left", "#1E293B"
            );
            ParagraphStyle headingStyle = new ParagraphStyle(
                    "heading-1", "body-text", "Georgia", 26.0, 1.2, "center", "#0F172A"
            );

            StoryModel story = new StoryModel("story-1", List.of(
                    new Paragraph("heading", "heading-1", "Демонстрация стилей BDoc"),
                    new Paragraph("body", "body-text",
                            "Это тестовый документ BDoc v0.1-composite. Параграф заголовка наследует " +
                                    "шрифт и цвет от body-text через basedOn, но переопределяет размер, " +
                                    "выравнивание и цвет.")
            ));
            writer.writeStory(story);

            LayerModel bgLayer = new LayerModel("layer-bg", "Background", "background", true, 1.0);
            LayerModel decorLayer = new LayerModel("layer-decor", "Decoration", "decoration", true, 1.0);
            LayerModel textLayer = new LayerModel("layer-text", "Text", "text", true, 1.0);

            VectorShape frame = new VectorShape(
                    "shape-1", "layer-decor",
                    new Geometry(40.0, 40.0, 515.0, 762.0, 24.0, 24.0),
                    "rounded-rectangle"
            );

            VectorShape divider = new VectorShape(
                    "shape-2", "layer-decor",
                    new Geometry(70.0, 120.0, 455.0, 0.0),
                    "line"
            );

            TextFrame textFrame = new TextFrame(
                    "text-1", "layer-text",
                    new Geometry(70.0, 150.0, 455.0, 180.0),
                    "story-1"
            );

            PageModel page = new PageModel(
                    "page-1", 1, 595.0, 842.0, "pt",
                    List.of(bgLayer, decorLayer, textLayer),
                    List.of(frame, divider, textFrame)
            );
            writer.writePage(page);

            StylesCatalog styles = new StylesCatalog(
                    List.of(bodyStyle, headingStyle),
                    List.of()
            );

            writer.finish(
                    "doc-1", "BDoc Demo", "book",
                    "0.1-composite", "ru-RU", styles
            );
        }
    }
}