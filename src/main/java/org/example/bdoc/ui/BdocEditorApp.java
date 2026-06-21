package org.example.bdoc.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.bdoc.model.DocumentModel;
import org.example.bdoc.model.PageModel;
import org.example.bdoc.model.SampleDocuments;
import org.example.bdoc.render.PageRenderer;

public class BdocEditorApp extends Application {

    @Override
    public void start(Stage stage) {
        DocumentModel document = SampleDocuments.sample();
        PageModel firstPage = document.getPages().get(0);

        ListView<String> documentTree = new ListView<>();
        documentTree.getItems().add("Document: " + document.getTitle());
        document.getPages().forEach(page -> documentTree.getItems().add("Page " + page.getIndex()));
        document.getStories().forEach(story -> documentTree.getItems().add("Story: " + story.getId()));
        documentTree.setPrefWidth(220);

        Canvas canvas = new Canvas(firstPage.getWidth(), firstPage.getHeight());
        new PageRenderer().render(canvas.getGraphicsContext2D(), document, firstPage);

        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setPadding(new Insets(24));
        canvasPane.setStyle("-fx-background-color: #CBD5E1;");

        BorderPane propertiesPane = new BorderPane();
        propertiesPane.setTop(new Label("Properties"));
        propertiesPane.setPadding(new Insets(12));
        propertiesPane.setPrefWidth(220);

        SplitPane splitPane = new SplitPane(documentTree, canvasPane, propertiesPane);
        splitPane.setDividerPositions(0.18, 0.82);

        ToolBar toolBar = new ToolBar(new Label("BDoc Editor v0.1 MVP"));

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(splitPane);
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-background-color: white;");

        Scene scene = new Scene(root, 1280, 900, Color.WHITE);
        stage.setTitle("BDoc Editor");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}