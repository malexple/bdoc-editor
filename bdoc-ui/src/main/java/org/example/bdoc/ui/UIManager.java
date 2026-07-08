package org.example.bdoc.ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.bdoc.i18n.LocalizationManager;
import org.example.bdoc.io.BdocValidationException;
import org.example.bdoc.plugin.BdocSettings;
import org.example.bdoc.spi.ToolbarActionExtension;
import org.example.bdoc.ui.task.TaskQueue;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

public class UIManager {
    private final BdocEditorApp app;
    private final Stage primaryStage;
    private final TaskQueue taskQueue;
    private final ThemeManager themeManager;

    private MenuBar menuBar;
    private ToolBar fileToolBar;
    private Label statusLabel;
    private SplitPane mainSplitPane;
    private SplitPane rightSidebarSplitPane;
    private BorderPane propertiesPanel;
    private BorderPane layersPanel;
    private VBox propertiesContainer;
    private Label propertiesTitleLabel;
    private Label layersTitleLabel;
    private Label fileToolbarTitleLabel;

    private Menu recentFilesMenu;
    private Menu fileMenu, editMenu, viewMenu, helpMenu;
    private MenuItem newSampleItem, openItem, saveAsItem, exitItem, undoItem, redoItem, aboutItem, clearRecentFilesItem;
    private CheckMenuItem showTreeItem, showPropertiesItem;
    private Button newSampleBtn, openBtn, saveAsBtn, localeBtn, themeBtn;
    private final Map<ToolbarActionExtension, Button> pluginButtons = new LinkedHashMap<>();

    public UIManager(BdocEditorApp app, Stage primaryStage, TaskQueue taskQueue, ThemeManager themeManager) {
        this.app = app;
        this.primaryStage = primaryStage;
        this.taskQueue = taskQueue;
        this.themeManager = themeManager;
    }

    public void buildUI(TreeManager treeManager, PropertiesPanelManager propMgr, ToolManager toolMgr, CanvasController canvasCtrl) {
        createStatusBar();
        createPropertiesPanel(propMgr);
        createLayersPanel(treeManager);
        createRightSidebar();
        createMainSplitPane(canvasCtrl, toolMgr);
        createFileToolBar();
        createMenuBar();
        applySavedLayout();
        applyCurrentTheme();
        refreshTexts();
    }

    private void createStatusBar() {
        statusLabel = new Label();
        statusLabel.getStyleClass().add("bdoc-status-bar");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setText(LocalizationManager.getInstance().get("status.noDocument"));
    }

    private void createPropertiesPanel(PropertiesPanelManager propMgr) {
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
        propMgr.setPropertiesContainer(propertiesContainer, propertiesTitleLabel);
    }

    private void createLayersPanel(TreeManager treeMgr) {
        layersTitleLabel = new Label();
        layersTitleLabel.getStyleClass().add("bdoc-section-title");
        VBox layersContainer = new VBox(10, treeMgr.getTreeView());
        VBox.setVgrow(treeMgr.getTreeView(), Priority.ALWAYS);
        layersContainer.setPadding(new Insets(10, 0, 0, 0));
        layersPanel = new BorderPane();
        layersPanel.setTop(layersTitleLabel);
        layersPanel.setCenter(layersContainer);
        layersPanel.getStyleClass().add("bdoc-layers-pane");
        layersPanel.setPadding(new Insets(12));
        layersPanel.setMinWidth(260);
    }

    private void createRightSidebar() {
        rightSidebarSplitPane = new SplitPane(propertiesPanel, layersPanel);
        rightSidebarSplitPane.setOrientation(Orientation.VERTICAL);
    }

    private void createMainSplitPane(CanvasController canvasCtrl, ToolManager toolMgr) {
        StackPane pageHolder = new StackPane(canvasCtrl.getCanvas());
        pageHolder.getStyleClass().add("page-holder");
        ToolBar palette = toolMgr.buildToolPalette();
        StackPane.setAlignment(palette, Pos.CENTER_LEFT);
        StackPane.setMargin(palette, new Insets(0, 0, 0, 16));
        StackPane canvasArea = new StackPane(pageHolder, palette);
        canvasArea.setPadding(new Insets(24));
        canvasArea.getStyleClass().add("workspace-area");
        mainSplitPane = new SplitPane(canvasArea, rightSidebarSplitPane);
        SplitPane.setResizableWithParent(rightSidebarSplitPane, false);
    }

