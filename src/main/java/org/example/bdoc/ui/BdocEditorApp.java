package org.example.bdoc.ui;

import atlantafx.base.theme.PrimerDark;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.bdoc.io.BdocContainerSerializer;
import org.example.bdoc.io.BdocIntegrityValidator;
import org.example.bdoc.io.BdocValidationException;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.*;
import org.example.bdoc.plugin.BdocSettings;
import org.example.bdoc.plugin.PluginContext;
import org.example.bdoc.plugin.PluginDescriptor;
import org.example.bdoc.render.PageRenderer;
import org.example.bdoc.ui.properties.DefaultGeometryPropertiesPanelFactory;
import org.example.bdoc.ui.properties.PropertiesPanelFactory;
import org.example.bdoc.ui.properties.TextEditorPropertiesPanelFactory;
import org.example.bdoc.ui.task.TaskQueue;
import org.example.bdoc.ui.tool.DtpToolStrategy;
import org.example.bdoc.ui.tool.SelectionToolStrategy;
import org.example.bdoc.ui.tool.TextToolStrategy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Ядро редактора (аналог IdeFrame в IntelliJ Platform). Не содержит знаний
 * о конкретных инструментах — только диспетчеризует события мыши активной
 * DtpToolStrategy и собирает правую панель через зарегистрированные
 * PropertiesPanelFactory. Весь ephemeral-стейт Drag/Resize живёт внутри
 * SelectionToolStrategy, а не здесь (см. обсуждение Этапа 2, пункт 2).
 */
public class BdocEditorApp extends Application implements EditorContext {

    private final BdocContainerSerializer serializer = new BdocContainerSerializer();
    private final PageRenderer pageRenderer = new PageRenderer();
    private final BdocIntegrityValidator integrityValidator = new BdocIntegrityValidator();

    private DocumentHandle document;
    private File currentFile;
    private int currentPageIndex = 1;

    private Canvas canvas;
    private TreeView<TreeNodeData> documentTree;
    private Label statusLabel;
    private VBox propertiesContainer;

    private String currentToolId = "SELECTION";
    private BdocObject selectedObject = null;

    private TaskQueue taskQueue;
    private Stage primaryStage;

    // === SETTINGS PERSISTENCE: держим ссылки, чтобы прочитать значения при закрытии окна ===
    private SplitPane rootSplitPane;
    private Menu recentFilesMenu;

    /** Узел дерева: либо страница, либо слой, либо объект. */
    private enum NodeKind { DOCUMENT, PAGE, LAYER, OBJECT }

    private static final class TreeNodeData {
        final NodeKind kind;
        final int pageIndex;
        final LayerModel layer;
        final BdocObject object;
        final boolean isMasterLocked;

        static TreeNodeData document() {
            return new TreeNodeData(NodeKind.DOCUMENT, -1, null, null, false);
        }
        static TreeNodeData page(int pageIndex) {
            return new TreeNodeData(NodeKind.PAGE, pageIndex, null, null, false);
        }
        static TreeNodeData layer(int pageIndex, LayerModel layer) {
            return new TreeNodeData(NodeKind.LAYER, pageIndex, layer, null, false);
        }
        static TreeNodeData object(int pageIndex, LayerModel layer, BdocObject object, boolean masterLocked) {
            return new TreeNodeData(NodeKind.OBJECT, pageIndex, layer, object, masterLocked);
        }

        private TreeNodeData(NodeKind kind, int pageIndex, LayerModel layer, BdocObject object, boolean isMasterLocked) {
            this.kind = kind;
            this.pageIndex = pageIndex;
            this.layer = layer;
            this.object = object;
            this.isMasterLocked = isMasterLocked;
        }
    }

