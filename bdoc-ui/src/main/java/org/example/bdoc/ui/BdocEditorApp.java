package org.example.bdoc.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.bdoc.io.BdocValidationException;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.i18n.LocalizationManager;
import org.example.bdoc.model.BdocObject;
import org.example.bdoc.model.MasterPage;
import org.example.bdoc.model.PageModel;
import org.example.bdoc.model.PathModel;
import org.example.bdoc.plugin.BdocSettings;
import org.example.bdoc.ui.task.TaskQueue;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class BdocEditorApp extends Application implements EditorContext, DocumentEventSink {

    private Stage primaryStage;
    private TaskQueue taskQueue;
    private final ThemeManager themeManager = new ThemeManager();

    private DocumentManager documentManager;
    private UIManager uiManager;
    private TreeManager treeManager;
    private PropertiesPanelManager propertiesManager;
    private ToolManager toolManager;
    private CanvasController canvasController;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.taskQueue = new TaskQueue(stage);

        PluginInitializer.init();

        LocalizationManager i18n = LocalizationManager.getInstance();
        i18n.setLocale(Locale.forLanguageTag(BdocSettings.getInstance().loadLocale()));

        documentManager = new DocumentManager(this);
        treeManager = new TreeManager(this, documentManager);
        propertiesManager = new PropertiesPanelManager(this);
        toolManager = new ToolManager(this);
        canvasController = new CanvasController(this, documentManager, toolManager);

        uiManager = new UIManager(this, primaryStage, taskQueue, themeManager);
        uiManager.buildUI(treeManager, propertiesManager, toolManager, canvasController);

        BorderPane root = new BorderPane();
        VBox top = new VBox(uiManager.getMenuBar(), uiManager.getFileToolBar());
        root.setTop(top);
        root.setCenter(uiManager.getMainSplitPane());
        root.setBottom(uiManager.getStatusLabel());
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 1360, 900);
        themeManager.apply(scene, BdocSettings.getInstance().loadTheme());

        stage.setScene(scene);
        double[] bounds = BdocSettings.getInstance().loadWindowBounds();
        stage.setX(bounds[0]); stage.setY(bounds[1]);
        stage.setWidth(bounds[2]); stage.setHeight(bounds[3]);
        if (BdocSettings.getInstance().isWindowMaximized()) stage.setMaximized(true);

        stage.setOnCloseRequest(e -> {
            BdocSettings.getInstance().saveWindowBounds(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight(), stage.isMaximized());
            uiManager.saveLayout();
            BdocSettings.getInstance().saveActiveTool(toolManager.getCurrentToolId());
        });

        uiManager.refreshTexts();
        stage.show();
        documentManager.loadInitialSample();
    }

    // ---------- DocumentEventSink ----------
    @Override public void onDocumentOpened() {
        treeManager.refreshTree();
        canvasController.renderCurrentPage();
        uiManager.updateStatus("Opened " + documentManager.getCurrentFile().getName() + " (" + documentManager.getDocument().getPageCount() + " pages)");
        uiManager.rebuildRecentFilesMenu();
        uiManager.refreshTexts();
    }
    @Override public void onPageChanged(int pageIndex) { canvasController.renderCurrentPage(); }
    @Override public void onObjectSelected(BdocObject obj) {
        propertiesManager.rebuildPropertiesPanel(obj);
        treeManager.selectTreeNodeFor(obj);
    }
    @Override public void onDataChanged() { treeManager.refreshTree(); canvasController.renderCurrentPage(); }
    @Override public void onRecentFilesChanged() { uiManager.rebuildRecentFilesMenu(); uiManager.refreshTexts(); }
    @Override public void showError(String title, String msg) { uiManager.showError(title, msg); }
    @Override public void showValidationErrors(String title, BdocValidationException vex) { uiManager.showValidationErrors(title, vex); }

    // ---------- Actions ----------
    public void onNewSample() { documentManager.loadInitialSample(); }
    public void onOpen() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open BDoc file");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("BDoc files", "*.bdoc"));
        File f = fc.showOpenDialog(primaryStage);
        if (f != null) documentManager.openDocument(f);
    }
    public void onSaveAs() {
        if (documentManager.getDocument() == null) { showError("Save error", "No document is currently open."); return; }
        if (!documentManager.validateBeforeSave()) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Save BDoc file as");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("BDoc files", "*.bdoc"));
        File cur = documentManager.getCurrentFile();
        fc.setInitialFileName(cur != null ? "restored-" + cur.getName() : "document.bdoc");
        File target = fc.showSaveDialog(primaryStage);
        if (target == null) return;
        try {
            uiManager.updateStatus("Re-packing BDoc archive...");
            documentManager.saveDocument(documentManager.getDocument(), target);
            uiManager.updateStatus("Saved to " + target.getAbsolutePath() + " ✓");
            documentManager.setCurrentFile(target);
            BdocSettings.getInstance().pushRecentFile(target.getAbsolutePath());
            uiManager.rebuildRecentFilesMenu();
            uiManager.refreshTexts();
        } catch (IOException ex) {
            showError("Save error", "Failed to compile document package: " + ex.getMessage());
        }
    }
    public void toggleTheme() {
        String current = BdocSettings.getInstance().loadTheme();
        String next = ThemeManager.OBSIDIAN_INK.equalsIgnoreCase(current) ? ThemeManager.PAPER_MATTE : ThemeManager.OBSIDIAN_INK;
        BdocSettings.getInstance().saveTheme(next);
        uiManager.applyCurrentTheme();
        uiManager.refreshTexts();
    }

    public void openDocument(File file) {
        documentManager.openDocument(file);
    }

    // ---------- EditorContext ----------
    @Override public DocumentHandle getDocument() { return documentManager.getDocument(); }
    @Override public PageModel getCurrentPage() { return documentManager.getCurrentPage(); }
    @Override public MasterPage getCurrentMasterPage() { return documentManager.getCurrentMasterPage(); }
    @Override public int getCurrentPageIndex() { return documentManager.getCurrentPageIndex(); }
    @Override public BdocObject getSelectedObject() { return documentManager.getSelectedObject(); }
    @Override public void setSelectedObject(BdocObject obj) { documentManager.setSelectedObject(obj); }
    @Override public BdocObject materializeOverrideIfNeeded(BdocObject obj) {
        return documentManager.materializeOverrideIfNeeded(getCurrentPage(), getCurrentMasterPage(), obj);
    }
    @Override public BdocObject replacePathData(BdocObject obj, PathModel newPath) {
        return documentManager.replacePathData(obj, newPath);
    }
    @Override public void renderCurrentPage() { canvasController.renderCurrentPage(); }
    @Override public void refreshTree() { treeManager.refreshTree(); }
    @Override public void setStatusText(String text) { uiManager.updateStatus(text); }
    @Override public double[] toLocalPoint(double sx, double sy, BdocObject obj) {
        return canvasController.toLocalPoint(sx, sy, obj);
    }
    @Override public void runWriteAction(Runnable mutation) { documentManager.runWriteAction(mutation); }
    @Override public TaskQueue getTaskQueue() { return taskQueue; }
    @Override public BdocObject restoreToMaster(BdocObject override) {
        return documentManager.restoreToMaster(getCurrentPage(), getCurrentMasterPage(), override);
    }

    public static void main(String[] args) { launch(args); }
}