    private void createFileToolBar() {
        fileToolbarTitleLabel = new Label();
        newSampleBtn = new Button(); newSampleBtn.getStyleClass().add("bdoc-toolbar-button");
        newSampleBtn.setOnAction(e -> app.onNewSample());
        openBtn = new Button(); openBtn.getStyleClass().add("bdoc-toolbar-button");
        openBtn.setOnAction(e -> app.onOpen());
        saveAsBtn = new Button(); saveAsBtn.getStyleClass().add("bdoc-toolbar-button");
        saveAsBtn.setOnAction(e -> app.onSaveAs());
        localeBtn = new Button(); localeBtn.getStyleClass().add("bdoc-toolbar-button");
        localeBtn.setOnAction(e -> toggleLocale());
        themeBtn = new Button(); themeBtn.getStyleClass().add("bdoc-toolbar-button");
        themeBtn.setOnAction(e -> app.toggleTheme());

        fileToolBar = new ToolBar(fileToolbarTitleLabel, new Separator(),
                newSampleBtn, openBtn, saveAsBtn, new Separator(), localeBtn, themeBtn);
        ServiceLoader<ToolbarActionExtension> loader = ServiceLoader.load(ToolbarActionExtension.class);
        for (ToolbarActionExtension ext : loader) {
            Button btn = new Button();
            btn.getStyleClass().add("bdoc-toolbar-button");
            btn.setOnAction(e -> ext.execute(app));
            btn.setDisable(!ext.isEnabled(app));
            pluginButtons.put(ext, btn);
            fileToolBar.getItems().add(btn);
        }
        fileToolBar.getStyleClass().add("bdoc-file-toolbar");
    }

