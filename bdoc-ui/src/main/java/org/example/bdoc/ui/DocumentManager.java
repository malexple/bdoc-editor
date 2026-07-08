package org.example.bdoc.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import org.example.bdoc.io.*;
import org.example.bdoc.model.*;
import org.example.bdoc.plugin.BdocSettings;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DocumentManager {
    private final BdocContainerSerializer serializer = new BdocContainerSerializer();
    private final BdocIntegrityValidator validator = new BdocIntegrityValidator();
    private final DocumentEventSink eventSink;

    private DocumentHandle document;
    private File currentFile;
    private int currentPageIndex = 1;
    private BdocObject selectedObject;

    public DocumentManager(DocumentEventSink eventSink) { this.eventSink = eventSink; }

    public DocumentHandle getDocument() { return document; }
    public File getCurrentFile() { return currentFile; }
    public void setCurrentFile(File f) { currentFile = f; }
    public int getCurrentPageIndex() { return currentPageIndex; }
    public void setCurrentPageIndex(int idx) { currentPageIndex = idx; eventSink.onPageChanged(idx); }
    public BdocObject getSelectedObject() { return selectedObject; }
    public void setSelectedObject(BdocObject obj) { selectedObject = obj; eventSink.onObjectSelected(obj); }

    public PageModel getCurrentPage() {
        try { return document.loadPage(currentPageIndex); }
        catch (IOException e) { eventSink.showError("Page load error", e.getMessage()); return null; }
    }
    public MasterPage getCurrentMasterPage() {
        PageModel page = getCurrentPage();
        return page != null ? document.getMasterPage(page.getTemplateRef()) : null;
    }

    public void openDocument(File file) {
        try {
            DocumentHandle previous = document;
            DocumentHandle opened = serializer.open(file);
            try { validator.validate(opened); }
            catch (BdocValidationException vex) {
                opened.close();
                eventSink.showValidationErrors("Cannot open document", vex);
                return;
            }
            document = opened;
            currentFile = file;
            currentPageIndex = 1;
            selectedObject = null;
            if (previous != null) previous.close();
            eventSink.onDocumentOpened();
            BdocSettings.getInstance().pushRecentFile(file.getAbsolutePath());
            eventSink.onRecentFilesChanged();
        } catch (Exception e) {
            eventSink.showError("Open error", e.getMessage());
        }
    }

    public void saveDocument(DocumentHandle handle, File target) throws IOException {
        ObjectMapper json = new ObjectMapper();
        CBORMapper cbor = new CBORMapper();
        Map<String, String> env = new HashMap<>(); env.put("create", "true");
        if (target.exists()) target.delete();
        URI uri = URI.create("jar:file:" + target.toURI().getPath());
        try (java.nio.file.FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Files.createDirectories(zipfs.getPath("/pages"));
            Files.createDirectories(zipfs.getPath("/stories"));
            Files.createDirectories(zipfs.getPath("/resources"));
            Files.createDirectories(zipfs.getPath("/templates"));

            for (String sid : handle.getStoryIds()) {
                StoryModel story = handle.getStory(sid);
                if (story != null) {
                    Path p = zipfs.getPath("/stories/" + sid + ".json");
                    try (var os = Files.newOutputStream(p)) { json.writerWithDefaultPrettyPrinter().writeValue(os, story); }
                }
            }
            List<Map<String,Object>> pagesList = new ArrayList<>();
            for (Integer idx : handle.getPageIndices()) {
                PageModel page = handle.loadPage(idx);
                Path p = zipfs.getPath("/pages/page-" + idx + ".cbor");
                try (var os = Files.newOutputStream(p)) { cbor.writeValue(os, page); }
                Map<String,Object> e = new HashMap<>();
                e.put("index", idx); e.put("id", "page-"+idx); e.put("file", "pages/page-"+idx+".cbor");
                pagesList.add(e);
            }
            List<Map<String,Object>> templatesList = new ArrayList<>();
            for (MasterPage mp : handle.getTemplates().getMasterPages()) {
                Path p = zipfs.getPath("/templates/" + mp.getId() + ".json");
                try (var os = Files.newOutputStream(p)) { json.writerWithDefaultPrettyPrinter().writeValue(os, mp); }
                Map<String,Object> e = new HashMap<>();
                e.put("id", mp.getId()); e.put("file", "templates/"+mp.getId()+".json");
                templatesList.add(e);
            }
            List<Map<String,Object>> storiesList = new ArrayList<>();
            for (String sid : handle.getStoryIds()) {
                Map<String,Object> e = new HashMap<>();
                e.put("id", sid); e.put("file", "stories/"+sid+".json");
                storiesList.add(e);
            }
            Map<String,Object> manifest = new LinkedHashMap<>();
            manifest.put("id", handle.getId());
            manifest.put("title", handle.getTitle());
            manifest.put("documentType", handle.getDocumentType());
            manifest.put("version", "0.1-composite");
            manifest.put("pages", pagesList);
            manifest.put("stories", storiesList);
            manifest.put("templates", templatesList);
            Path mp = zipfs.getPath("/manifest.json");
            try (var os = Files.newOutputStream(mp)) { json.writerWithDefaultPrettyPrinter().writeValue(os, manifest); }
            if (handle.getStyles() != null) {
                Path sp = zipfs.getPath("/styles.json");
                try (var os = Files.newOutputStream(sp)) { json.writerWithDefaultPrettyPrinter().writeValue(os, handle.getStyles()); }
            }
        }
    }

    public boolean validateBeforeSave() {
        try { validator.validate(document); return true; }
        catch (BdocValidationException vex) { eventSink.showValidationErrors("Cannot save document", vex); return false; }
    }

    public BdocObject materializeOverrideIfNeeded(PageModel page, MasterPage master, BdocObject object) {
        if (master == null || object.isMasterOverride() || master.findObject(object.getId()) == null) return object;
        Set<String> overridden = new HashSet<>(); overridden.add("geometry");
        Geometry clonedGeo = object.getGeometry().copy();
        BdocObject override;
        if (object instanceof TextFrame tf)
            override = new TextFrame(tf.getId(), tf.getLayerRef(), clonedGeo, tf.getStoryRef(), tf.getId(), overridden);
        else if (object instanceof ImageFrame imf)
            override = new ImageFrame(imf.getId(), imf.getLayerRef(), clonedGeo, imf.getAssetRef(), imf.getId(), overridden);
        else if (object instanceof VectorShape vs)
            override = new VectorShape(vs.getId(), vs.getLayerRef(), clonedGeo, vs.getShapeType(), vs.getId(), overridden);
        else if (object instanceof HeaderFooterRule hfr)
            override = new HeaderFooterRule(hfr.getId(), hfr.getLayerRef(), clonedGeo, hfr.getZone(), hfr.getTextTemplate(), hfr.getStyleRef(), hfr.getId(), overridden);
        else return object;
        page.getObjects().add(override);
        eventSink.onDataChanged();
        return override;
    }

    public BdocObject restoreToMaster(PageModel page, MasterPage master, BdocObject override) {
        if (master == null || !override.isMasterOverride()) return override;
        page.getObjects().removeIf(o -> o.getId().equals(override.getId()) && o.isMasterOverride());
        return master.findObject(override.getMasterSourceId());
    }

    public BdocObject replacePathData(BdocObject object, PathModel newPath) {
        if (!(object instanceof VectorShape vs)) return object;
        VectorShape updated = new VectorShape(vs.getId(), vs.getLayerRef(), vs.getGeometry(), vs.getShapeType(),
                vs.getMasterSourceId(), vs.getOverriddenProperties(), vs.isVisible(), vs.getClipGeometry(),
                vs.getMaskRef(), vs.isMask(), vs.isArtifact(), vs.getArtifactType(), vs.getTextWrap(),
                newPath, vs.getTransform());
        PageModel page = getCurrentPage();
        for (int i = 0; i < page.getObjects().size(); i++) {
            if (page.getObjects().get(i).getId().equals(vs.getId())) {
                page.getObjects().set(i, updated);
                break;
            }
        }
        eventSink.onDataChanged();
        return updated;
    }

    public void runWriteAction(Runnable mutation) {
        mutation.run();
        eventSink.onDataChanged();
    }

    public void loadInitialSample() {
        try {
            File sample = Files.createTempFile("bdoc-sample", ".bdoc").toFile();
            sample.deleteOnExit();
            SampleDocuments.writeSample(sample);
            openDocument(sample);
        } catch (Exception e) {
            eventSink.showError("Failed to create sample", e.getMessage());
        }
    }
}