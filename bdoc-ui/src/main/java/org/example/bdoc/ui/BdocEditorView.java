package org.example.bdoc.ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class BdocEditorView {

    private static final double DEFAULT_RIGHT_SIDEBAR_WIDTH = 300;
    private static final double RIGHT_SIDEBAR_MIN_WIDTH = 260;

    private final BorderPane root;
    private final VBox topContainer;

    private final Canvas canvas;
    private final StackPane canvasPane;

    private final TreeView<EditorTreeNodeData> documentTree;

    private final BorderPane propertiesPane;
    private final VBox propertiesContainer;
    private final ScrollPane propertiesScrollPane;

    private final BorderPane treePane;

    private final SplitPane rightSidebarSplitPane;
    private final SplitPane mainSplitPane;

    private final HBox statusBar;
    private final Label zoomStatusLabel;
    private final Label toolStatusLabel;
    private final Label contextStatusLabel;
    private final Label messageStatusLabel;

    public BdocEditorView() {
        this.root = new BorderPane();
        this.topContainer = new VBox();

        this.canvas = new Canvas(595, 842);
        this.canvasPane = new StackPane(canvas);
        this.canvasPane.setPadding(new Insets(24));
        this.canvasPane.getStyleClass().add("bdoc-canvas-pane");

        this.documentTree = new TreeView<>();

        this.propertiesPane = new BorderPane();
        this.propertiesPane.setPadding(new Insets(12));
        this.propertiesPane.setMinWidth(0);
        this.propertiesPane.getStyleClass().add("bdoc-properties-pane");

        Label propTitle = new Label("Properties");
        propTitle.getStyleClass().add("bdoc-section-title");
        this.propertiesPane.setTop(propTitle);

        this.propertiesContainer = new VBox(10);
        this.propertiesContainer.setPadding(new Insets(10, 0, 0, 0));

        this.propertiesScrollPane = new ScrollPane(propertiesContainer);
        this.propertiesScrollPane.setFitToWidth(true);
        this.propertiesScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.propertiesScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        this.propertiesScrollPane.getStyleClass().add("bdoc-properties-scroll");

        this.propertiesPane.setCenter(propertiesScrollPane);

        this.treePane = new BorderPane();
        this.treePane.getStyleClass().add("bdoc-tree-pane");

        Label treeTitle = new Label("Document Tree");
        treeTitle.getStyleClass().add("bdoc-section-title");
        BorderPane.setMargin(treeTitle, new Insets(12, 12, 8, 12));
        this.treePane.setTop(treeTitle);

        this.documentTree.getStyleClass().add("bdoc-document-tree");
        BorderPane.setMargin(documentTree, new Insets(0, 0, 0, 0));
        this.treePane.setCenter(documentTree);

        this.rightSidebarSplitPane = new SplitPane(propertiesPane, treePane);
        this.rightSidebarSplitPane.setOrientation(Orientation.VERTICAL);
        this.rightSidebarSplitPane.getStyleClass().add("bdoc-right-sidebar-split");
        this.rightSidebarSplitPane.setDividerPositions(0.62);

        BorderPane rightSidebarContainer = new BorderPane(rightSidebarSplitPane);
        rightSidebarContainer.setPrefWidth(DEFAULT_RIGHT_SIDEBAR_WIDTH);
        rightSidebarContainer.setMinWidth(RIGHT_SIDEBAR_MIN_WIDTH);
        rightSidebarContainer.setMaxWidth(Double.MAX_VALUE);
        rightSidebarContainer.getStyleClass().add("bdoc-right-sidebar-container");

        this.mainSplitPane = new SplitPane(canvasPane, rightSidebarContainer);
        this.mainSplitPane.setOrientation(Orientation.HORIZONTAL);
        this.mainSplitPane.getStyleClass().add("bdoc-main-split");

        SplitPane.setResizableWithParent(rightSidebarContainer, false);

        this.zoomStatusLabel = createStatusLabel("100%");
        this.toolStatusLabel = createStatusLabel("Tool: Selection");
        this.contextStatusLabel = createStatusLabel("Page: —");
        this.messageStatusLabel = createStatusLabel("No document loaded");

        RegionSpacer spacer = new RegionSpacer();

        this.statusBar = new HBox(
                zoomStatusLabel,
                createStatusSeparator(),
                toolStatusLabel,
                createStatusSeparator(),
                contextStatusLabel,
                createStatusSeparator(),
                spacer,
                messageStatusLabel
        );
        this.statusBar.getStyleClass().add("bdoc-status-bar");
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox.setHgrow(messageStatusLabel, Priority.ALWAYS);

        root.setTop(topContainer);
        root.setCenter(mainSplitPane);
        root.setBottom(statusBar);
        root.getStyleClass().add("root");
    }

    public void setTopBars(MenuBar menuBar, ToolBar fileToolBar) {
        topContainer.getChildren().setAll(menuBar, fileToolBar);
    }

    public void setToolPalette(ToolBar toolPalette) {
        if (!canvasPane.getChildren().contains(toolPalette)) {
            StackPane.setAlignment(toolPalette, Pos.CENTER_LEFT);
            StackPane.setMargin(toolPalette, new Insets(0, 0, 0, 15));
            canvasPane.getChildren().add(toolPalette);
        }
    }

    public BorderPane getRoot() {
        return root;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public StackPane getCanvasPane() {
        return canvasPane;
    }

    public TreeView<EditorTreeNodeData> getDocumentTree() {
        return documentTree;
    }

    public BorderPane getPropertiesPane() {
        return propertiesPane;
    }

    public VBox getPropertiesContainer() {
        return propertiesContainer;
    }

    public ScrollPane getPropertiesScrollPane() {
        return propertiesScrollPane;
    }

    public BorderPane getTreePane() {
        return treePane;
    }

    public SplitPane getRightSidebarSplitPane() {
        return rightSidebarSplitPane;
    }

    public SplitPane getMainSplitPane() {
        return mainSplitPane;
    }

    public HBox getStatusBar() {
        return statusBar;
    }

    public Label getZoomStatusLabel() {
        return zoomStatusLabel;
    }

    public Label getToolStatusLabel() {
        return toolStatusLabel;
    }

    public Label getContextStatusLabel() {
        return contextStatusLabel;
    }

    public Label getMessageStatusLabel() {
        return messageStatusLabel;
    }

    public void setStatusMessage(String text) {
        messageStatusLabel.setText(text);
    }

    public void setToolStatus(String text) {
        toolStatusLabel.setText(text);
    }

    public void setContextStatus(String text) {
        contextStatusLabel.setText(text);
    }

    public void setZoomStatus(String text) {
        zoomStatusLabel.setText(text);
    }

    private Label createStatusLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("bdoc-status-label");
        return label;
    }

    private Separator createStatusSeparator() {
        Separator separator = new Separator(Orientation.VERTICAL);
        separator.getStyleClass().add("bdoc-status-separator");
        return separator;
    }

    private static final class RegionSpacer extends javafx.scene.layout.Region {
        private RegionSpacer() {
            setMinWidth(0);
        }
    }
}