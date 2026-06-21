package org.example.bdoc.io;

import org.example.bdoc.model.DocumentModel;
import org.example.bdoc.model.SampleDocuments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BdocXmlSerializerTest {

    private final BdocXmlSerializer serializer = new BdocXmlSerializer();

    @Test
    void xmlRoundtripPreservesDocumentStructure(@TempDir Path tempDir) {
        DocumentModel original = SampleDocuments.sample();
        File file = tempDir.resolve("test.bdoc").toFile();

        serializer.save(original, file);
        DocumentModel loaded = serializer.load(file);

        assertEquals(original.getId(), loaded.getId());
        assertEquals(original.getTitle(), loaded.getTitle());
        assertEquals(original.getDocumentType(), loaded.getDocumentType());
        assertEquals(original.getPages().size(), loaded.getPages().size());
        assertEquals(original.getStories().size(), loaded.getStories().size());

        var originalPage = original.getPages().get(0);
        var loadedPage = loaded.getPages().get(0);

        assertEquals(originalPage.getWidth(), loadedPage.getWidth());
        assertEquals(originalPage.getHeight(), loadedPage.getHeight());
        assertEquals(originalPage.getLayers().size(), loadedPage.getLayers().size());
        assertEquals(originalPage.getObjects().size(), loadedPage.getObjects().size());

        assertEquals(
                original.getStories().get(0).getJoinedText(),
                loaded.getStories().get(0).getJoinedText()
        );
    }

    @Test
    void xmlContainsExpectedElements() {
        DocumentModel document = SampleDocuments.sample();
        String xml = serializer.toXml(document);

        assertTrue(xml.contains("urn:bdoc:v0.1"));
        assertTrue(xml.contains("documentType=\"book\""));
        assertTrue(xml.contains("type=\"rounded-rectangle\""));
        assertTrue(xml.contains("storyRef=\"story-1\""));
        assertTrue(xml.contains("<geometry"));
        assertTrue(xml.contains("<paragraph role=\"body\""));
    }
}