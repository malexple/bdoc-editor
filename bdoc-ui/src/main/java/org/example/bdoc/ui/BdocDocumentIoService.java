package org.example.bdoc.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import org.example.bdoc.io.BdocContainerSerializer;
import org.example.bdoc.io.BdocIntegrityValidator;
import org.example.bdoc.io.BdocValidationException;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.MasterPage;
import org.example.bdoc.model.PageModel;
import org.example.bdoc.model.StoryModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BdocDocumentIoService {

    private final BdocContainerSerializer serializer = new BdocContainerSerializer();
    private final BdocIntegrityValidator integrityValidator = new BdocIntegrityValidator();

    public File createSampleDocumentFile() throws Exception {
        File sampleFile = Files.createTempFile("bdoc-sample", ".bdoc").toFile();
        sampleFile.deleteOnExit();
        SampleDocuments.writeSample(sampleFile);
        return sampleFile;
    }

    public DocumentHandle open(File file) throws IOException {
        return serializer.open(file);
    }

    public void validate(DocumentHandle handle) throws BdocValidationException {
        integrityValidator.validate(handle);
    }

    public void save(DocumentHandle handle, File targetFile) throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        CBORMapper cborMapper = new CBORMapper();

        Map<String, String> env = new HashMap<>();
        env.put("create", "true");

        if (targetFile.exists()) {
            targetFile.delete();
        }

        java.net.URI uri = java.net.URI.create("jar:file:" + targetFile.toURI().getPath());

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Files.createDirectories(zipfs.getPath("/pages"));
            Files.createDirectories(zipfs.getPath("/stories"));
            Files.createDirectories(zipfs.getPath("/resources"));
            Files.createDirectories(zipfs.getPath("/templates"));

            for (String storyId : handle.getStoryIds()) {
                StoryModel story = handle.getStory(storyId);
                if (story != null) {
                    java.nio.file.Path storyPath = zipfs.getPath("/stories/" + storyId + ".json");
                    try (java.io.OutputStream os = Files.newOutputStream(storyPath)) {
                        jsonMapper.writerWithDefaultPrettyPrinter().writeValue(os, story);
                    }
                }
            }

            for (Integer index : handle.getPageIndices()) {
                PageModel page = handle.loadPage(index);
                java.nio.file.Path pagePath = zipfs.getPath("/pages/page-" + index + ".cbor");
                try (java.io.OutputStream os = Files.newOutputStream(pagePath)) {
                    cborMapper.writeValue(os, page);
                }
            }

            List<Map<String, Object>> templatesList = new ArrayList<>();
            for (MasterPage masterPage : handle.getTemplates().getMasterPages()) {
                java.nio.file.Path templatePath = zipfs.getPath("/templates/" + masterPage.getId() + ".json");
                try (java.io.OutputStream os = Files.newOutputStream(templatePath)) {
                    jsonMapper.writerWithDefaultPrettyPrinter().writeValue(os, masterPage);
                }

                Map<String, Object> tEntry = new HashMap<>();
                tEntry.put("id", masterPage.getId());
                tEntry.put("file", "templates/" + masterPage.getId() + ".json");
                templatesList.add(tEntry);
            }

            Map<String, Object> manifestMap = new HashMap<>();
            manifestMap.put("id", handle.getId());
            manifestMap.put("title", handle.getTitle());
            manifestMap.put("documentType", handle.getDocumentType());
            manifestMap.put("version", "0.1-composite");

            List<Map<String, Object>> pagesList = new ArrayList<>();
            for (Integer index : handle.getPageIndices()) {
                Map<String, Object> pEntry = new HashMap<>();
                pEntry.put("index", index);
                pEntry.put("id", "page-" + index);
                pEntry.put("file", "pages/page-" + index + ".cbor");
                pagesList.add(pEntry);
            }
            manifestMap.put("pages", pagesList);

            List<Map<String, Object>> storiesList = new ArrayList<>();
            for (String storyId : handle.getStoryIds()) {
                Map<String, Object> sEntry = new HashMap<>();
                sEntry.put("id", storyId);
                sEntry.put("file", "stories/" + storyId + ".json");
                storiesList.add(sEntry);
            }
            manifestMap.put("stories", storiesList);
            manifestMap.put("templates", templatesList);

            java.nio.file.Path manifestPath = zipfs.getPath("/manifest.json");
            try (java.io.OutputStream os = Files.newOutputStream(manifestPath)) {
                jsonMapper.writerWithDefaultPrettyPrinter().writeValue(os, manifestMap);
            }

            if (handle.getStyles() != null) {
                java.nio.file.Path stylesPath = zipfs.getPath("/styles.json");
                try (java.io.OutputStream os = Files.newOutputStream(stylesPath)) {
                    jsonMapper.writerWithDefaultPrettyPrinter().writeValue(os, handle.getStyles());
                }
            }
        }
    }
}