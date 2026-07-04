package org.example.bdoc.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.bdoc.io.BdocContainerSerializer;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.*;
import org.example.bdoc.render.PageRenderer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BdocEditorApp extends Application {

    private final BdocContainerSerializer serializer = new BdocContainerSerializer();
    private final PageRenderer pageRenderer = new PageRenderer();

    private DocumentHandle document;
    private File currentFile;
    private int currentPageIndex = 1;

    private Canvas canvas;
    private ListView<String> documentTree;
    private Label statusLabel;

    private DtpTool currentTool = DtpTool.SELECTION;
    private BdocObject selectedObject = null;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private double objectInitialX = 0;
    private double objectInitialY = 0;

    private VBox propertiesContainer;
    private Slider opacitySlider;
    private CheckBox visibleCheckBox;
    private TextArea storyTextArea;

    private boolean isResizing = false;
    private int resizeHandleIndex = -1;
    private double initialWidth = 0;
    private double initialHeight = 0;
    private final double HANDLE_SIZE = 6.0;

    @Override
    public void start(Stage stage) {
        documentTree = new ListView<>();
        documentTree.setPrefWidth(220);
        documentTree.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            if (newIdx != null && newIdx.intValue() >= 0) {
                onPageSelected(newIdx.intValue());
            }
        });

        canvas = new Canvas(595, 842);
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setPadding(new Insets(24));
        canvasPane.setStyle("-fx-background-color: #CBD5E1;");

        BorderPane propertiesPane = new BorderPane();
        propertiesPane.setPadding(new Insets(12));
        propertiesPane.setPrefWidth(240);
        Label propTitle = new Label("Properties & Layers");
        propTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        propertiesPane.setTop(propTitle);

        propertiesContainer = new VBox(10);
        propertiesContainer.setPadding(new Insets(10, 0, 0, 0));
        propertiesPane.setCenter(propertiesContainer);

        SplitPane splitPane = new SplitPane(documentTree, canvasPane, propertiesPane);
        splitPane.setDividerPositions(0.18, 0.78);
        statusLabel = new Label("No document loaded");

        ToggleGroup toolGroup = new ToggleGroup();
        ToggleButton selectToolBtn = new ToggleButton("🏹 Select");
        selectToolBtn.setToggleGroup(toolGroup);
        selectToolBtn.setSelected(true);
        selectToolBtn.setOnAction(e -> currentTool = DtpTool.SELECTION);

        ToggleButton textToolBtn = new ToggleButton("📝 Text");
        textToolBtn.setToggleGroup(toolGroup);
        textToolBtn.setOnAction(e -> currentTool = DtpTool.TEXT);

        Button openBtn = new Button("Open");
        openBtn.setOnAction(e -> onOpen(stage));
        Button saveAsBtn = new Button("Save As");
        saveAsBtn.setOnAction(e -> onSaveAs(stage));
        Button newSampleBtn = new Button("New Sample");
        newSampleBtn.setOnAction(e -> onNewSample(stage));

        ToolBar toolBar = new ToolBar(
                new Label("BDoc Editor v0.1"),
                new Separator(),
                selectToolBtn, textToolBtn,
                new Separator(),
                newSampleBtn, openBtn, saveAsBtn
        );

        canvas.setOnMousePressed(e -> {
            if (document == null) return;
            try {
                PageModel page = document.loadPage(currentPageIndex);
                MasterPage masterPage = document.getMasterPage(page.getTemplateRef());

                if (currentTool == DtpTool.SELECTION && selectedObject != null) {
                    Geometry g = selectedObject.getGeometry();
                    double x = e.getX();
                    double y = e.getY();

                    double[][] handles = {
                            {g.getX(), g.getY()},
                            {g.getX() + g.getWidth(), g.getY()},
                            {g.getX(), g.getY() + g.getHeight()},
                            {g.getX() + g.getWidth(), g.getY() + g.getHeight()}
                    };

                    for (int i = 0; i < handles.length; i++) {
                        if (x >= handles[i][0] - HANDLE_SIZE && x <= handles[i][0] + HANDLE_SIZE &&
                                y >= handles[i][1] - HANDLE_SIZE && y <= handles[i][1] + HANDLE_SIZE) {

                            // Резать по мастеру можно только через материализованный override
                            selectedObject = materializeOverrideIfNeeded(page, masterPage, selectedObject);

                            isResizing = true;
                            resizeHandleIndex = i;
                            Geometry gg = selectedObject.getGeometry();
                            dragStartX = x;
                            dragStartY = y;
                            objectInitialX = gg.getX();
                            objectInitialY = gg.getY();
                            initialWidth = gg.getWidth();
                            initialHeight = gg.getHeight();
                            statusLabel.setText("Resizing object from corner: " + i);
                            return;
                        }
                    }
                }

                isResizing = false;
                resizeHandleIndex = -1;

                List<BdocObject> effectiveObjects = pageRenderer.resolveEffectiveObjects(page, masterPage);
                BdocObject found = null;
                for (int i = effectiveObjects.size() - 1; i >= 0; i--) {
                    BdocObject obj = effectiveObjects.get(i);
                    Geometry g = obj.getGeometry();
                    if (e.getX() >= g.getX() && e.getX() <= g.getX() + g.getWidth() &&
                            e.getY() >= g.getY() && e.getY() <= g.getY() + g.getHeight()) {
                        found = obj;
                        break;
                    }
                }

                selectedObject = found;

                if (currentTool == DtpTool.SELECTION) {
                    if (selectedObject != null) {
                        dragStartX = e.getX();
                        dragStartY = e.getY();
                        objectInitialX = selectedObject.getGeometry().getX();
                        objectInitialY = selectedObject.getGeometry().getY();
                        boolean fromMaster = pageRenderer.isRawMasterObject(selectedObject, masterPage);
                        statusLabel.setText((fromMaster ? "Selected (master-locked): " : "Selected: ") + selectedObject.getId());
                        updatePropertiesPane(page, masterPage, selectedObject);
                    } else {
                        propertiesContainer.getChildren().clear();
                    }
                    renderCurrentPage();
                } else if (currentTool == DtpTool.TEXT) {
                    if (selectedObject instanceof TextFrame textFrame) {
                        StoryModel story = document.getStory(textFrame.getStoryRef());
                        statusLabel.setText("Editing Story: " + textFrame.getStoryRef());
                        renderCurrentPage();
                        updateTextEditorPane(textFrame, story);
                    } else {
                        propertiesContainer.getChildren().clear();
                        renderCurrentPage();
                    }
                }
            } catch (IOException ex) {
                showError("Mouse Press Error", ex.getMessage());
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (selectedObject == null) return;

            try {
                PageModel page = document.loadPage(currentPageIndex);
                MasterPage masterPage = document.getMasterPage(page.getTemplateRef());

                // Первое движение по "сырому" объекту мастера — материализуем override
                if (!isResizing && pageRenderer.isRawMasterObject(selectedObject, masterPage)) {
                    selectedObject = materializeOverrideIfNeeded(page, masterPage, selectedObject);
                    objectInitialX = selectedObject.getGeometry().getX();
                    objectInitialY = selectedObject.getGeometry().getY();
                }
            } catch (IOException ex) {
                showError("Drag Error", ex.getMessage());
                return;
            }

            double deltaX = e.getX() - dragStartX;
            double deltaY = e.getY() - dragStartY;
            Geometry g = selectedObject.getGeometry();

            if (isResizing) {
                switch (resizeHandleIndex) {
                    case 0 -> {
                        double newWidth = initialWidth - deltaX;
                        double newHeight = initialHeight - deltaY;
                        if (newWidth > 10 && newHeight > 10) {
                            g.setX(objectInitialX + deltaX);
                            g.setY(objectInitialY + deltaY);
                            g.setWidth(newWidth);
                            g.setHeight(newHeight);
                        }
                    }
                    case 1 -> {
                        double newWidth = initialWidth + deltaX;
                        double newHeight = initialHeight - deltaY;
                        if (newWidth > 10 && newHeight > 10) {
                            g.setY(objectInitialY + deltaY);
                            g.setWidth(newWidth);
                            g.setHeight(newHeight);
                        }
                    }
                    case 2 -> {
                        double newWidth = initialWidth - deltaX;
                        double newHeight = initialHeight + deltaY;
                        if (newWidth > 10 && newHeight > 10) {
                            g.setX(objectInitialX + deltaX);
                            g.setWidth(newWidth);
                            g.setHeight(newHeight);
                        }
                    }
                    case 3 -> {
                        double newWidth = initialWidth + deltaX;
                        double newHeight = initialHeight + deltaY;
                        if (newWidth > 10 && newHeight > 10) {
                            g.setWidth(newWidth);
                            g.setHeight(newHeight);
                        }
                    }
                }
                renderCurrentPage();
            } else if (currentTool == DtpTool.SELECTION) {
                g.setX(objectInitialX + deltaX);
                g.setY(objectInitialY + deltaY);
                renderCurrentPage();
            }
        });

        canvas.setOnMouseReleased(e -> {
            isResizing = false;
            resizeHandleIndex = -1;
        });

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(splitPane);
        root.setBottom(statusLabel);
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-background-color: white;");

        Scene scene = new Scene(root, 1280, 900, Color.WHITE);
        stage.setTitle("BDoc Editor - Реставрация печатных изданий");
        stage.setScene(scene);
        stage.show();
        loadInitialSample(stage);
    }

    /**
     * Если object — "сырой" объект мастера без локального override, создаёт
     * его копию с masterSourceId и overriddenProperties={"geometry"},
     * добавляет в page.getObjects() и возвращает копию. Если object уже
     * является override или страница не привязана к мастеру — возвращает
     * object без изменений.
     */
    private BdocObject materializeOverrideIfNeeded(PageModel page, MasterPage masterPage, BdocObject object) {
        if (masterPage == null || object.isMasterOverride()) {
            return object;
        }
        if (masterPage.findObject(object.getId()) == null) {
            return object;
        }

        Set<String> overridden = new HashSet<>();
        overridden.add("geometry");
        Geometry clonedGeometry = object.getGeometry().copy();

        BdocObject override;
        if (object instanceof TextFrame tf) {
            override = new TextFrame(tf.getId(), tf.getLayerRef(), clonedGeometry, tf.getStoryRef(), tf.getId(), overridden);
        } else if (object instanceof ImageFrame imf) {
            override = new ImageFrame(imf.getId(), imf.getLayerRef(), clonedGeometry, imf.getAssetRef(), imf.getId(), overridden);
        } else if (object instanceof VectorShape vs) {
            override = new VectorShape(vs.getId(), vs.getLayerRef(), clonedGeometry, vs.getShapeType(), vs.getId(), overridden);
        } else if (object instanceof HeaderFooterRule hfr) {
            override = new HeaderFooterRule(
                    hfr.getId(), hfr.getLayerRef(), clonedGeometry,
                    hfr.getZone(), hfr.getTextTemplate(), hfr.getStyleRef(),
                    hfr.getId(), overridden);
        } else {
            return object;
        }

        page.getObjects().add(override);
        return override;
    }

    /** Удаляет локальный override и возвращает исходный объект мастера. */
    private BdocObject restoreToMaster(PageModel page, MasterPage masterPage, BdocObject overrideObject) {
        if (masterPage == null || !overrideObject.isMasterOverride()) {
            return overrideObject;
        }
        page.getObjects().removeIf(o -> o.getId().equals(overrideObject.getId()) && o.isMasterOverride());
        return masterPage.findObject(overrideObject.getMasterSourceId());
    }

    private void loadInitialSample(Stage stage) {
        try {
            File sampleFile = Files.createTempFile("bdoc-sample", ".bdoc").toFile();
            sampleFile.deleteOnExit();
            SampleDocuments.writeSample(sampleFile);
            openDocument(sampleFile);
        } catch (Exception ex) {
            showError("Failed to create sample document", ex.getMessage());
        }
    }

    private void onNewSample(Stage stage) {
        try {
            File sampleFile = Files.createTempFile("bdoc-sample", ".bdoc").toFile();
            sampleFile.deleteOnExit();
            SampleDocuments.writeSample(sampleFile);
            openDocument(sampleFile);
        } catch (Exception ex) {
            showError("Failed to create sample document", ex.getMessage());
        }
    }

    private void onOpen(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open BDoc file");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("BDoc files", "*.bdoc"));
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            openDocument(file);
        }
    }

    private void onSaveAs(Stage stage) {
        if (document == null) {
            showError("Save error", "No document is currently open.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save BDoc file as");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("BDoc files", "*.bdoc"));

        if (currentFile != null) {
            fc.setInitialFileName("restored_" + currentFile.getName());
        } else {
            fc.setInitialFileName("document.bdoc");
        }

        File target = fc.showSaveDialog(stage);
        if (target == null) {
            return;
        }
        try {
            statusLabel.setText("Re-packing BDoc archive with new layout and text...");
            saveDocument(document, target);
            statusLabel.setText("Saved to " + target.getAbsolutePath() + " [Pack OK]");
        } catch (IOException ex) {
            showError("Save error", "Failed to compile document package: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void openDocument(File file) {
        try {
            DocumentHandle previous = document;

            DocumentHandle opened = serializer.open(file);
            document = opened;
            currentFile = file;
            currentPageIndex = 1;

            if (previous != null) {
                previous.close();
            }

            refreshTree();
            documentTree.getSelectionModel().select(0);
            renderCurrentPage();

            statusLabel.setText("Opened: " + file.getName()
                    + " (" + document.getPageCount() + " pages)");
        } catch (Exception ex) {
            showError("Open error", ex.getMessage());
        }
    }

    private void onPageSelected(int listIndex) {
        if (document == null) {
            return;
        }
        int pageIndex = listIndex + 1;
        if (pageIndex < 1 || pageIndex > document.getPageCount()) {
            return;
        }
        currentPageIndex = pageIndex;
        renderCurrentPage();
    }

    private void renderCurrentPage() {
        if (document == null) {
            return;
        }
        try {
            PageModel page = document.loadPage(currentPageIndex);
            canvas.setWidth(page.getWidth());
            canvas.setHeight(page.getHeight());
            pageRenderer.render(canvas.getGraphicsContext2D(), document, page, selectedObject);
        } catch (IOException ex) {
            showError("Render error", ex.getMessage());
        }
    }

    private void refreshTree() {
        documentTree.getItems().clear();
        documentTree.getItems().add("Document: " + document.getTitle());

        for (int i = 1; i <= document.getPageCount(); i++) {
            documentTree.getItems().add("Page " + i);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updatePropertiesPane(PageModel page, MasterPage masterPage, BdocObject object) {
        propertiesContainer.getChildren().clear();

        LayerModel objectLayer = page.getLayers().stream()
                .filter(l -> l.getId().equals(object.getLayerRef()))
                .findFirst()
                .orElse(null);

        if (objectLayer == null) return;

        Label objectInfo = new Label("Object ID: " + object.getId() + "\nType: " + object.getType());
        objectInfo.setStyle("-fx-text-fill: #475569;");

        Label layerLabel = new Label("Layer: " + objectLayer.getName() + " (" + objectLayer.getRole() + ")");
        layerLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        visibleCheckBox = new CheckBox("Layer Visible");
        visibleCheckBox.setSelected(objectLayer.isVisible());
        visibleCheckBox.setOnAction(e -> {
            objectLayer.setVisible(visibleCheckBox.isSelected());
            renderCurrentPage();
        });

        Label opacityLabel = new Label("Layer Opacity: " + Math.round(objectLayer.getOpacity() * 100) + "%");
        opacitySlider = new Slider(0.0, 1.0, objectLayer.getOpacity());
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            objectLayer.setOpacity(newVal.doubleValue());
            opacityLabel.setText("Layer Opacity: " + Math.round(newVal.doubleValue() * 100) + "%");
            renderCurrentPage();
        });

        propertiesContainer.getChildren().addAll(
                objectInfo,
                new Separator(),
                layerLabel,
                visibleCheckBox,
                opacityLabel,
                opacitySlider
        );

        // Кнопка "Сбросить к параметрам мастера" — только для override-объектов
        if (object.isMasterOverride()) {
            Label masterInfo = new Label("Linked to master: " + object.getMasterSourceId());
            masterInfo.setStyle("-fx-text-fill: #B45309; -fx-font-size: 11px; -fx-padding: 8 0 0 0;");

            Button restoreBtn = new Button("Restore to Master");
            restoreBtn.setOnAction(e -> {
                BdocObject restored = restoreToMaster(page, masterPage, object);
                selectedObject = restored;
                statusLabel.setText("Restored to master: " + (restored != null ? restored.getId() : "?"));
                renderCurrentPage();
                if (restored != null) {
                    updatePropertiesPane(page, masterPage, restored);
                } else {
                    propertiesContainer.getChildren().clear();
                }
            });

            propertiesContainer.getChildren().addAll(masterInfo, restoreBtn);
        }
    }

    private void updateTextEditorPane(TextFrame textFrame, StoryModel story) {
        propertiesContainer.getChildren().clear();

        Label titleLabel = new Label("Text Frame Content");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Label infoLabel = new Label("Frame ID: " + textFrame.getId() + "\nStory Ref: " + textFrame.getStoryRef());
        infoLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        storyTextArea = new TextArea();
        storyTextArea.setPrefHeight(300);
        storyTextArea.setWrapText(true);

        if (story != null) {
            storyTextArea.setText(story.getJoinedText());
        }

        storyTextArea.textProperty().addListener((obs, oldText, newText) -> {
            if (story != null) {
                story.getParagraphs().clear();
                String[] lines = newText.split("\n");
                for (String line : lines) {
                    story.getParagraphs().add(new Paragraph("body", "body-text", line));
                }
                renderCurrentPage();
            }
        });

        Label hintLabel = new Label("Tip: You can paste cleaned OCR text here. Advertisement blocks can be wiped instantly.");
        hintLabel.setWrapText(true);
        hintLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-style: italic;");

        propertiesContainer.getChildren().addAll(
                titleLabel,
                infoLabel,
                new Separator(),
                storyTextArea,
                hintLabel
        );
    }

    private void saveDocument(DocumentHandle handle, File targetFile) throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        CBORMapper cborMapper = new CBORMapper();

        java.util.Map<String, String> env = new java.util.HashMap<>();
        env.put("create", "true");

        if (targetFile.exists()) {
            targetFile.delete();
        }

        java.net.URI uri = java.net.URI.create("jar:file:" + targetFile.toURI().getPath());

        try (java.nio.file.FileSystem zipfs = java.nio.file.FileSystems.newFileSystem(uri, env)) {
            java.nio.file.Files.createDirectories(zipfs.getPath("/pages"));
            java.nio.file.Files.createDirectories(zipfs.getPath("/stories"));
            java.nio.file.Files.createDirectories(zipfs.getPath("/resources"));
            java.nio.file.Files.createDirectories(zipfs.getPath("/templates"));

            for (String storyId : handle.getStoryIds()) {
                StoryModel story = handle.getStory(storyId);
                if (story != null) {
                    java.nio.file.Path storyPath = zipfs.getPath("/stories/" + storyId + ".json");
                    try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(storyPath)) {
                        jsonMapper.writerWithDefaultPrettyPrinter().writeValue(os, story);
                    }
                }
            }

            for (Integer index : handle.getPageIndices()) {
                PageModel page = handle.loadPage(index);
                java.nio.file.Path pagePath = zipfs.getPath("/pages/page-" + index + ".cbor");
                try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(pagePath)) {
                    cborMapper.writeValue(os, page);
                }
            }

            // Шаблоны мастер-страниц — новое в этой версии сохранения
            java.util.List<java.util.Map<String, Object>> templatesList = new java.util.ArrayList<>();
            for (MasterPage masterPage : handle.getTemplates().getMasterPages()) {
                java.nio.file.Path templatePath = zipfs.getPath("/templates/" + masterPage.getId() + ".json");
                try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(templatePath)) {
                    jsonMapper.writerWithDefaultPrettyPrinter().writeValue(os, masterPage);
                }
                java.util.Map<String, Object> tEntry = new java.util.HashMap<>();
                tEntry.put("id", masterPage.getId());
                tEntry.put("file", "templates/" + masterPage.getId() + ".json");
                templatesList.add(tEntry);
            }

            java.util.Map<String, Object> manifestMap = new java.util.HashMap<>();
            manifestMap.put("id", handle.getId());
            manifestMap.put("title", handle.getTitle());
            manifestMap.put("documentType", handle.getDocumentType());
            manifestMap.put("version", "0.1-composite");

            java.util.List<java.util.Map<String, Object>> pagesList = new java.util.ArrayList<>();
            for (Integer index : handle.getPageIndices()) {
                java.util.Map<String, Object> pEntry = new java.util.HashMap<>();
                pEntry.put("index", index);
                pEntry.put("id", "page-" + index);
                pEntry.put("file", "pages/page-" + index + ".cbor");
                pagesList.add(pEntry);
            }
            manifestMap.put("pages", pagesList);

            java.util.List<java.util.Map<String, Object>> storiesList = new java.util.ArrayList<>();
            for (String storyId : handle.getStoryIds()) {
                java.util.Map<String, Object> sEntry = new java.util.HashMap<>();
                sEntry.put("id", storyId);
                sEntry.put("file", "stories/" + storyId + ".json");
                storiesList.add(sEntry);
            }
            manifestMap.put("stories", storiesList);
            manifestMap.put("templates", templatesList);

            java.nio.file.Path manifestPath = zipfs.getPath("/manifest.json");
            try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(manifestPath)) {
                jsonMapper.writerWithDefaultPrettyPrinter().writeValue(os, manifestMap);
            }

            if (handle.getStyles() != null) {
                java.nio.file.Path stylesPath = zipfs.getPath("/styles.json");
                try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(stylesPath)) {
                    jsonMapper.writerWithDefaultPrettyPrinter().writeValue(os, handle.getStyles());
                }
            }
        }
    }
}