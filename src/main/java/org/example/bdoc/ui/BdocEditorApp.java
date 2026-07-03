package org.example.bdoc.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.bdoc.io.BdocContainerSerializer;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.*;
import org.example.bdoc.render.PageRenderer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class BdocEditorApp extends Application {

    private final BdocContainerSerializer serializer = new BdocContainerSerializer();
    private final PageRenderer pageRenderer = new PageRenderer();

    private DocumentHandle document;
    private File currentFile;
    private int currentPageIndex = 1;

    private Canvas canvas;
    private ListView<String> documentTree;
    private Label statusLabel;

    // Состояние интерактивности и инструментов
    private DtpTool currentTool = DtpTool.SELECTION;
    private BdocObject selectedObject = null;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private double objectInitialX = 0;
    private double objectInitialY = 0;

    // Компоненты панели свойств справа
    private VBox propertiesContainer;
    private Slider opacitySlider;
    private CheckBox visibleCheckBox;
    private TextArea storyTextArea;


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

        // --- ПАНЕЛЬ СВОЙСТВ (Справа) ---
        BorderPane propertiesPane = new BorderPane();
        propertiesPane.setPadding(new Insets(12));
        propertiesPane.setPrefWidth(240);
        Label propTitle = new Label("Properties & Layers");
        propTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        propertiesPane.setTop(propTitle);

        propertiesContainer = new VBox(10);
        propertiesContainer.setPadding(new Insets(10, 0, 0, 0));
        propertiesPane.setCenter(propertiesContainer);

        SplitPane splitPane = new SplitPane(documentTree, canvasPane, propertiesPane);
        splitPane.setDividerPositions(0.18, 0.78);
        statusLabel = new Label("No document loaded");

        // --- КНОПКИ ИНСТРУМЕНТОВ (DTP Tools) ---
        ToggleGroup toolGroup = new ToggleGroup();
        ToggleButton selectToolBtn = new ToggleButton("🏹 Select");
        selectToolBtn.setToggleGroup(toolGroup);
        selectToolBtn.setSelected(true);
        selectToolBtn.setOnAction(e -> currentTool = DtpTool.SELECTION);

        ToggleButton textToolBtn = new ToggleButton("📝 Text");
        textToolBtn.setToggleGroup(toolGroup);
        textToolBtn.setOnAction(e -> currentTool = DtpTool.TEXT);

        Button openBtn = new Button("Open");
        openBtn.setOnAction(e -> onOpen(stage));
        Button saveAsBtn = new Button("Save As");
        saveAsBtn.setOnAction(e -> onSaveAs(stage));
        Button newSampleBtn = new Button("New Sample");
        newSampleBtn.setOnAction(e -> onNewSample(stage));

        ToolBar toolBar = new ToolBar(
                new Label("BDoc Editor v0.1"),
                new Separator(),
                selectToolBtn, textToolBtn, // Панель инструментов верстки
                new Separator(),
                newSampleBtn, openBtn, saveAsBtn
        );

        // --- ИНТЕРАКТИВНАЯ ЛОГИКА МЫШИ НА CANVAS ---
        canvas.setOnMousePressed(e -> {
            if (document == null) return;
            try {
                PageModel page = document.loadPage(currentPageIndex);

                // Перебираем объекты с конца в начало (верхние слои в приоритете)
                BdocObject found = null;
                List<BdocObject> objects = page.getObjects();
                for (int i = objects.size() - 1; i >= 0; i--) {
                    BdocObject obj = objects.get(i);
                    Geometry g = obj.getGeometry();
                    if (e.getX() >= g.getX() && e.getX() <= g.getX() + g.getWidth() &&
                            e.getY() >= g.getY() && e.getY() <= g.getY() + g.getHeight()) {
                        found = obj;
                        break;
                    }
                }

                selectedObject = found;

                // --- ЛОГИКА ИНСТРУМЕНТА: SELECTION (СТРЕЛКА) ---
                if (currentTool == DtpTool.SELECTION) {
                    if (selectedObject != null) {
                        dragStartX = e.getX();
                        dragStartY = e.getY();
                        objectInitialX = selectedObject.getGeometry().getX();
                        objectInitialY = selectedObject.getGeometry().getY();
                        statusLabel.setText("Selected: " + selectedObject.getId() + " (" + selectedObject.getType() + ")");
                        updatePropertiesPane(page, selectedObject);
                    } else {
                        propertiesContainer.getChildren().clear();
                    }
                    renderCurrentPage();
                }
                // --- ЛОГИКА ИНСТРУМЕНТА: TEXT (ТЕКСТ) ---
                else if (currentTool == DtpTool.TEXT) {
                    if (selectedObject instanceof TextFrame textFrame) {
                        StoryModel story = document.getStory(textFrame.getStoryRef());
                        statusLabel.setText("Editing Story: " + textFrame.getStoryRef());

                        // Перерисовываем экран, чтобы выделить текстовый фрейм
                        renderCurrentPage();

                        // Активируем панель текстового редактора справа
                        updateTextEditorPane(textFrame, story);
                    } else {
                        statusLabel.setText("Text tool: click inside a TextFrame to edit text");
                        propertiesContainer.getChildren().clear();
                        renderCurrentPage();
                    }
                }
            } catch (IOException ex) {
                showError("Mouse Error", ex.getMessage());
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (currentTool == DtpTool.SELECTION && selectedObject != null) {
                // Вариант А: Прямо меняем координаты в mutable-геометрии
                double deltaX = e.getX() - dragStartX;
                double deltaY = e.getY() - dragStartY;
                selectedObject.getGeometry().setX(objectInitialX + deltaX);
                selectedObject.getGeometry().setY(objectInitialY + deltaY);
                renderCurrentPage();
            }
        });

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(splitPane);
        root.setBottom(statusLabel);
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-background-color: white;");

        Scene scene = new Scene(root, 1280, 900, Color.WHITE);
        stage.setTitle("BDoc Editor - Реставрация печатных изданий");
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
            pageRenderer.render(canvas.getGraphicsContext2D(), document, page, selectedObject);
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

    private void updatePropertiesPane(PageModel page, BdocObject object) {
        propertiesContainer.getChildren().clear();

        // Находим слой, которому принадлежит объект
        LayerModel objectLayer = page.getLayers().stream()
                .filter(l -> l.getId().equals(object.getLayerRef()))
                .findFirst()
                .orElse(null);

        if (objectLayer == null) return;

        Label objectInfo = new Label("Object ID: " + object.getId() + "\nType: " + object.getType());
        objectInfo.setStyle("-fx-text-fill: #475569;");

        Label layerLabel = new Label("Layer: " + objectLayer.getName() + " (" + objectLayer.getRole() + ")");
        layerLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        // Чекбокс видимости слоя
        visibleCheckBox = new CheckBox("Layer Visible");
        visibleCheckBox.setSelected(objectLayer.isVisible());
        visibleCheckBox.setOnAction(e -> {
            objectLayer.setVisible(visibleCheckBox.isSelected());
            renderCurrentPage();
        });

        // Ползунок прозрачности слоя (критично для реставрации сканов)
        Label opacityLabel = new Label("Layer Opacity: " + Math.round(objectLayer.getOpacity() * 100) + "%");
        opacitySlider = new Slider(0.0, 1.0, objectLayer.getOpacity());
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            objectLayer.setOpacity(newVal.doubleValue());
            opacityLabel.setText("Layer Opacity: " + Math.round(newVal.doubleValue() * 100) + "%");
            renderCurrentPage(); // Мгновенно перерисовываем Canvas с новой прозрачностью скана
        });

        propertiesContainer.getChildren().addAll(
                objectInfo,
                new Separator(),
                layerLabel,
                visibleCheckBox,
                opacityLabel,
                opacitySlider
        );
    }

    private void updateTextEditorPane(TextFrame textFrame, StoryModel story) {
        propertiesContainer.getChildren().clear();

        Label titleLabel = new Label("Text Frame Content");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Label infoLabel = new Label("Frame ID: " + textFrame.getId() + "\nStory Ref: " + textFrame.getStoryRef());
        infoLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        storyTextArea = new TextArea();
        storyTextArea.setPrefHeight(300);
        storyTextArea.setWrapText(true);

        // Загружаем текущий текст истории в поле ввода
        if (story != null) {
            storyTextArea.setText(story.getJoinedText());
        }

        // Слушатель изменений: как только пользователь вводит символ или удаляет мусор/рекламу,
        // данные сохраняются в StoryModel в памяти, и холст мгновенно обновляется
        storyTextArea.textProperty().addListener((obs, oldText, newText) -> {
            if (story != null) {
                // Очищаем старые параграфы и записываем обновленный текст
                story.getParagraphs().clear();

                // Разбиваем введенный текст по строкам, чтобы восстановить структуру параграфов
                String[] lines = newText.split("\n");
                for (String line : lines) {
                    // Создаем чистый параграф (роль body, стиль по умолчанию body-text)
                    story.getParagraphs().add(new Paragraph("body", "body-text", line));
                }

                // Мгновенно пересчитываем Layout и перерисовываем Canvas через TextWrapper
                renderCurrentPage();
            }
        });

        Label hintLabel = new Label("Tip: You can paste cleaned OCR text here. Advertisement blocks can be wiped instantly.");
        hintLabel.setWrapText(true);
        hintLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-style: italic;");

        propertiesContainer.getChildren().addAll(
                titleLabel,
                infoLabel,
                new Separator(),
                storyTextArea,
                hintLabel
        );
    }


}