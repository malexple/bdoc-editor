package org.example.bdoc.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.bdoc.io.BdocXmlSerializer;
import org.example.bdoc.model.DocumentModel;
import org.example.bdoc.model.PageModel;
import org.example.bdoc.model.SampleDocuments;
import org.example.bdoc.render.PageRenderer;

import java.io.File;

public class BdocEditorApp extends Application {

    private DocumentModel document;
    private Canvas canvas;
    private ListView<String> documentTree;
    private final BdocXmlSerializer serializer = new BdocXmlSerializer();

    @Override
    public void start(Stage stage) {
        document = SampleDocuments.sample();

        documentTree = new ListView<>();
        documentTree.setPrefWidth(220);
        refreshTree();

        PageModel firstPage = document.getPages().get(0);
        canvas = new Canvas(firstPage.getWidth(), firstPage.getHeight());
        renderPage(firstPage);

        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setPadding(new Insets(24));
        canvasPane.setStyle("-fx-background-color: #CBD5E1;");

        BorderPane propertiesPane = new BorderPane();
        propertiesPane.setTop(new Label("Properties"));
        propertiesPane.setPadding(new Insets(12));
        propertiesPane.setPrefWidth(220);

        SplitPane splitPane = new SplitPane(documentTree, canvasPane, propertiesPane);
        splitPane.setDividerPositions(0.18, 0.82);

        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Save BDoc file");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("BDoc files", "*.bdoc"));
            File file = fc.showSaveDialog(stage);
            if (file != null) {
                serializer.save(document, file);
            }
        });

        Button openBtn = new Button("Open");
        openBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Open BDoc file");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("BDoc files", "*.bdoc"));
            File file = fc.showOpenDialog(stage);
            if (file != null) {
                document = serializer.load(file);
                refreshTree();
                PageModel page = document.getPages().get(0);
                canvas.setWidth(page.getWidth());
                canvas.setHeight(page.getHeight());
                renderPage(page);
            }
        });

        ToolBar toolBar = new ToolBar(
                new Label("BDoc Editor v0.1"),
                saveBtn,
                openBtn
        );

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(splitPane);
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-background-color: white;");

        Scene scene = new Scene(root, 1280, 900, Color.WHITE);
        stage.setTitle("BDoc Editor");
        stage.setScene(scene);
        stage.show();
    }

    private void renderPage(PageModel page) {
        new PageRenderer().render(canvas.getGraphicsContext2D(), document, page);
    }

    private void refreshTree() {
        documentTree.getItems().clear();
        documentTree.getItems().add("Document: " + document.getTitle());
        document.getPages().forEach(page -> documentTree.getItems().add("Page " + page.getIndex()));
        document.getStories().forEach(story -> documentTree.getItems().add("Story: " + story.getId()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}