    @Override
    public void start(Stage stage) {
        // === THEME: AtlantaFX PrimerDark по умолчанию, до создания Scene ===
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        this.primaryStage = stage;
        this.taskQueue = new TaskQueue(stage);

        registerBuiltinPlugins();

        // === SETTINGS PERSISTENCE: восстанавливаем последний активный инструмент ===
        currentToolId = BdocSettings.getInstance().loadActiveTool();

        documentTree = new TreeView<>();
        documentTree.setPrefWidth(260);
        documentTree.setCellFactory(tv -> new TreeNodeCell());
        documentTree.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem == null) return;
            onTreeSelectionChanged(newItem.getValue());
        });

        canvas = new Canvas(595, 842);
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setPadding(new Insets(24));
        canvasPane.getStyleClass().add("bdoc-canvas-pane");

        BorderPane propertiesPane = new BorderPane();
        propertiesPane.setPadding(new Insets(12));
        propertiesPane.setPrefWidth(240);
        propertiesPane.getStyleClass().add("bdoc-properties-pane");
        Label propTitle = new Label("Properties & Layers");
        propTitle.getStyleClass().add("bdoc-section-title");
        propertiesPane.setTop(propTitle);

        propertiesContainer = new VBox(10);
        propertiesContainer.setPadding(new Insets(10, 0, 0, 0));
        propertiesPane.setCenter(propertiesContainer);

        SplitPane splitPane = new SplitPane(documentTree, canvasPane, propertiesPane);
        this.rootSplitPane = splitPane;

        // === SETTINGS PERSISTENCE: восстанавливаем позиции divider'ов ===
        double[] savedDividers = BdocSettings.getInstance().loadDividerPositions(new double[]{0.22, 0.76});
        splitPane.setDividerPositions(savedDividers[0], savedDividers[1]);

        statusLabel = new Label("No document loaded");
        statusLabel.getStyleClass().add("bdoc-status-bar");
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        MenuBar menuBar = buildMenuBar(stage, splitPane, documentTree, propertiesPane);
        ToolBar fileToolBar = buildFileToolBar(stage);
        ToolBar toolPalette = buildToolPalette();

        VBox topBar = new VBox(menuBar, fileToolBar);

        canvas.setOnMousePressed(e -> dispatchToActiveTool(strategy -> strategy.onMousePressed(e, this)));
        canvas.setOnMouseDragged(e -> dispatchToActiveTool(strategy -> strategy.onMouseDragged(e, this)));
        canvas.setOnMouseReleased(e -> dispatchToActiveTool(strategy -> strategy.onMouseReleased(e, this)));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(toolPalette);
        root.setCenter(splitPane);
        root.setBottom(statusLabel);
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 1360, 900);
        scene.getStylesheets().add(getClass().getResource("/theme/bdoc-theme.css").toExternalForm());
        stage.setTitle("BDoc Editor - Реставрация печатных изданий");
        stage.setScene(scene);

        // === SETTINGS PERSISTENCE: восстанавливаем размер/позицию/maximized окна ===
        double[] bounds = BdocSettings.getInstance().loadWindowBounds();
        stage.setX(bounds[0]);
        stage.setY(bounds[1]);
        stage.setWidth(bounds[2]);
        stage.setHeight(bounds[3]);
        if (BdocSettings.getInstance().isWindowMaximized()) {
            stage.setMaximized(true);
        }

        // === SETTINGS PERSISTENCE: сохраняем всё при закрытии окна ===
        stage.setOnCloseRequest(e -> {
            BdocSettings.getInstance().saveWindowBounds(
                    stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight(), stage.isMaximized());
            BdocSettings.getInstance().saveDividerPositions(splitPane.getDividerPositions());
            BdocSettings.getInstance().saveActiveTool(currentToolId);
        });

        stage.show();
        loadInitialSample(stage);
    }

    // ================= Регистрация встроенных плагинов (ядра) =================

    private void registerBuiltinPlugins() {
        PluginContext ctx = PluginContext.getInstance();

        PluginDescriptor core = new PluginDescriptor(
                "bdoc-core", "org.example.bdoc.core", "BDoc Core", "0.1", "BDoc Team");
        ctx.registerPlugin(core);

        ctx.registerTool(new SelectionToolStrategy());
        ctx.registerTool(new TextToolStrategy());

        ctx.registerPropertiesFactory(new DefaultGeometryPropertiesPanelFactory());
        ctx.registerPropertiesFactory(new TextEditorPropertiesPanelFactory());
    }

    private ToolBar buildDynamicToolBar() {
        ToolBar toolBar = new ToolBar(new Label("BDoc Editor v0.1"), new Separator());
        ToggleGroup toolGroup = new ToggleGroup();

        for (DtpToolStrategy tool : PluginContext.getInstance().getRegisteredTools().values()) {
            ToggleButton btn = new ToggleButton(tool.getLabel());
            btn.setToggleGroup(toolGroup);
            if (tool.getToolId().equals(currentToolId)) btn.setSelected(true);

            btn.setOnAction(e -> {
                DtpToolStrategy oldTool = PluginContext.getInstance().getTool(currentToolId);
                if (oldTool != null) oldTool.deactivate(this);

                currentToolId = tool.getToolId();
                tool.activate(this);
                statusLabel.setText("Active Tool: " + tool.getLabel());
                renderCurrentPage();
            });
            toolBar.getItems().add(btn);
        }
        return toolBar;
    }

    private void dispatchToActiveTool(Consumer<DtpToolStrategy> action) {
        if (document == null) return;
        DtpToolStrategy strategy = PluginContext.getInstance().getTool(currentToolId);
        if (strategy != null) {
            try {
                action.accept(strategy);
            } catch (Exception ex) {
                showError("Tool Error", ex.getMessage());
            }
        }
    }

    // ================= EditorContext: узкий контракт для плагинов =================

    @Override
    public DocumentHandle getDocument() { return document; }

    @Override
    public PageModel getCurrentPage() {
        try {
            return document.loadPage(currentPageIndex);
        } catch (IOException ex) {
            showError("Page load error", ex.getMessage());
            return null;
        }
    }

    @Override
    public MasterPage getCurrentMasterPage() {
        PageModel page = getCurrentPage();
        return page != null ? document.getMasterPage(page.getTemplateRef()) : null;
    }

    @Override
    public int getCurrentPageIndex() { return currentPageIndex; }

    @Override
    public BdocObject getSelectedObject() { return selectedObject; }

    @Override
    public void setSelectedObject(BdocObject object) {
        this.selectedObject = object;
        rebuildPropertiesPanel(object);
        selectTreeNodeFor(object);
    }

    @Override
    public BdocObject materializeOverrideIfNeeded(BdocObject object) {
        return materializeOverrideIfNeededInternal(getCurrentPage(), getCurrentMasterPage(), object);
    }

    @Override
    public BdocObject replacePathData(BdocObject object, PathModel newPathData) {
        if (!(object instanceof VectorShape vs)) {
            return object;
        }
        VectorShape updated = new VectorShape(
                vs.getId(), vs.getLayerRef(), vs.getGeometry(), vs.getShapeType(),
                vs.getMasterSourceId(), vs.getOverriddenProperties(), vs.isVisible(),
                vs.getClipGeometry(), vs.getMaskRef(), vs.isMask(), vs.isArtifact(),
                vs.getArtifactType(), vs.getTextWrap(), newPathData, vs.getTransform()
        );
        PageModel page = getCurrentPage();
        for (int i = 0; i < page.getObjects().size(); i++) {
            if (page.getObjects().get(i).getId().equals(vs.getId())) {
                page.getObjects().set(i, updated);
                break;
            }
        }
        return updated;
    }

    @Override
    public void renderCurrentPage() {
        if (document == null) return;
        try {
            PageModel page = document.loadPage(currentPageIndex);
            canvas.setWidth(page.getWidth());
            canvas.setHeight(page.getHeight());
            GraphicsContext gc = canvas.getGraphicsContext2D();
            pageRenderer.render(gc, document, page);

            DtpToolStrategy strategy = PluginContext.getInstance().getTool(currentToolId);
            if (strategy != null) {
                strategy.renderOverlay(gc, this);
            }
        } catch (IOException ex) {
            showError("Render error", ex.getMessage());
        }
    }

    @Override
    public void refreshTree() { refreshTreeInternal(); }

    @Override
    public void setStatusText(String text) { statusLabel.setText(text); }

    @Override
    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public double[] toLocalPoint(double screenX, double screenY, BdocObject object) {
        TransformModel t = object.getTransform();
        if (t == null || t.isIdentity()) {
            return new double[]{screenX, screenY};
        }
        Geometry g = object.getGeometry();
        double centerX = g.getX() + g.getWidth() / 2.0;
        double centerY = g.getY() + g.getHeight() / 2.0;

        double px = screenX - t.getTranslateX();
        double py = screenY - t.getTranslateY();

        px -= centerX;
        py -= centerY;

        double rad = Math.toRadians(-t.getRotationDegrees());
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double rx = px * cos - py * sin;
        double ry = px * sin + py * cos;

        rx /= t.getScaleX();
        ry /= t.getScaleY();

        return new double[]{rx + centerX, ry + centerY};
    }

    @Override
    public void runWriteAction(Runnable mutation) {
        // v0.1: синхронное выполнение, без блокировки холста и без записи
        // Undo-шага — заготовка транзакционного API (Этап 2, пункт 9.2).
        // Полная реализация (блокировка, атомарная сериализация CBOR,
        // история изменений) — Этап 2.10.
        mutation.run();
    }

    @Override
    public TaskQueue getTaskQueue() { return taskQueue; }

    // ================= Правая панель через фабрики =================

    private void rebuildPropertiesPanel(BdocObject object) {
        propertiesContainer.getChildren().clear();
        if (object == null) return;
        PropertiesPanelFactory factory = PluginContext.getInstance().findPropertiesFactory(object);
        if (factory != null) {
            factory.buildPanel(propertiesContainer, object, this);
        }
    }

    // ================= Дерево документа =================

    private class TreeNodeCell extends TreeCell<TreeNodeData> {
        @Override
        protected void updateItem(TreeNodeData data, boolean empty) {
            super.updateItem(data, empty);
            if (empty || data == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
                return;
            }

            switch (data.kind) {
                case DOCUMENT -> {
                    setText(document != null ? "Document: " + document.getTitle() : "Document");
                    setGraphic(null);
                    setStyle("-fx-font-weight: bold;");
                }
                case PAGE -> {
                    setText("Page " + data.pageIndex);
                    setGraphic(null);
                    setStyle("");
                }
                case LAYER -> {
                    CheckBox eyeBox = new CheckBox(data.layer.getName() + " (" + data.layer.getRole() + ")");
                    eyeBox.setSelected(data.layer.isVisible());
                    eyeBox.setOnAction(e -> {
                        data.layer.setVisible(eyeBox.isSelected());
                        renderCurrentPage();
                    });
                    setGraphic(eyeBox);
                    setText(null);
                    setStyle("-fx-font-weight: bold;");
                }
                case OBJECT -> {
                    CheckBox eyeBox = new CheckBox();
                    eyeBox.setSelected(data.object.isVisible());
                    String label = (data.isMasterLocked ? "🔒 " : "") + data.object.getId()
                            + " [" + data.object.getType() + "]";
                    eyeBox.setText(label);
                    eyeBox.setOnAction(e -> {
                        try {
                            PageModel page = document.loadPage(data.pageIndex);
                            MasterPage masterPage = document.getMasterPage(page.getTemplateRef());
                            BdocObject target = data.isMasterLocked
                                    ? materializeOverrideIfNeededInternal(page, masterPage, data.object)
                                    : data.object;
                            target.setVisible(eyeBox.isSelected());
                            renderCurrentPage();
                            if (data.isMasterLocked) {
                                refreshTree();
                            }
                        } catch (IOException ex) {
                            showError("Visibility toggle error", ex.getMessage());
                        }
                    });
                    HBox box = new HBox(eyeBox);
                    setGraphic(box);
                    setText(null);
                    setStyle(data.isMasterLocked ? "-fx-opacity: 0.55; -fx-font-style: italic;" : "");
                }
            }
        }
    }

    private void onTreeSelectionChanged(TreeNodeData data) {
        if (data == null || document == null) return;

        if (data.kind == NodeKind.PAGE) {
            currentPageIndex = data.pageIndex;
            selectedObject = null;
            propertiesContainer.getChildren().clear();

            try {
                PageModel page = document.loadPage(currentPageIndex);
                Unit currentUnit = Unit.fromString(page.getUnit());
                double displayW = currentUnit.fromPoints(page.getWidth());
                double displayH = currentUnit.fromPoints(page.getHeight());

                statusLabel.setText(String.format(
                        "Active Page: %d | Format: %.1f × %.1f %s (%.0f × %.0f pt)",
                        currentPageIndex, displayW, displayH, page.getUnit(), page.getWidth(), page.getHeight()));
            } catch (IOException ex) {
                statusLabel.setText("Page selected (Render Error)");
            }

            renderCurrentPage();
            return;
        }

        if (data.kind == NodeKind.OBJECT) {
            currentPageIndex = data.pageIndex;
            setSelectedObject(data.object);
            renderCurrentPage();
        }
    }

    private void selectTreeNodeFor(BdocObject object) {
        if (object == null || documentTree.getRoot() == null) return;
        findAndSelectRecursive(documentTree.getRoot(), object);
    }

    private boolean findAndSelectRecursive(TreeItem<TreeNodeData> item, BdocObject target) {
        if (item.getValue() != null && item.getValue().kind == NodeKind.OBJECT
                && item.getValue().object == target) {
            documentTree.getSelectionModel().select(item);
            return true;
        }
        for (TreeItem<TreeNodeData> child : item.getChildren()) {
            if (findAndSelectRecursive(child, target)) {
                return true;
            }
        }
        return false;
    }

    private void refreshTreeInternal() {
        TreeItem<TreeNodeData> root = new TreeItem<>(TreeNodeData.document());
        root.setExpanded(true);

        for (int pageIndex = 1; pageIndex <= document.getPageCount(); pageIndex++) {
            TreeItem<TreeNodeData> pageItem = new TreeItem<>(TreeNodeData.page(pageIndex));
            pageItem.setExpanded(pageIndex == currentPageIndex);

            try {
                PageModel page = document.loadPage(pageIndex);
                MasterPage masterPage = document.getMasterPage(page.getTemplateRef());
                List<BdocObject> effectiveObjects = pageRenderer.resolveEffectiveObjects(page, masterPage);

                for (LayerModel layer : page.getLayers()) {
                    TreeItem<TreeNodeData> layerItem = new TreeItem<>(TreeNodeData.layer(pageIndex, layer));
                    layerItem.setExpanded(true);

                    for (BdocObject object : effectiveObjects) {
                        if (!object.getLayerRef().equals(layer.getId())) continue;
                        boolean isMasterLocked = pageRenderer.isRawMasterObject(object, masterPage);
                        layerItem.getChildren().add(new TreeItem<>(
                                TreeNodeData.object(pageIndex, layer, object, isMasterLocked)));
                    }
                    pageItem.getChildren().add(layerItem);
                }
            } catch (IOException ex) {
                showError("Tree build error", "Failed to load page " + pageIndex + ": " + ex.getMessage());
            }

            root.getChildren().add(pageItem);
        }

        documentTree.setRoot(root);
        documentTree.setShowRoot(true);
    }

    // ================= Файловые операции =================

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

        try {
            integrityValidator.validate(document);
        } catch (BdocValidationException validationEx) {
            showValidationErrors("Cannot save document", validationEx);
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

            // === SETTINGS PERSISTENCE: обновляем Recent Files после успешного сохранения ===
            currentFile = target;
            BdocSettings.getInstance().pushRecentFile(target.getAbsolutePath());
            rebuildRecentFilesMenu();
        } catch (IOException ex) {
            showError("Save error", "Failed to compile document package: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void openDocument(File file) {
        try {
            DocumentHandle previous = document;

            DocumentHandle opened = serializer.open(file);

            try {
                integrityValidator.validate(opened);
            } catch (BdocValidationException validationEx) {
                opened.close();
                showValidationErrors("Cannot open document", validationEx);
                return;
            }

            document = opened;
            currentFile = file;
            currentPageIndex = 1;
            selectedObject = null;

            if (previous != null) {
                previous.close();
            }

            refreshTree();
            renderCurrentPage();

            statusLabel.setText("Opened: " + file.getName()
                    + " (" + document.getPageCount() + " pages)");

            // === SETTINGS PERSISTENCE: обновляем Recent Files после успешного открытия ===
            BdocSettings.getInstance().pushRecentFile(file.getAbsolutePath());
            rebuildRecentFilesMenu();
        } catch (Exception ex) {
            showError("Open error", ex.getMessage());
        }
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

    // ================= Master overrides =================

    private BdocObject materializeOverrideIfNeededInternal(PageModel page, MasterPage masterPage, BdocObject object) {
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
        refreshTree();
        return override;
    }

    private BdocObject restoreToMaster(PageModel page, MasterPage masterPage, BdocObject overrideObject) {
        if (masterPage == null || !overrideObject.isMasterOverride()) {
            return overrideObject;
        }
        page.getObjects().removeIf(o -> o.getId().equals(overrideObject.getId()) && o.isMasterOverride());
        return masterPage.findObject(overrideObject.getMasterSourceId());
    }

    @Override
    public BdocObject restoreToMaster(BdocObject overrideObject) {
        return restoreToMaster(getCurrentPage(), getCurrentMasterPage(), overrideObject);
    }

    private void showValidationErrors(String title, BdocValidationException validationEx) {
        StringBuilder sb = new StringBuilder();
        for (String error : validationEx.getErrors()) {
            sb.append("• ").append(error).append("\n");
        }
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Preflight validation failed (" + validationEx.getErrors().size() + " issue(s))");
        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(500);
        textArea.setPrefHeight(300);
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // === MENU BAR: File/Edit/View/Help + подменю Recent Files ===
    private MenuBar buildMenuBar(Stage stage, SplitPane splitPane, TreeView<TreeNodeData> tree, BorderPane propertiesPane) {
        Menu fileMenu = new Menu("File");
        MenuItem newSampleItem = new MenuItem("New Sample");
        newSampleItem.setOnAction(e -> onNewSample(stage));
        MenuItem openItem = new MenuItem("Open...");
        openItem.setOnAction(e -> onOpen(stage));

        // === MENU BAR: Recent Files — динамический подменю, пересобирается при показе ===
        recentFilesMenu = new Menu("Recent Files");
        rebuildRecentFilesMenu();

        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setOnAction(e -> onSaveAs(stage));
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> stage.close());
        fileMenu.getItems().addAll(newSampleItem, openItem, recentFilesMenu, saveAsItem, new SeparatorMenuItem(), exitItem);

        Menu editMenu = new Menu("Edit");
        MenuItem undoItem = new MenuItem("Undo");
        undoItem.setDisable(true);
        MenuItem redoItem = new MenuItem("Redo");
        redoItem.setDisable(true);
        editMenu.getItems().addAll(undoItem, redoItem);

        Menu viewMenu = new Menu("View");
        CheckMenuItem showTreeItem = new CheckMenuItem("Show Document Tree");
        showTreeItem.setSelected(true);
        showTreeItem.selectedProperty().addListener((obs, was, is) ->
                splitPane.setDividerPositions(is ? 0.22 : 0.0, 0.76));
        CheckMenuItem showPropsItem = new CheckMenuItem("Show Properties Panel");
        showPropsItem.setSelected(true);
        showPropsItem.selectedProperty().addListener((obs, was, is) ->
                splitPane.setDividerPositions(showTreeItem.isSelected() ? 0.22 : 0.0, is ? 0.76 : 1.0));
        viewMenu.getItems().addAll(showTreeItem, showPropsItem);

        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About BDoc Editor");
        aboutItem.setOnAction(e -> {
            Alert about = new Alert(Alert.AlertType.INFORMATION);
            about.setTitle("About");
            about.setHeaderText("BDoc Editor v0.1");
            about.setContentText("Редактор архивных печатных изданий на JavaFX.");
            about.showAndWait();
        });
        helpMenu.getItems().add(aboutItem);

        return new MenuBar(fileMenu, editMenu, viewMenu, helpMenu);
    }

    // === MENU BAR: пересборка списка последних 5 файлов из BdocSettings ===
    private void rebuildRecentFilesMenu() {
        if (recentFilesMenu == null) return;
        recentFilesMenu.getItems().clear();

        List<String> recent = BdocSettings.getInstance().loadRecentFiles();
        if (recent.isEmpty()) {
            MenuItem empty = new MenuItem("(empty)");
            empty.setDisable(true);
            recentFilesMenu.getItems().add(empty);
            return;
        }

        for (String path : recent) {
            MenuItem item = new MenuItem(path);
            item.setOnAction(e -> openDocument(new File(path)));
            recentFilesMenu.getItems().add(item);
        }

        recentFilesMenu.getItems().add(new SeparatorMenuItem());
        MenuItem clearItem = new MenuItem("Clear Recent Files");
        clearItem.setOnAction(e -> {
            BdocSettings.getInstance().clearRecentFiles();
            rebuildRecentFilesMenu();
        });
        recentFilesMenu.getItems().add(clearItem);
    }

    private ToolBar buildFileToolBar(Stage stage) {
        Label titleLabel = new Label("BDoc Editor v0.1");
        Button openBtn = new Button("Open");
        openBtn.getStyleClass().add("bdoc-toolbar-button");
        openBtn.setOnAction(e -> onOpen(stage));
        Button saveAsBtn = new Button("Save As");
        saveAsBtn.getStyleClass().add("bdoc-toolbar-button");
        saveAsBtn.setOnAction(e -> onSaveAs(stage));
        Button newSampleBtn = new Button("New Sample");
        newSampleBtn.getStyleClass().add("bdoc-toolbar-button");
        newSampleBtn.setOnAction(e -> onNewSample(stage));

        ToolBar toolBar = new ToolBar(titleLabel, new Separator(), newSampleBtn, openBtn, saveAsBtn);
        toolBar.getStyleClass().add("bdoc-file-toolbar");
        return toolBar;
    }

    // === TOOL PALETTE: вертикальная палитра инструментов слева ===
    private ToolBar buildToolPalette() {
        ToolBar palette = new ToolBar();
        palette.setOrientation(Orientation.VERTICAL);
        palette.getStyleClass().add("bdoc-tool-palette");

        ToggleGroup toolGroup = new ToggleGroup();
        for (DtpToolStrategy tool : PluginContext.getInstance().getRegisteredTools().values()) {
            ToggleButton btn = new ToggleButton(glyphFor(tool.getToolId()));
            btn.getStyleClass().add("bdoc-tool-palette-button");
            btn.setToggleGroup(toolGroup);
            btn.setTooltip(new Tooltip(tool.getLabel()));
            if (tool.getToolId().equals(currentToolId)) btn.setSelected(true);

            btn.setOnAction(e -> {
                DtpToolStrategy oldTool = PluginContext.getInstance().getTool(currentToolId);
                if (oldTool != null) oldTool.deactivate(this);

                currentToolId = tool.getToolId();
                tool.activate(this);
                statusLabel.setText("Active Tool: " + tool.getLabel());
                renderCurrentPage();
            });
            palette.getItems().add(btn);
        }
        return palette;
    }

    private String glyphFor(String toolId) {
        return switch (toolId) {
            case "SELECTION" -> "\u2196"; // ↖ курсор выделения
            case "TEXT" -> "T";
            default -> "?";
        };
    }
}