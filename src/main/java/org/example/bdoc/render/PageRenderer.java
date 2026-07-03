package org.example.bdoc.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class PageRenderer {

    private final Map<String, Image> imageCache = new HashMap<>();

    public void render(GraphicsContext gc, DocumentHandle document, PageModel page) throws IOException {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, page.getWidth(), page.getHeight());

        Map<String, LayerModel> layers = new HashMap<>();
        for (LayerModel layer : page.getLayers()) {
            layers.put(layer.getId(), layer);
        }

        page.getObjects().stream()
                .filter(object -> {
                    LayerModel layer = layers.get(object.getLayerRef());
                    return layer != null && layer.isVisible();
                })
                .sorted(Comparator.comparingInt(o -> page.getLayers().indexOf(layers.get(o.getLayerRef()))))
                .forEach(object -> renderObject(gc, object, document, layers.get(object.getLayerRef())));
    }

    private void renderObject(GraphicsContext gc, BdocObject object, DocumentHandle document, LayerModel layer) {
        gc.setGlobalAlpha(layer.getOpacity());
        try {
            if (object instanceof VectorShape shape) {
                renderShape(gc, shape);
            } else if (object instanceof TextFrame textFrame) {
                renderTextFrame(gc, textFrame, document.getStory(textFrame.getStoryRef()));
            } else if (object instanceof ImageFrame imageFrame) {
                renderImageFrame(gc, imageFrame, document);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to render object: " + object.getId(), e);
        } finally {
            gc.setGlobalAlpha(1.0);
        }
    }

    private void renderShape(GraphicsContext gc, VectorShape shape) {
        Geometry g = shape.getGeometry();
        gc.setStroke(Color.web("#2F4858"));
        gc.setLineWidth(2.0);

        switch (shape.getShapeType()) {
            case "rectangle" -> gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());
            case "rounded-rectangle" -> gc.strokeRoundRect(
                    g.getX(), g.getY(), g.getWidth(), g.getHeight(),
                    g.getArcWidth() != null ? g.getArcWidth() : 0,
                    g.getArcHeight() != null ? g.getArcHeight() : 0);
            case "line" -> gc.strokeLine(g.getX(), g.getY(), g.getX() + g.getWidth(), g.getY() + g.getHeight());
            default -> throw new IllegalArgumentException("Unknown shapeType: " + shape.getShapeType());
        }
    }

    private void renderTextFrame(GraphicsContext gc, TextFrame textFrame, StoryModel story) {
        Geometry g = textFrame.getGeometry();

        gc.setStroke(Color.web("#94A3B8"));
        gc.setLineWidth(1.0);
        gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());

        gc.setFill(Color.web("#0F172A"));
        gc.setFont(Font.font("Serif", 20));
        gc.setTextAlign(TextAlignment.LEFT);

        String text = story != null ? story.getJoinedText() : "<missing story>";
        gc.fillText(text, g.getX() + 12, g.getY() + 32, g.getWidth() - 24);
    }

    private void renderImageFrame(GraphicsContext gc, ImageFrame imageFrame, DocumentHandle document) throws IOException {
        Geometry g = imageFrame.getGeometry();
        Image image = imageCache.computeIfAbsent(imageFrame.getAssetRef(), ref -> {
            try {
                byte[] bytes = document.loadResourceBytes(ref);
                return new Image(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                throw new RuntimeException("Failed to load image asset: " + ref, e);
            }
        });
        gc.drawImage(image, g.getX(), g.getY(), g.getWidth(), g.getHeight());
    }
}