    private void createMenuBar() {
        fileMenu = new Menu();
        newSampleItem = new MenuItem(); newSampleItem.setOnAction(e -> app.onNewSample());
        openItem = new MenuItem(); openItem.setOnAction(e -> app.onOpen());
        recentFilesMenu = new Menu(); rebuildRecentFilesMenu();
        saveAsItem = new MenuItem(); saveAsItem.setOnAction(e -> app.onSaveAs());
        exitItem = new MenuItem(); exitItem.setOnAction(e -> primaryStage.close());
        fileMenu.getItems().addAll(newSampleItem, openItem, recentFilesMenu, saveAsItem, new SeparatorMenuItem(), exitItem);

        editMenu = new Menu();
        undoItem = new MenuItem(); undoItem.setDisable(true);
        redoItem = new MenuItem(); redoItem.setDisable(true);
        editMenu.getItems().addAll(undoItem, redoItem);

        viewMenu = new Menu();
        showTreeItem = new CheckMenuItem(); showTreeItem.setSelected(true);
        showTreeItem.selectedProperty().addListener((obs, old, sel) -> {
            layersPanel.setVisible(sel); layersPanel.setManaged(sel);
            if (sel) { rightSidebarSplitPane.getItems().setAll(propertiesPanel, layersPanel);
                rightSidebarSplitPane.setDividerPositions(0.60); }
            else rightSidebarSplitPane.getItems().setAll(propertiesPanel);
        });
        showPropertiesItem = new CheckMenuItem(); showPropertiesItem.setSelected(true);
        showPropertiesItem.selectedProperty().addListener((obs, old, sel) -> {
            propertiesPanel.setVisible(sel); propertiesPanel.setManaged(sel);
            if (sel && showTreeItem.isSelected()) {
                rightSidebarSplitPane.getItems().setAll(propertiesPanel, layersPanel);
                rightSidebarSplitPane.setDividerPositions(0.60);
            } else if (sel) rightSidebarSplitPane.getItems().setAll(propertiesPanel);
            else if (showTreeItem.isSelected()) rightSidebarSplitPane.getItems().setAll(layersPanel);
            else rightSidebarSplitPane.getItems().setAll(new Pane());
        });
        viewMenu.getItems().addAll(showTreeItem, showPropertiesItem);

        helpMenu = new Menu();
        aboutItem = new MenuItem();
        aboutItem.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("About"); a.setHeaderText("BDoc Editor v0.1");
            a.setContentText("JavaFX desktop editor prototype.");
            a.showAndWait();
        });
        helpMenu.getItems().add(aboutItem);

        menuBar = new MenuBar(fileMenu, editMenu, viewMenu, helpMenu);
    }

    public MenuBar getMenuBar() { return menuBar; }
    public ToolBar getFileToolBar() { return fileToolBar; }
    public Label getStatusLabel() { return statusLabel; }
    public SplitPane getMainSplitPane() { return mainSplitPane; }

    public void refreshTexts() {
        LocalizationManager i18n = LocalizationManager.getInstance();
        primaryStage.setTitle(i18n.get("app.title"));
        propertiesTitleLabel.setText(i18n.get("ui.properties.title"));
        layersTitleLabel.setText(i18n.get("ui.layers.title"));
        if (app.getDocument() == null) statusLabel.setText(i18n.get("status.noDocument"));

        fileMenu.setText(i18n.get("menu.file"));
        editMenu.setText(i18n.get("menu.edit"));
        viewMenu.setText(i18n.get("menu.view"));
        helpMenu.setText(i18n.get("menu.help"));
        newSampleItem.setText(i18n.get("menu.file.newSample"));
        openItem.setText(i18n.get("menu.file.open"));
        saveAsItem.setText(i18n.get("menu.file.saveAs"));
        exitItem.setText(i18n.get("menu.file.exit"));
        undoItem.setText(i18n.get("menu.edit.undo"));
        redoItem.setText(i18n.get("menu.edit.redo"));
        showTreeItem.setText(i18n.get("menu.view.showTree"));
        showPropertiesItem.setText(i18n.get("menu.view.showProperties"));
        aboutItem.setText(i18n.get("menu.help.about"));
        recentFilesMenu.setText(i18n.get("menu.file.recent"));
        if (clearRecentFilesItem != null) clearRecentFilesItem.setText(i18n.get("menu.file.recent.clear"));

        fileToolbarTitleLabel.setText(i18n.get("app.title.short"));
        newSampleBtn.setText(i18n.get("toolbar.newSample"));
        openBtn.setText(i18n.get("toolbar.open"));
        saveAsBtn.setText(i18n.get("toolbar.saveAs"));
        localeBtn.setText(i18n.get("toolbar.localeSwitch"));
        themeBtn.setText(i18n.get("toolbar.themeSwitch"));

        for (Map.Entry<ToolbarActionExtension, Button> e : pluginButtons.entrySet()) {
            e.getValue().textProperty().bind(
                    LocalizationManager.getInstance().createStringBinding(e.getKey().getBundleOwnerId(), e.getKey().getLabelKey())
            );
            e.getValue().setDisable(!e.getKey().isEnabled(app));
        }
    }

    public void rebuildRecentFilesMenu() {
        if (recentFilesMenu == null) return;
        recentFilesMenu.getItems().clear();
        var recent = BdocSettings.getInstance().loadRecentFiles();
        if (recent.isEmpty()) {
            MenuItem empty = new MenuItem("(empty)"); empty.setDisable(true);
            recentFilesMenu.getItems().add(empty);
            return;
        }
        for (String path : recent) {
            MenuItem item = new MenuItem(path);
            item.setOnAction(e -> app.openDocument(new File(path)));
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

    public void updateStatus(String text) { statusLabel.setText(text); }

    public void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(title); a.setContentText(msg);
        a.showAndWait();
    }

    public void showValidationErrors(String title, BdocValidationException vex) {
        StringBuilder sb = new StringBuilder();
        for (String e : vex.getErrors()) sb.append("• ").append(e).append('\n');
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText("Preflight validation failed (" + vex.getErrors().size() + " issues)");
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false); ta.setWrapText(true); ta.setPrefWidth(500); ta.setPrefHeight(300);
        a.getDialogPane().setContent(ta);
        a.showAndWait();
    }

    public void toggleLocale() {
        LocalizationManager i18n = LocalizationManager.getInstance();
        Locale current = i18n.getCurrentLocale();
        Locale next = "ru".equalsIgnoreCase(current.getLanguage()) ? Locale.ENGLISH : new Locale("ru");
        i18n.setLocale(next);
        refreshTexts();
    }

    public void applyCurrentTheme() {
        if (primaryStage.getScene() != null)
            themeManager.apply(primaryStage.getScene(), BdocSettings.getInstance().loadTheme());
    }

    private void applySavedLayout() {
        double[] mainDefaults = new double[]{0.78};
        double[] savedMain = BdocSettings.getInstance().loadDividerPositions(mainDefaults);
        mainSplitPane.setDividerPositions(savedMain.length > 0 ? savedMain[0] : 0.78);
        double[] sideDefaults = new double[]{0.60};
        double[] savedSide = loadRightSidebarDividerPositions(sideDefaults);
        rightSidebarSplitPane.setDividerPositions(savedSide.length > 0 ? savedSide[0] : 0.60);
    }

    private double[] loadRightSidebarDividerPositions(double[] defaults) {
        double[] res = new double[defaults.length];
        for (int i = 0; i < defaults.length; i++)
            res[i] = BdocSettings.getInstance().node("layout").getDouble("rightSidebarDivider" + i, defaults[i]);
        return res;
    }

    public void saveLayout() {
        double[] main = mainSplitPane.getDividerPositions();
        if (main.length > 0) BdocSettings.getInstance().saveDividerPositions(main[0]);
        double[] right = rightSidebarSplitPane.getDividerPositions();
        if (right.length > 0) {
            BdocSettings.getInstance().node("layout").putDouble("rightSidebarDivider0", right[0]);
        }
    }
}