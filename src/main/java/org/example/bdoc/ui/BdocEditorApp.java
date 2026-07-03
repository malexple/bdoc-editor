package org.example.bdoc.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.bdoc.io.BdocContainerSerializer;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.ManifestPageEntry;
import org.example.bdoc.model.PageModel;
import org.example.bdoc.model.SampleDocuments;
import org.example.bdoc.model.StoryModel;
import org.example.bdoc.render.PageRenderer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class BdocEditorApp extends Application {

    private final BdocContainerSerializer serializer = new BdocContainerSerializer();
    private final PageRenderer pageRenderer = new PageRenderer();

    private DocumentHandle document;
    private File currentFile;
    private int currentPageIndex = 1;

    private Canvas canvas;
    private ListView<String> documentTree;
    private Label statusLabel;

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
        propertiesPane.setTop(new Label("Properties"));
        propertiesPane.setPadding(new Insets(12));
        propertiesPane.setPrefWidth(220);

        SplitPane splitPane = new SplitPane(documentTree, canvasPane, propertiesPane);
        splitPane.setDividerPositions(0.18, 0.82);

        statusLabel = new Label("No document loaded");

        Button openBtn = new Button("Open");
        openBtn.setOnAction(e -> onOpen(stage));

        Button saveAsBtn = new Button("Save As");
        saveAsBtn.setOnAction(e -> onSaveAs(stage));

        Button newSampleBtn = new Button("New Sample");
        newSampleBtn.setOnAction(e -> onNewSample(stage));

        ToolBar toolBar = new ToolBar(
                new Label("BDoc Editor v0.1-composite"),
                newSampleBtn,
                openBtn,
                saveAsBtn
        );

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(splitPane);
        root.setBottom(statusLabel);
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-background-color: white;");

        Scene scene = new Scene(root, 1280, 900, Color.WHITE);
        stage.setTitle("BDoc Editor");
        stage.setScene(scene);
        stage.show();

        loadInitialSample(stage);
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
        if (currentFile == null) {
            showError("Save error", "No document is currently open.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save BDoc file as");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("BDoc files", "*.bdoc"));
        fc.setInitialFileName(currentFile.getName());
        File target = fc.showSaveDialog(stage);
        if (target == null) {
            return;
        }
        try {
            // Редактор пока не изменяет содержимое документа "на месте",
            // поэтому Save As копирует ZIP-контейнер целиком.
            // Как только появятся операции редактирования, это заменится
            // на пересборку через BdocContainerSerializer.Writer.
            Files.copy(currentFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            statusLabel.setText("Saved to " + target.getAbsolutePath());
        } catch (IOException ex) {
            showError("Save error", ex.getMessage());
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
            pageRenderer.render(canvas.getGraphicsContext2D(), document, page);
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
}