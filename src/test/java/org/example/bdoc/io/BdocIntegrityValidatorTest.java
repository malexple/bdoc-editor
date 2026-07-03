package org.example.bdoc.io;

import org.example.bdoc.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BdocIntegrityValidatorTest {

    private final BdocContainerSerializer serializer = new BdocContainerSerializer();
    private final BdocIntegrityValidator validator = new BdocIntegrityValidator();

    @Test
    void validDocumentPassesValidation(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("valid.bdoc").toFile();

        try (BdocContainerSerializer.Writer writer = serializer.beginWrite(file)) {
            writer.writeStory(new StoryModel("story-1", List.of(
                    new Paragraph("body", null, "Текст без стиля — допустимо.")
            )));

            LayerModel textLayer = new LayerModel("layer-text", "Text", "text", true, 1.0);
            writer.writePage(new PageModel(
                    "page-1", 1, 595.0, 842.0, "pt",
                    List.of(textLayer),
                    List.of(new TextFrame("frame-1", "layer-text",
                            new Geometry(70, 120, 455, 600), "story-1"))
            ));

            writer.finish("doc-1", "Valid Doc", "book", "0.1-composite", "ru-RU", StylesCatalog.empty());
        }

        try (DocumentHandle handle = serializer.open(file)) {
            assertDoesNotThrow(() -> validator.validate(handle));
        }
    }

    @Test
    void missingStoryRefIsDetected(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("bad-storyref.bdoc").toFile();

        try (BdocContainerSerializer.Writer writer = serializer.beginWrite(file)) {
            writer.writeStory(new StoryModel("story-1", List.of(
                    new Paragraph("body", null, "Есть история.")
            )));

            LayerModel textLayer = new LayerModel("layer-text", "Text", "text", true, 1.0);
            writer.writePage(new PageModel(
                    "page-1", 1, 595.0, 842.0, "pt",
                    List.of(textLayer),
                    List.of(new TextFrame("frame-1", "layer-text",
                            new Geometry(70, 120, 455, 600), "story-missing"))
            ));

            writer.finish("doc-1", "Bad Doc", "book", "0.1-composite", "ru-RU", StylesCatalog.empty());
        }

        try (DocumentHandle handle = serializer.open(file)) {
            BdocValidationException ex = assertThrows(
                    BdocValidationException.class, () -> validator.validate(handle));
            assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("story-missing")));
        }
    }

    @Test
    void missingAssetRefIsDetected(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("bad-assetref.bdoc").toFile();

        try (BdocContainerSerializer.Writer writer = serializer.beginWrite(file)) {
            writer.writeStory(new StoryModel("story-1", List.of(
                    new Paragraph("body", null, "Текст.")
            )));

            LayerModel bgLayer = new LayerModel("layer-bg", "Scan", "background", true, 0.4);
            writer.writePage(new PageModel(
                    "page-1", 1, 595.0, 842.0, "mm",
                    List.of(bgLayer),
                    List.of(new ImageFrame("scan-1", "layer-bg",
                            new Geometry(0, 0, 595, 842), "resources/missing.jpg"))
            ));

            writer.finish("doc-1", "Bad Doc", "book", "0.1-composite", "ru-RU", StylesCatalog.empty());
        }

        try (DocumentHandle handle = serializer.open(file)) {
            BdocValidationException ex = assertThrows(
                    BdocValidationException.class, () -> validator.validate(handle));
            assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("resources/missing.jpg")));
        }
    }

    @Test
    void invalidLayerRefIsDetected(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("bad-layerref.bdoc").toFile();

        try (BdocContainerSerializer.Writer writer = serializer.beginWrite(file)) {
            writer.writeStory(new StoryModel("story-1", List.of(
                    new Paragraph("body", null, "Текст.")
            )));

            LayerModel textLayer = new LayerModel("layer-text", "Text", "text", true, 1.0);
            writer.writePage(new PageModel(
                    "page-1", 1, 595.0, 842.0, "pt",
                    List.of(textLayer),
                    List.of(new TextFrame("frame-1", "layer-missing",
                            new Geometry(70, 120, 455, 600), "story-1"))
            ));

            writer.finish("doc-1", "Bad Doc", "book", "0.1-composite", "ru-RU", StylesCatalog.empty());
        }

        try (DocumentHandle handle = serializer.open(file)) {
            BdocValidationException ex = assertThrows(
                    BdocValidationException.class, () -> validator.validate(handle));
            assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("layer-missing")));
        }
    }
}