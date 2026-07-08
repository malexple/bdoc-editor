package org.example.bdoc.ui;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.bdoc.io.BdocValidationException;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.*;
import org.example.bdoc.plugin.BdocSettings;
import org.example.bdoc.plugin.PluginContext;
import org.example.bdoc.plugin.PluginDescriptor;
import org.example.bdoc.plugin.PluginRuntime;
import org.example.bdoc.render.PageRenderer;
import org.example.bdoc.spi.DtpToolStrategy;
import org.example.bdoc.spi.PropertiesPanelFactory;
import org.example.bdoc.spi.ToolbarActionExtension;
import org.example.bdoc.ui.properties.DefaultGeometryPropertiesPanelFactory;
import org.example.bdoc.ui.properties.TextEditorPropertiesPanelFactory;
import org.example.bdoc.ui.task.TaskQueue;
import org.example.bdoc.ui.tool.SelectionToolStrategy;
import org.example.bdoc.ui.tool.TextToolStrategy;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;

public class BdocEditorApp extends Application
        implements EditorContext, BdocDocumentTreeController.Callbacks {

    private final BdocDocumentIoService documentIo = new BdocDocumentIoService();
    private final PageRenderer pageRenderer = new PageRenderer();
    private final ThemeManager themeManager = new ThemeManager();

    private final Map<ToolbarActionExtension, Button> pluginButtons = new LinkedHashMap<>();

    private DocumentHandle document;
    private File currentFile;
    private int currentPageIndex = 1;
    private String currentToolId = "SELECTION";
    private BdocObject selectedObject;

    private Stage primaryStage;
    private TaskQueue taskQueue;

    private BdocEditorView view;
    private BdocDocumentTreeController treeController;
    private Menu recentFilesMenu;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.taskQueue = new TaskQueue(stage);

        registerBuiltinPlugins();
        currentToolId = BdocSettings.getInstance().loadActiveTool();

        this.view = new BdocEditorView();
        this.treeController = new BdocDocumentTreeController(
                view.getDocumentTree(),
                pageRenderer,
                this
        );

        MenuBar menuBar = buildMenuBar(stage, view.getMainSplitPane());
        ToolBar fileToolBar = buildFileToolBar(stage);
        ToolBar toolPalette = buildToolPalette();

        view.setTopBars(menuBar, fileToolBar);
        view.setToolPalette(toolPalette);

        wireCanvasEvents(view.getCanvas());

        Scene scene = new Scene(view.getRoot(), 1360, 900);
        themeManager.apply(scene, BdocSettings.getInstance().loadTheme());

        stage.setTitle("BDoc Editor - Реставрация печатных изданий");
        stage.setScene(scene);

        restoreWindowState(stage);
        installClosePersistence(stage);

        stage.show();
        loadInitialSample();
    }

    private void wireCanvasEvents(Canvas canvas) {
        canvas.setOnMousePressed(e -> dispatchToActiveTool(strategy -> strategy.onMousePressed(e, this)));
        canvas.setOnMouseDragged(e -> dispatchToActiveTool(strategy -> strategy.onMouseDragged(e, this)));
        canvas.setOnMouseReleased(e -> dispatchToActiveTool(strategy -> strategy.onMouseReleased(e, this)));
    }

    private void restoreWindowState(Stage stage) {
        double[] bounds = BdocSettings.getInstance().loadWindowBounds();
        stage.setX(bounds[0]);
        stage.setY(bounds[1]);
        stage.setWidth(bounds[2]);
        stage.setHeight(bounds[3]);

        if (BdocSettings.getInstance().isWindowMaximized()) {
            stage.setMaximized(true);
        }

        double[] savedDividers = BdocSettings.getInstance()
                .loadDividerPositions(new double[]{0.22, 0.76});
        view.getMainSplitPane().setDividerPositions(savedDividers[0], savedDividers[1]);
    }

    private void installClosePersistence(Stage stage) {
        stage.setOnCloseRequest(e -> {
            BdocSettings.getInstance().saveWindowBounds(
                    stage.getX(),
                    stage.getY(),
                    stage.getWidth(),
                    stage.getHeight(),
                    stage.isMaximized()
            );
            BdocSettings.getInstance().saveDividerPositions(view.getMainSplitPane().getDividerPositions());
            BdocSettings.getInstance().saveActiveTool(currentToolId);
        });
    }

    private void registerBuiltinPlugins() {
        PluginContext ctx = PluginContext.getInstance();

        PluginDescriptor core = new PluginDescriptor(
                "bdoc-core",
                "org.example.bdoc.core",
                "BDoc Core",
                "0.1",
                "BDoc Team"
        );
        ctx.registerPlugin(core);

        ctx.registerTool(new SelectionToolStrategy());
        ctx.registerTool(new TextToolStrategy());

        ctx.registerPropertiesFactory(new DefaultGeometryPropertiesPanelFactory());
        ctx.registerPropertiesFactory(new TextEditorPropertiesPanelFactory());
    }

    private void dispatchToActiveTool(Consumer<DtpToolStrategy> action) {
        if (document == null) {
            return;
        }

        DtpToolStrategy strategy = PluginContext.getInstance().getTool(currentToolId);
        if (strategy == null) {
            return;
        }

        try {
            action.accept(strategy);
        } catch (Exception ex) {
            showError("Tool Error", ex.getMessage());
        }
    }

    @Override
    public DocumentHandle getDocument() {
        return document;
    }

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
    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    @Override
    public BdocObject getSelectedObject() {
        return selectedObject;
    }

    @Override
    public void setSelectedObject(BdocObject object) {
        this.selectedObject = object;
        rebuildPropertiesPanel(object);
        treeController.selectTreeNodeFor(object);
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
                vs.getId(),
                vs.getLayerRef(),
                vs.getGeometry(),
                vs.getShapeType(),
                vs.getMasterSourceId(),
                vs.getOverriddenProperties(),
                vs.isVisible(),
                vs.getClipGeometry(),
                vs.getMaskRef(),
                vs.isMask(),
                vs.isArtifact(),
                vs.getArtifactType(),
                vs.getTextWrap(),
                newPathData,
                vs.getTransform()
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
        if (document == null) {
            return;
        }

        try {
            PageModel page = document.loadPage(currentPageIndex);
            Canvas canvas = view.getCanvas();

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
    public void refreshTree() {
        if (document == null) {
            view.getDocumentTree().setRoot(null);
            return;
        }
        treeController.refreshTree(document, currentPageIndex);
    }

    @Override
    public void setStatusText(String text) {
        view.getStatusLabel().setText(text);
    }

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
        mutation.run();
    }

    @Override
    public TaskQueue getTaskQueue() {
        return taskQueue;
    }

    private void rebuildPropertiesPanel(BdocObject object) {
        view.getPropertiesContainer().getChildren().clear();
        if (object == null) {
            return;
        }

        PropertiesPanelFactory factory = PluginContext.getInstance().findPropertiesFactory(object);
        if (factory != null) {
            factory.buildPanel(view.getPropertiesContainer(), object, this);
        }
    }

    private void loadInitialSample() {
        try {
            File sampleFile = documentIo.createSampleDocumentFile();
            openDocument(sampleFile);
        } catch (Exception ex) {
            showError("Failed to create sample document", ex.getMessage());
        }
    }

    private void onNewSample() {
        try {
            File sampleFile = documentIo.createSampleDocumentFile();
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
            documentIo.validate(document);
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
            view.getStatusLabel().setText("Re-packing BDoc archive with new layout and text...");
            documentIo.save(document, target);
            view.getStatusLabel().setText("Saved to " + target.getAbsolutePath() + " [Pack OK]");

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
            DocumentHandle opened = documentIo.open(file);

            try {
                documentIo.validate(opened);
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

            view.getStatusLabel().setText(
                    "Opened: " + file.getName() + " (" + document.getPageCount() + " pages)"
            );

            BdocSettings.getInstance().pushRecentFile(file.getAbsolutePath());
            rebuildRecentFilesMenu();
        } catch (Exception ex) {
            showError("Open error", ex.getMessage());
        }
    }

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
            override = new TextFrame(
                    tf.getId(),
                    tf.getLayerRef(),
                    clonedGeometry,
                    tf.getStoryRef(),
                    tf.getId(),
                    overridden
            );
        } else if (object instanceof ImageFrame imf) {
            override = new ImageFrame(
                    imf.getId(),
                    imf.getLayerRef(),
                    clonedGeometry,
                    imf.getAssetRef(),
                    imf.getId(),
                    overridden
            );
        } else if (object instanceof VectorShape vs) {
            override = new VectorShape(
                    vs.getId(),
                    vs.getLayerRef(),
                    clonedGeometry,
                    vs.getShapeType(),
                    vs.getId(),
                    overridden
            );
        } else if (object instanceof HeaderFooterRule hfr) {
            override = new HeaderFooterRule(
                    hfr.getId(),
                    hfr.getLayerRef(),
                    clonedGeometry,
                    hfr.getZone(),
                    hfr.getTextTemplate(),
                    hfr.getStyleRef(),
                    hfr.getId(),
                    overridden
            );
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

        page.getObjects().removeIf(o ->
                o.getId().equals(overrideObject.getId()) && o.isMasterOverride());

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

    private MenuBar buildMenuBar(Stage stage, SplitPane splitPane) {
        Menu fileMenu = new Menu("File");

        MenuItem newSampleItem = new MenuItem("New Sample");
        newSampleItem.setOnAction(e -> onNewSample());

        MenuItem openItem = new MenuItem("Open...");
        openItem.setOnAction(e -> onOpen(stage));

        recentFilesMenu = new Menu("Recent Files");
        rebuildRecentFilesMenu();

        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setOnAction(e -> onSaveAs(stage));

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(
                newSampleItem,
                openItem,
                recentFilesMenu,
                saveAsItem,
                new SeparatorMenuItem(),
                exitItem
        );

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

    private void rebuildRecentFilesMenu() {
        if (recentFilesMenu == null) {
            return;
        }

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
        newSampleBtn.setOnAction(e -> onNewSample());

        ToolBar toolBar = new ToolBar(titleLabel, new Separator(), newSampleBtn, openBtn, saveAsBtn);

        ModuleLayer layer = PluginRuntime.getLayer();
        ServiceLoader<ToolbarActionExtension> loader =
                ServiceLoader.load(layer, ToolbarActionExtension.class);

        for (ToolbarActionExtension ext : loader) {
            Button pluginBtn = new Button(ext.getActionId());
            pluginBtn.getStyleClass().add("bdoc-toolbar-button");
            pluginBtn.setOnAction(e -> ext.execute(this));
            pluginBtn.setDisable(!ext.isEnabled(this));

            pluginButtons.put(ext, pluginBtn);
            toolBar.getItems().add(new Separator());
            toolBar.getItems().add(pluginBtn);
        }

        toolBar.getStyleClass().add("bdoc-file-toolbar");
        return toolBar;
    }

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

            if (tool.getToolId().equals(currentToolId)) {
                btn.setSelected(true);
            }

            btn.setOnAction(e -> {
                DtpToolStrategy oldTool = PluginContext.getInstance().getTool(currentToolId);
                if (oldTool != null) {
                    oldTool.deactivate(this);
                }

                currentToolId = tool.getToolId();
                tool.activate(this);
                view.getStatusLabel().setText("Active Tool: " + tool.getLabel());
                renderCurrentPage();
            });

            palette.getItems().add(btn);
        }

        return palette;
    }

    private String glyphFor(String toolId) {
        return switch (toolId) {
            case "SELECTION" -> "\u2196";
            case "TEXT" -> "T";
            default -> "?";
        };
    }

    @Override
    public DocumentHandle getDocumentForTree() {
        return document;
    }

    @Override
    public int getCurrentPageIndexForTree() {
        return currentPageIndex;
    }

    @Override
    public void onPageSelectedFromTree(int pageIndex) {
        currentPageIndex = pageIndex;
        selectedObject = null;
        view.getPropertiesContainer().getChildren().clear();

        try {
            PageModel page = document.loadPage(currentPageIndex);
            Unit currentUnit = Unit.fromString(page.getUnit());
            double displayW = currentUnit.fromPoints(page.getWidth());
            double displayH = currentUnit.fromPoints(page.getHeight());

            view.getStatusLabel().setText(String.format(
                    "Active Page: %d | Format: %.1f × %.1f %s (%.0f × %.0f pt)",
                    currentPageIndex,
                    displayW,
                    displayH,
                    page.getUnit(),
                    page.getWidth(),
                    page.getHeight()
            ));
        } catch (IOException ex) {
            view.getStatusLabel().setText("Page selected (Render Error)");
        }

        renderCurrentPage();
    }

    @Override
    public void onObjectSelectedFromTree(int pageIndex, BdocObject object) {
        currentPageIndex = pageIndex;
        setSelectedObject(object);
        renderCurrentPage();
    }

    @Override
    public BdocObject materializeOverrideForTree(PageModel page, MasterPage masterPage, BdocObject object) {
        return materializeOverrideIfNeededInternal(page, masterPage, object);
    }

    @Override
    public void renderCurrentPageFromTree() {
        renderCurrentPage();
    }

    @Override
    public void refreshTreeFromTree() {
        refreshTree();
    }

    @Override
    public void showErrorFromTree(String title, String message) {
        showError(title, message);
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}