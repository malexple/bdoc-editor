package org.example.bdoc.ui.task;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.control.Label;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Безопасный запуск тяжёлых операций (OCR, сетевые вызовы ИИ, пересчёт
 * растра) вне UI-потока JavaFX (Пункт 9.1). Аналог
 * ProgressManager.getInstance().run(Task) в IntelliJ Platform.
 */
public final class TaskQueue {

    private final Stage ownerStage;

    public TaskQueue(Stage ownerStage) {
        this.ownerStage = ownerStage;
    }

    /**
     * Запускает supplier в фоновом потоке, показывает модальное окно с
     * прогресс-баром, и по завершении вызывает onSuccess в JavaFX-потоке
     * через Platform.runLater (сериализовано внутри Task/Worker API).
     */
    public <T> void run(String title, Supplier<T> backgroundWork, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return backgroundWork.get();
            }
        };

        Stage progressStage = new Stage();
        progressStage.initOwner(ownerStage);
        progressStage.initModality(Modality.WINDOW_MODAL);
        progressStage.setTitle(title);

        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(280);
        bar.progressProperty().bind(task.progressProperty());
        VBox box = new VBox(10, new Label(title), bar);
        box.setPadding(new Insets(16));
        progressStage.setScene(new Scene(box));

        task.setOnSucceeded(e -> {
            progressStage.close();
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            progressStage.close();
            if (onError != null) onError.accept(task.getException());
        });

        Thread thread = new Thread(task, "bdoc-task-" + title);
        thread.setDaemon(true);
        thread.start();
        progressStage.show();
    }
}