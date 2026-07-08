package org.example.bdoc.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.bdoc.i18n.LocalizationManager;
import org.example.bdoc.io.BdocContainerSerializer;
import org.example.bdoc.io.BdocIntegrityValidator;
import org.example.bdoc.io.BdocValidationException;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.*;
import org.example.bdoc.plugin.BdocSettings;
import org.example.bdoc.plugin.PluginContext;
import org.example.bdoc.plugin.PluginDescriptor;
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
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

public class BdocEditorApp extends Application implements EditorContext {

    private final BdocContainerSerializer serializer = new BdocContainerSerializer();
    private final PageRenderer pageRenderer = new PageRenderer();
    private final BdocIntegrityValidator integrityValidator = new BdocIntegrityValidator();
    private final ThemeManager themeManager = new ThemeManager();

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

    private SplitPane mainSplitPane;
    private SplitPane rightSidebarSplitPane;

    private Menu recentFilesMenu;
    private Menu fileMenu;
    private Menu editMenu;
    private Menu viewMenu;
    private Menu helpMenu;

    private MenuItem newSampleItem;
    private MenuItem openItem;
    private MenuItem saveAsItem;
    private MenuItem exitItem;
    private MenuItem undoItem;
    private MenuItem redoItem;
    private MenuItem aboutItem;
    private MenuItem clearRecentFilesItem;

    private CheckMenuItem showTreeItem;
    private CheckMenuItem showPropertiesItem;

    private Label propertiesTitleLabel;
    private Label layersTitleLabel;
    private Label fileToolbarTitleLabel;

    private Button openBtn;
    private Button saveAsBtn;
    private Button newSampleBtn;
    private Button localeBtn;
    private Button themeBtn;

    private BorderPane propertiesPanel;
    private BorderPane layersPanel;
    private ToolBar floatingToolPalette;

    private final Map<ToolbarActionExtension, Button> pluginButtons = new LinkedHashMap<>();

    private enum NodeKind {
        DOCUMENT, PAGE, LAYER, OBJECT
    }

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
        this.primaryStage = stage;
        this.taskQueue = new TaskQueue(stage);

        registerBuiltinPlugins();

        LocalizationManager i18n = LocalizationManager.getInstance();
        String localeTag = BdocSettings.getInstance().loadLocale();
        i18n.setLocale(Locale.forLanguageTag(localeTag));
        currentToolId = BdocSettings.getInstance().loadActiveTool();

        canvas = new Canvas(595, 842);

