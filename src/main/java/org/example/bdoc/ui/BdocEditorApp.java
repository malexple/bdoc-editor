package org.example.bdoc.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
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

    // Состояние изменения размеров (Resize)
    private boolean isResizing = false;
    private int resizeHandleIndex = -1; // 0: верхний-левый, 1: верхний-правый, 2: нижний-левый, 3: нижний-правый
    private double initialWidth = 0;
    private double initialHeight = 0;
    private final double HANDLE_SIZE = 6.0; // Должно совпадать с размером квадратиков в PageRenderer



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

                // --- 1. ПРОВЕРКА НА КЛИК ПО УГЛОВЫМ МАРКЕРАМ (RESIZE) ---
                if (currentTool == DtpTool.SELECTION && selectedObject != null) {
                    Geometry g = selectedObject.getGeometry();
                    double x = e.getX();
                    double y = e.getY();

                    // Координаты углов с учётом размера маркера
                    double[][] handles = {
                            {g.getX(), g.getY()},                                 // 0: Топ-Лево
                            {g.getX() + g.getWidth(), g.getY()},                  // 1: Топ-Право
                            {g.getX(), g.getY() + g.getHeight()},                 // 2: Низ-Лево
                            {g.getX() + g.getWidth(), g.getY() + g.getHeight()}   // 3: Низ-Право
                    };

                    for (int i = 0; i < handles.length; i++) {
                        if (x >= handles[i][0] - HANDLE_SIZE && x <= handles[i][0] + HANDLE_SIZE &&
                                y >= handles[i][1] - HANDLE_SIZE && y <= handles[i][1] + HANDLE_SIZE) {
                            isResizing = true;
                            resizeHandleIndex = i;
                            dragStartX = x;
                            dragStartY = y;
                            objectInitialX = g.getX();
                            objectInitialY = g.getY();
                            initialWidth = g.getWidth();
                            initialHeight = g.getHeight();
                            statusLabel.setText("Resizing object from corner: " + i);
                            return; // Прерываем метод, обычное выделение делать не нужно
                        }
                    }
                }

                // --- 2. ОБЫЧНЫЙ ПОИСК ОБЪЕКТА ДЛЯ ВЫДЕЛЕНИЯ / ПЕРЕМЕЩЕНИЯ ---
                isResizing = false;
                resizeHandleIndex = -1;

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

                if (currentTool == DtpTool.SELECTION) {
                    if (selectedObject != null) {
                        dragStartX = e.getX();
                        dragStartY = e.getY();
                        objectInitialX = selectedObject.getGeometry().getX();
                        objectInitialY = selectedObject.getGeometry().getY();
                        statusLabel.setText("Selected: " + selectedObject.getId());
                        updatePropertiesPane(page, selectedObject);
                    } else {
                        propertiesContainer.getChildren().clear();
                    }
                    renderCurrentPage();
                }
                else if (currentTool == DtpTool.TEXT) {
                    if (selectedObject instanceof TextFrame textFrame) {
                        StoryModel story = document.getStory(textFrame.getStoryRef());
                        statusLabel.setText("Editing Story: " + textFrame.getStoryRef());
                        renderCurrentPage();
                        updateTextEditorPane(textFrame, story);
                    } else {
                        propertiesContainer.getChildren().clear();
                        renderCurrentPage();
                    }
                }
            } catch (IOException ex) {
                showError("Mouse Press Error", ex.getMessage());
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (selectedObject == null) return;

            double deltaX = e.getX() - dragStartX;
            double deltaY = e.getY() - dragStartY;
            Geometry g = selectedObject.getGeometry();

            // --- ЛОГИКА ИЗМЕНЕНИЯ РАЗМЕРОВ (RESIZE) ---
            if (isResizing) {
                switch (resizeHandleIndex) {
                    case 0 -> { // Топ-Лево: меняются и координаты, и размеры
                        double newWidth = initialWidth - deltaX;
                        double newHeight = initialHeight - deltaY;
                        if (newWidth > 10 && newHeight > 10) {
                            g.setX(objectInitialX + deltaX);
                            g.setY(objectInitialY + deltaY);
                            g.setWidth(newWidth);
                            g.setHeight(newHeight);
                        }
                    }
                    case 1 -> { // Топ-Право: меняется координата Y, ширина и высота
                        double newWidth = initialWidth + deltaX;
                        double newHeight = initialHeight - deltaY;
                        if (newWidth > 10 && newHeight > 10) {
                            g.setY(objectInitialY + deltaY);
                            g.setWidth(newWidth);
                            g.setHeight(newHeight);
                        }
                    }
                    case 2 -> { // Низ-Лево: меняется координата X, ширина и высота
                        double newWidth = initialWidth - deltaX;
                        double newHeight = initialHeight + deltaY;
                        if (newWidth > 10 && newHeight > 10) {
                            g.setX(objectInitialX + deltaX);
                            g.setWidth(newWidth);
                            g.setHeight(newHeight);
                        }
                    }
                    case 3 -> { // Низ-Право: меняются только ширина и высота (самый частый кейс)
                        double newWidth = initialWidth + deltaX;
                        double newHeight = initialHeight + deltaY;
                        if (newWidth > 10 && newHeight > 10) {
                            g.setWidth(newWidth);
                            g.setHeight(newHeight);
                        }
                    }
                }
                renderCurrentPage(); // Мгновенный пересчет переноса строк внутри фрейма при изменении ширины!
            }
            // --- ОБЫЧНОЕ ПЕРЕМЕЩЕНИЕ (DRAG) ---
            else if (currentTool == DtpTool.SELECTION) {
                g.setX(objectInitialX + deltaX);
                g.setY(objectInitialY + deltaY);
                renderCurrentPage();
            }
        });

        canvas.setOnMouseReleased(e -> {
            isResizing = false;
            resizeHandleIndex = -1;
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
        if (document == null) {
            showError("Save error", "No document is currently open.");
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
            statusLabel.setText("Re-packing BDoc archive with new layout and text...");

            // Вызываем метод полной компиляции измененных данных в ZIP+CBOR контейнер
            saveDocument(document, target);

            statusLabel.setText("Saved to " + target.getAbsolutePath() + " [Pack OK]");
        } catch (IOException ex) {
            showError("Save error", "Failed to compile document package: " + ex.getMessage());
            ex.printStackTrace();
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

    private void saveDocument(DocumentHandle handle, File targetFile) throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        CBORMapper cborMapper = new CBORMapper();

        // Настройки для создания нового ZIP-архива средствами Java
        java.util.Map<String, String> env = new java.util.HashMap<>();
        env.put("create", "true");

        if (targetFile.exists()) {
            targetFile.delete();
        }

        java.net.URI uri = java.net.URI.create("jar:file:" + targetFile.toURI().getPath());

        try (java.nio.file.FileSystem zipfs = java.nio.file.FileSystems.newFileSystem(uri, env)) {
            // 1. Создаем внутреннюю структуру пакета BDoc
            java.nio.file.Files.createDirectories(zipfs.getPath("/pages"));
            java.nio.file.Files.createDirectories(zipfs.getPath("/stories"));
            java.nio.file.Files.createDirectories(zipfs.getPath("/resources"));

            // 2. Сериализуем текстовые истории (Stories), которые пользователь очистил от рекламы
            for (String storyId : handle.getStoryIds()) {
                StoryModel story = handle.getStory(storyId);
                if (story != null) {
                    java.nio.file.Path storyPath = zipfs.getPath("/stories/" + storyId + ".json");
                    try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(storyPath)) {
                        jsonMapper.writerWithDefaultPrettyPrinter().writeValue(os, story);
                    }
                }
            }

            // 3. Сериализуем страницы (Pages) в бинарный CBOR с обновленной геометрией из памяти
            // Пробегаемся по всем индексам, которые вернул дескриптор
            for (Integer index : handle.getPageIndices()) {
                PageModel page = handle.loadPage(index); // Использует кэш handle, сохраняя правки Resize

                // Нам нужно определить имя файла страницы.
                // Для совместимости с дефолтным сэмплом используем шаблон "page-X.cbor"
                java.nio.file.Path pagePath = zipfs.getPath("/pages/page-" + index + ".cbor");
                try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(pagePath)) {
                    cborMapper.writeValue(os, page);
                }
            }

            // 4. Формируем и сохраняем manifest.json
            // Так как manifest в DocumentHandle скрыт (private), мы собираем его динамически
            // на основе метаданных и существующих индексов из открытого handle
            java.util.Map<String, Object> manifestMap = new java.util.HashMap<>();
            manifestMap.put("id", handle.getId());
            manifestMap.put("title", handle.getTitle());
            manifestMap.put("documentType", handle.getDocumentType());
            manifestMap.put("version", "0.1-composite");

            // Формируем список страниц для манифеста
            java.util.List<java.util.Map<String, Object>> pagesList = new java.util.ArrayList<>();
            for (Integer index : handle.getPageIndices()) {
                java.util.Map<String, Object> pEntry = new java.util.HashMap<>();
                pEntry.put("index", index);
                pEntry.put("id", "page-" + index);
                pEntry.put("file", "pages/page-" + index + ".cbor");
                pagesList.add(pEntry);
            }
            manifestMap.put("pages", pagesList);

            // Формируем список историй для манифеста
            java.util.List<java.util.Map<String, Object>> storiesList = new java.util.ArrayList<>();
            for (String storyId : handle.getStoryIds()) {
                java.util.Map<String, Object> sEntry = new java.util.HashMap<>();
                sEntry.put("id", storyId);
                sEntry.put("file", "stories/" + storyId + ".json");
                storiesList.add(sEntry);
            }
            manifestMap.put("stories", storiesList);

            java.nio.file.Path manifestPath = zipfs.getPath("/manifest.json");
            try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(manifestPath)) {
                jsonMapper.writerWithDefaultPrettyPrinter().writeValue(os, manifestMap);
            }

            // 5. Перенос каталога стилей (Styles)
            if (handle.getStyles() != null) {
                java.nio.file.Path stylesPath = zipfs.getPath("/styles.json");
                try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(stylesPath)) {
                    jsonMapper.writerWithDefaultPrettyPrinter().writeValue(os, handle.getStyles());
                }
            }
        }
    }
}