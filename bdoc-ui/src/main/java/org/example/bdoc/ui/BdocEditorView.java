package org.example.bdoc.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class BdocEditorView {

    private final BorderPane root;

    private final VBox topContainer;
    private final TreeView<EditorTreeNodeData> documentTree;
    private final Canvas canvas;
    private final StackPane canvasPane;
    private final BorderPane propertiesPane;
    private final VBox propertiesContainer;
    private final Label statusLabel;
    private final SplitPane mainSplitPane;

    public BdocEditorView() {
        this.root = new BorderPane();
        this.topContainer = new VBox();

        this.documentTree = new TreeView<>();
        this.documentTree.setPrefWidth(260);

        this.canvas = new Canvas(595, 842);
        this.canvasPane = new StackPane(canvas);
        this.canvasPane.setPadding(new Insets(24));
        this.canvasPane.getStyleClass().add("bdoc-canvas-pane");

        this.propertiesPane = new BorderPane();
        this.propertiesPane.setPadding(new Insets(12));
        this.propertiesPane.setPrefWidth(240);
        this.propertiesPane.getStyleClass().add("bdoc-properties-pane");

        Label propTitle = new Label("Properties & Layers");
        propTitle.getStyleClass().add("bdoc-section-title");
        this.propertiesPane.setTop(propTitle);

        this.propertiesContainer = new VBox(10);
        this.propertiesContainer.setPadding(new Insets(10, 0, 0, 0));
        this.propertiesPane.setCenter(propertiesContainer);

        this.mainSplitPane = new SplitPane(documentTree, canvasPane, propertiesPane);

        this.statusLabel = new Label("No document loaded");
        this.statusLabel.getStyleClass().add("bdoc-status-bar");
        this.statusLabel.setMaxWidth(Double.MAX_VALUE);

        root.setTop(topContainer);
        root.setCenter(mainSplitPane);
        root.setBottom(statusLabel);
        root.getStyleClass().add("root");
    }

    public void setTopBars(MenuBar menuBar, ToolBar fileToolBar) {
        topContainer.getChildren().setAll(menuBar, fileToolBar);
    }

    public void setToolPalette(ToolBar toolPalette) {
        if (!canvasPane.getChildren().contains(toolPalette)) {
            StackPane.setAlignment(toolPalette, Pos.CENTER_LEFT);
            StackPane.setMargin(toolPalette, new Insets(0, 0, 0, 16));
            canvasPane.getChildren().add(toolPalette);
        }
    }

    public BorderPane getRoot() {
        return root;
    }

    public TreeView<EditorTreeNodeData> getDocumentTree() {
        return documentTree;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public StackPane getCanvasPane() {
        return canvasPane;
    }

    public BorderPane getPropertiesPane() {
        return propertiesPane;
    }

    public VBox getPropertiesContainer() {
        return propertiesContainer;
    }

    public Label getStatusLabel() {
        return statusLabel;
    }

    public SplitPane getMainSplitPane() {
        return mainSplitPane;
    }
}