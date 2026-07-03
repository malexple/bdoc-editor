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
            StoryModel story = new StoryModel("story-1", List.of(
                    new Paragraph(
                            "body",
                            "main-text",
                            "Это тестовый документ BDoc v0.1-composite. Сначала фиксируется " +
                                    "структура документа, а затем программа применяет отображение."
                    )
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

            writer.finish(
                    "doc-1", "BDoc Demo", "book",
                    "0.1-composite", "ru-RU", StylesCatalog.empty()
            );
        }
    }
}