        documentTree = new TreeView<>();
        documentTree.setShowRoot(true);
        documentTree.setCellFactory(tv -> new TreeNodeCell());
        documentTree.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem == null) {
                return;
            }
            onTreeSelectionChanged(newItem.getValue());
        });

        StackPane pageHolder = new StackPane(canvas);
        pageHolder.getStyleClass().add("page-holder");

        floatingToolPalette = buildToolPalette();
        StackPane.setAlignment(floatingToolPalette, Pos.CENTER_LEFT);
        StackPane.setMargin(floatingToolPalette, new Insets(0, 0, 0, 16));

        StackPane canvasArea = new StackPane(pageHolder, floatingToolPalette);
        canvasArea.setPadding(new Insets(24));
        canvasArea.getStyleClass().add("workspace-area");

        propertiesTitleLabel = new Label();
        propertiesTitleLabel.getStyleClass().add("bdoc-section-title");

        propertiesContainer = new VBox(10);
        propertiesContainer.setPadding(new Insets(10, 0, 0, 0));

        propertiesPanel = new BorderPane();
        propertiesPanel.setTop(propertiesTitleLabel);
        propertiesPanel.setCenter(propertiesContainer);
        propertiesPanel.getStyleClass().add("bdoc-properties-pane");
        propertiesPanel.setPadding(new Insets(12));
        propertiesPanel.setMinWidth(260);

        layersTitleLabel = new Label();
        layersTitleLabel.getStyleClass().add("bdoc-section-title");

        VBox layersContainer = new VBox(10, documentTree);
        VBox.setVgrow(documentTree, Priority.ALWAYS);
        layersContainer.setPadding(new Insets(10, 0, 0, 0));

        layersPanel = new BorderPane();
        layersPanel.setTop(layersTitleLabel);
        layersPanel.setCenter(layersContainer);
        layersPanel.getStyleClass().add("bdoc-layers-pane");
        layersPanel.setPadding(new Insets(12));
        layersPanel.setMinWidth(260);

        rightSidebarSplitPane = new SplitPane(propertiesPanel, layersPanel);
        rightSidebarSplitPane.setOrientation(Orientation.VERTICAL);

        mainSplitPane = new SplitPane(canvasArea, rightSidebarSplitPane);
        SplitPane.setResizableWithParent(rightSidebarSplitPane, false);

        double[] mainDefaults = new double[]{0.78};
        double[] savedMainDividers = BdocSettings.getInstance().loadDividerPositions(mainDefaults);
        if (savedMainDividers.length > 0) {
            mainSplitPane.setDividerPositions(savedMainDividers[0]);
        } else {
            mainSplitPane.setDividerPositions(0.78);
        }

        double[] sidebarDefaults = new double[]{0.60};
        double[] savedSidebarDividers = loadRightSidebarDividerPositions(sidebarDefaults);
        if (savedSidebarDividers.length > 0) {
            rightSidebarSplitPane.setDividerPositions(savedSidebarDividers[0]);
        } else {
            rightSidebarSplitPane.setDividerPositions(0.60);
        }

        statusLabel = new Label();
        statusLabel.getStyleClass().add("bdoc-status-bar");
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        MenuBar menuBar = buildMenuBar(stage);
        ToolBar fileToolBar = buildFileToolBar(stage);

        VBox topBar = new VBox(menuBar, fileToolBar);

        canvas.setOnMousePressed(e -> dispatchToActiveTool(strategy -> strategy.onMousePressed(e, this)));
        canvas.setOnMouseDragged(e -> dispatchToActiveTool(strategy -> strategy.onMouseDragged(e, this)));
        canvas.setOnMouseReleased(e -> dispatchToActiveTool(strategy -> strategy.onMouseReleased(e, this)));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(mainSplitPane);
        root.setBottom(statusLabel);
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 1360, 900);
        themeManager.apply(scene, BdocSettings.getInstance().loadTheme());

        stage.setScene(scene);

        double[] bounds = BdocSettings.getInstance().loadWindowBounds();
        stage.setX(bounds[0]);
        stage.setY(bounds[1]);
        stage.setWidth(bounds[2]);
        stage.setHeight(bounds[3]);

        if (BdocSettings.getInstance().isWindowMaximized()) {
            stage.setMaximized(true);
        }

        stage.setOnCloseRequest(e -> {
            BdocSettings.getInstance().saveWindowBounds(
                    stage.getX(),
                    stage.getY(),
                    stage.getWidth(),
                    stage.getHeight(),
                    stage.isMaximized()
            );

            double[] mainDividers = mainSplitPane.getDividerPositions();
            if (mainDividers.length > 0) {
                BdocSettings.getInstance().saveDividerPositions(mainDividers[0]);
            }

            double[] rightDividers = rightSidebarSplitPane.getDividerPositions();
            if (rightDividers.length > 0) {
                saveRightSidebarDividerPositions(rightDividers[0]);
            }

            BdocSettings.getInstance().saveActiveTool(currentToolId);
        });

        refreshTexts();
        stage.show();
        loadInitialSample(stage);
    }

    private void saveRightSidebarDividerPositions(double divider) {
        BdocSettings.getInstance().node("layout").putDouble("rightSidebarDivider0", divider);
    }

    private double[] loadRightSidebarDividerPositions(double[] defaults) {
        double[] result = new double[defaults.length];
        for (int i = 0; i < defaults.length; i++) {
            result[i] = BdocSettings.getInstance().node("layout").getDouble("rightSidebarDivider" + i, defaults[i]);
        }
        return result;
    }

    private void applyCurrentTheme() {
        if (primaryStage != null && primaryStage.getScene() != null) {
            themeManager.apply(primaryStage.getScene(), BdocSettings.getInstance().loadTheme());
        }
    }

    private void toggleTheme() {
        String current = BdocSettings.getInstance().loadTheme();
        String next = ThemeManager.OBSIDIAN_INK.equalsIgnoreCase(current)
                ? ThemeManager.PAPER_MATTE
                : ThemeManager.OBSIDIAN_INK;

        BdocSettings.getInstance().saveTheme(next);
        applyCurrentTheme();
        refreshTexts();
    }

    private void refreshTexts() {
        LocalizationManager i18n = LocalizationManager.getInstance();


        if (primaryStage != null) {
            primaryStage.setTitle(i18n.get("app.title"));
        }

        if (propertiesTitleLabel != null) {
            propertiesTitleLabel.setText(i18n.get("ui.properties.title"));
        }
        if (layersTitleLabel != null) {
            layersTitleLabel.setText(i18n.get("ui.layers.title"));
        }

        if (statusLabel != null && document == null) {
            statusLabel.setText(i18n.get("status.noDocument"));
        }

        if (fileMenu != null) fileMenu.setText(i18n.get("menu.file"));
        if (editMenu != null) editMenu.setText(i18n.get("menu.edit"));
        if (viewMenu != null) viewMenu.setText(i18n.get("menu.view"));
        if (helpMenu != null) helpMenu.setText(i18n.get("menu.help"));

        if (newSampleItem != null) newSampleItem.setText(i18n.get("menu.file.newSample"));
        if (openItem != null) openItem.setText(i18n.get("menu.file.open"));
        if (saveAsItem != null) saveAsItem.setText(i18n.get("menu.file.saveAs"));
        if (exitItem != null) exitItem.setText(i18n.get("menu.file.exit"));

        if (undoItem != null) undoItem.setText(i18n.get("menu.edit.undo"));
        if (redoItem != null) redoItem.setText(i18n.get("menu.edit.redo"));

        if (showTreeItem != null) showTreeItem.setText(i18n.get("menu.view.showTree"));
        if (showPropertiesItem != null) showPropertiesItem.setText(i18n.get("menu.view.showProperties"));

        if (aboutItem != null) aboutItem.setText(i18n.get("menu.help.about"));

        if (recentFilesMenu != null) recentFilesMenu.setText(i18n.get("menu.file.recent"));
        if (clearRecentFilesItem != null) clearRecentFilesItem.setText(i18n.get("menu.file.recent.clear"));

        if (fileToolbarTitleLabel != null) fileToolbarTitleLabel.setText(i18n.get("app.title.short"));
        if (newSampleBtn != null) newSampleBtn.setText(i18n.get("toolbar.newSample"));
        if (openBtn != null) openBtn.setText(i18n.get("toolbar.open"));
        if (saveAsBtn != null) saveAsBtn.setText(i18n.get("toolbar.saveAs"));
        if (localeBtn != null) localeBtn.setText(i18n.get("toolbar.localeSwitch"));
        if (themeBtn != null) themeBtn.setText(i18n.get("toolbar.themeSwitch"));

        for (Map.Entry<ToolbarActionExtension, Button> entry : pluginButtons.entrySet()) {
            entry.getValue().textProperty().bind(
                    i18n.createStringBinding(
                            entry.getKey().getBundleOwnerId(),
                            entry.getKey().getLabelKey()
                    )
            );
            entry.getValue().setDisable(!entry.getKey().isEnabled(this));
        }
    }

    private void toggleLocale() {
        LocalizationManager i18n = LocalizationManager.getInstance();
        Locale current = i18n.getCurrentLocale();
        Locale next = "ru".equalsIgnoreCase(current.getLanguage()) ? Locale.ENGLISH : new Locale("ru");
        i18n.setLocale(next);
        refreshTexts();
    }

    private void registerBuiltinPlugins() {
        PluginContext ctx = PluginContext.getInstance();
        PluginDescriptor core = new PluginDescriptor("bdoc-core", "org.example.bdoc.core", "BDoc Core", "0.1", "BDoc Team");
        ctx.registerPlugin(core);

        ctx.registerTool(new SelectionToolStrategy());
        ctx.registerTool(new TextToolStrategy());

        ctx.registerPropertiesFactory(new DefaultGeometryPropertiesPanelFactory());
        ctx.registerPropertiesFactory(new TextEditorPropertiesPanelFactory());
    }

    private MenuBar buildMenuBar(Stage stage) {
        fileMenu = new Menu();
        newSampleItem = new MenuItem();
        newSampleItem.setOnAction(e -> onNewSample(stage));

        openItem = new MenuItem();
        openItem.setOnAction(e -> onOpen(stage));

        recentFilesMenu = new Menu();
        rebuildRecentFilesMenu();

        saveAsItem = new MenuItem();
        saveAsItem.setOnAction(e -> onSaveAs(stage));

        exitItem = new MenuItem();
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(
                newSampleItem,
                openItem,
                recentFilesMenu,
                saveAsItem,
                new SeparatorMenuItem(),
                exitItem
        );

        editMenu = new Menu();
        undoItem = new MenuItem();
        undoItem.setDisable(true);

        redoItem = new MenuItem();
        redoItem.setDisable(true);

        editMenu.getItems().addAll(undoItem, redoItem);

        viewMenu = new Menu();

        showTreeItem = new CheckMenuItem();
        showTreeItem.setSelected(true);
        showTreeItem.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            layersPanel.setVisible(isSelected);
            layersPanel.setManaged(isSelected);
            if (isSelected) {
                rightSidebarSplitPane.getItems().setAll(propertiesPanel, layersPanel);
                rightSidebarSplitPane.setDividerPositions(0.60);
            } else {
                rightSidebarSplitPane.getItems().setAll(propertiesPanel);
            }
        });

        showPropertiesItem = new CheckMenuItem();
        showPropertiesItem.setSelected(true);
        showPropertiesItem.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            propertiesPanel.setVisible(isSelected);
            propertiesPanel.setManaged(isSelected);

            if (isSelected && showTreeItem.isSelected()) {
                rightSidebarSplitPane.getItems().setAll(propertiesPanel, layersPanel);
                rightSidebarSplitPane.setDividerPositions(0.60);
            } else if (isSelected) {
                rightSidebarSplitPane.getItems().setAll(propertiesPanel);
            } else if (showTreeItem.isSelected()) {
                rightSidebarSplitPane.getItems().setAll(layersPanel);
            } else {
                rightSidebarSplitPane.getItems().setAll(new Pane());
            }
        });

        viewMenu.getItems().addAll(showTreeItem, showPropertiesItem);

        helpMenu = new Menu();
        aboutItem = new MenuItem();
        aboutItem.setOnAction(e -> {
            Alert about = new Alert(Alert.AlertType.INFORMATION);
            about.setTitle("About");
            about.setHeaderText("BDoc Editor v0.1");
            about.setContentText("JavaFX desktop editor prototype.");
            about.showAndWait();
        });
        helpMenu.getItems().add(aboutItem);

        return new MenuBar(fileMenu, editMenu, viewMenu, helpMenu);
    }

    private ToolBar buildFileToolBar(Stage stage) {
        fileToolbarTitleLabel = new Label();

        newSampleBtn = new Button();
        newSampleBtn.getStyleClass().add("bdoc-toolbar-button");
        newSampleBtn.setOnAction(e -> onNewSample(stage));

        openBtn = new Button();
        openBtn.getStyleClass().add("bdoc-toolbar-button");
        openBtn.setOnAction(e -> onOpen(stage));

        saveAsBtn = new Button();
        saveAsBtn.getStyleClass().add("bdoc-toolbar-button");
        saveAsBtn.setOnAction(e -> onSaveAs(stage));

        localeBtn = new Button();
        localeBtn.getStyleClass().add("bdoc-toolbar-button");
        localeBtn.setOnAction(e -> toggleLocale());

        themeBtn = new Button();
        themeBtn.getStyleClass().add("bdoc-toolbar-button");
        themeBtn.setOnAction(e -> toggleTheme());

        ToolBar toolBar = new ToolBar(
                fileToolbarTitleLabel,
                new Separator(),
                newSampleBtn,
                openBtn,
                saveAsBtn,
                new Separator(),
                localeBtn,
                themeBtn
        );

        ServiceLoader<ToolbarActionExtension> loader = ServiceLoader.load(ToolbarActionExtension.class);
        for (ToolbarActionExtension ext : loader) {
            Button pluginBtn = new Button();
            pluginBtn.getStyleClass().add("bdoc-toolbar-button");
            pluginBtn.setOnAction(e -> ext.execute(this));
            pluginBtn.setDisable(!ext.isEnabled(this));
            pluginButtons.put(ext, pluginBtn);
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
                statusLabel.setText("Active Tool: " + tool.getLabel());
                renderCurrentPage();
            });

            palette.getItems().add(btn);
        }

        return palette;
    }

    private String glyphFor(String toolId) {
        return switch (toolId) {
            case "SELECTION" -> "⬖";
            case "TEXT" -> "T";
            default -> "?";
        };
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

        clearRecentFilesItem = new MenuItem();
        clearRecentFilesItem.setOnAction(e -> {
            BdocSettings.getInstance().clearRecentFiles();
            rebuildRecentFilesMenu();
            refreshTexts();
        });
        recentFilesMenu.getItems().add(clearRecentFilesItem);
    }

    private void dispatchToActiveTool(Consumer<DtpToolStrategy> action) {
        if (document == null) {
            return;
        }

        DtpToolStrategy strategy = PluginContext.getInstance().getTool(currentToolId);
        if (strategy != null) {
            try {
                action.accept(strategy);
            } catch (Exception ex) {
                showError("Tool Error", ex.getMessage());
            }
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
        refreshTreeInternal();
    }

    @Override
    public void setStatusText(String text) {
        statusLabel.setText(text);
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

    @Override
    public BdocObject restoreToMaster(BdocObject overrideObject) {
        return restoreToMaster(getCurrentPage(), getCurrentMasterPage(), overrideObject);
    }

    private void rebuildPropertiesPanel(BdocObject object) {
        propertiesContainer.getChildren().clear();
        if (object == null) {
            return;
        }

        PropertiesPanelFactory factory = PluginContext.getInstance().findPropertiesFactory(object);
        if (factory != null) {
            factory.buildPanel(propertiesContainer, object, this);
        }
    }

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
                    CheckBox eyeBox = new CheckBox(data.layer.getName() + " [" + data.layer.getRole() + "]");
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

                    String label = data.isMasterLocked
                            ? data.object.getId() + " [" + data.object.getType() + "]"
                            : data.object.getId() + " [" + data.object.getType() + "]";

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
        if (data == null || document == null) {
            return;
        }

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
                        "Active Page %d — Format %.1f × %.1f %s (%.0f × %.0f pt)",
                        currentPageIndex,
                        displayW,
                        displayH,
                        page.getUnit(),
                        page.getWidth(),
                        page.getHeight()
                ));
            } catch (IOException ex) {
                statusLabel.setText("Page selected");
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
        if (object == null || documentTree.getRoot() == null) {
            return;
        }
        findAndSelectRecursive(documentTree.getRoot(), object);
    }

    private boolean findAndSelectRecursive(TreeItem<TreeNodeData> item, BdocObject target) {
        if (item.getValue() != null &&
                item.getValue().kind == NodeKind.OBJECT &&
                item.getValue().object == target) {
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
        if (document == null) {
            documentTree.setRoot(null);
            return;
        }

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
                        if (!object.getLayerRef().equals(layer.getId())) {
                            continue;
                        }

                        boolean isMasterLocked = pageRenderer.isRawMasterObject(object, masterPage);
                        layerItem.getChildren().add(
                                new TreeItem<>(TreeNodeData.object(pageIndex, layer, object, isMasterLocked))
                        );
                    }

                    pageItem.getChildren().add(layerItem);
                }
            } catch (IOException ex) {
                showError("Tree build error", "Failed to load page " + pageIndex + ": " + ex.getMessage());
            }

            root.getChildren().add(pageItem);
        }

        documentTree.setRoot(root);
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
            fc.setInitialFileName("restored-" + currentFile.getName());
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
            statusLabel.setText("Saved to " + target.getAbsolutePath() + " ✓");
            currentFile = target;
            BdocSettings.getInstance().pushRecentFile(target.getAbsolutePath());
            rebuildRecentFilesMenu();
            refreshTexts();
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
            statusLabel.setText("Opened " + file.getName() + " (" + document.getPageCount() + " pages)");
            BdocSettings.getInstance().pushRecentFile(file.getAbsolutePath());
            rebuildRecentFilesMenu();
            refreshTexts();
        } catch (Exception ex) {
            showError("Open error", ex.getMessage());
        }
    }

    private void saveDocument(DocumentHandle handle, File targetFile) throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        CBORMapper cborMapper = new CBORMapper();

        Map<String, String> env = new HashMap<>();
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

            List<Map<String, Object>> pagesList = new ArrayList<>();
            for (Integer index : handle.getPageIndices()) {
                PageModel page = handle.loadPage(index);
                java.nio.file.Path pagePath = zipfs.getPath("/pages/page-" + index + ".cbor");
                try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(pagePath)) {
                    cborMapper.writeValue(os, page);
                }

                Map<String, Object> pEntry = new HashMap<>();
                pEntry.put("index", index);
                pEntry.put("id", "page-" + index);
                pEntry.put("file", "pages/page-" + index + ".cbor");
                pagesList.add(pEntry);
            }

            List<Map<String, Object>> templatesList = new ArrayList<>();
            for (MasterPage masterPage : handle.getTemplates().getMasterPages()) {
                java.nio.file.Path templatePath = zipfs.getPath("/templates/" + masterPage.getId() + ".json");
                try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(templatePath)) {
                    jsonMapper.writerWithDefaultPrettyPrinter().writeValue(os, masterPage);
                }

                Map<String, Object> tEntry = new HashMap<>();
                tEntry.put("id", masterPage.getId());
                tEntry.put("file", "templates/" + masterPage.getId() + ".json");
                templatesList.add(tEntry);
            }

            List<Map<String, Object>> storiesList = new ArrayList<>();
            for (String storyId : handle.getStoryIds()) {
                Map<String, Object> sEntry = new HashMap<>();
                sEntry.put("id", storyId);
                sEntry.put("file", "stories/" + storyId + ".json");
                storiesList.add(sEntry);
            }

            Map<String, Object> manifestMap = new HashMap<>();
            manifestMap.put("id", handle.getId());
            manifestMap.put("title", handle.getTitle());
            manifestMap.put("documentType", handle.getDocumentType());
            manifestMap.put("version", "0.1-composite");
            manifestMap.put("pages", pagesList);
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

        page.getObjects().removeIf(o -> o.getId().equals(overrideObject.getId()) && o.isMasterOverride());
        return masterPage.findObject(overrideObject.getMasterSourceId());
    }

    private void showValidationErrors(String title, BdocValidationException validationEx) {
        StringBuilder sb = new StringBuilder();
        for (String error : validationEx.getErrors()) {
            sb.append("• ").append(error).append('\n');
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Preflight validation failed (" + validationEx.getErrors().size() + " issues)");

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(500);
        textArea.setPrefHeight(300);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}