package org.example.bdoc.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.example.bdoc.model.BdocObject;
import org.example.bdoc.model.DocumentModel;
import org.example.bdoc.model.Geometry;
import org.example.bdoc.model.LayerModel;
import org.example.bdoc.model.PageModel;
import org.example.bdoc.model.ShapeType;
import org.example.bdoc.model.StoryModel;
import org.example.bdoc.model.TextFrame;
import org.example.bdoc.model.VectorShape;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PageRenderer {

    public void render(GraphicsContext gc, DocumentModel document, PageModel page) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, page.getWidth(), page.getHeight());

        Map<String, StoryModel> stories = document.getStories().stream()
                .collect(Collectors.toMap(StoryModel::getId, Function.identity()));

        Map<String, LayerModel> layers = page.getLayers().stream()
                .collect(Collectors.toMap(LayerModel::getId, Function.identity()));

        page.getObjects().stream()
                .filter(object -> {
                    LayerModel layer = layers.get(object.getLayerRef());
                    return layer != null && layer.isVisible();
                })
                .sorted(Comparator.comparingInt(o -> layers.get(o.getLayerRef()).getZIndex()))
                .forEach(object -> renderObject(gc, object, stories));
    }

    private void renderObject(GraphicsContext gc, BdocObject object, Map<String, StoryModel> stories) {
        if (object instanceof VectorShape shape) {
            renderShape(gc, shape);
        } else if (object instanceof TextFrame textFrame) {
            renderTextFrame(gc, textFrame, stories.get(textFrame.getStoryRef()));
        }
    }

    private void renderShape(GraphicsContext gc, VectorShape shape) {
        Geometry g = shape.getGeometry();
        gc.setStroke(Color.web("#2F4858"));
        gc.setLineWidth(2.0);

        if (shape.getShapeType() == ShapeType.RECTANGLE) {
            gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());
        } else if (shape.getShapeType() == ShapeType.ROUNDED_RECTANGLE) {
            gc.strokeRoundRect(
                    g.getX(), g.getY(), g.getWidth(), g.getHeight(),
                    g.getArcWidth() != null ? g.getArcWidth() : 0,
                    g.getArcHeight() != null ? g.getArcHeight() : 0
            );
        } else if (shape.getShapeType() == ShapeType.LINE) {
            gc.strokeLine(g.getX(), g.getY(), g.getX() + g.getWidth(), g.getY() + g.getHeight());
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
}