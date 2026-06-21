package org.example.bdoc.render;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import org.example.bdoc.model.DocumentModel;
import org.example.bdoc.model.PageModel;
import org.example.bdoc.model.SampleDocuments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PageRendererTest {

    @BeforeAll
    static void initToolkit() {
        new JFXPanel(); // инициализирует FX toolkit и запускает FX thread
    }

    @Test
    void rendersSamplePageToImage() throws InterruptedException {
        DocumentModel document = SampleDocuments.sample();
        PageModel page = document.getPages().get(0);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<WritableImage> imageRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Canvas canvas = new Canvas(page.getWidth(), page.getHeight());
                new PageRenderer().render(canvas.getGraphicsContext2D(), document, page);
                imageRef.set(canvas.snapshot(null, null));
            } finally {
                latch.countDown();
            }
        });

        latch.await(); // ждём пока FX thread завершит работу

        WritableImage image = imageRef.get();
        assertNotNull(image);
        assertEquals((int) page.getWidth(), (int) image.getWidth());
        assertEquals((int) page.getHeight(), (int) image.getHeight());
    }
}