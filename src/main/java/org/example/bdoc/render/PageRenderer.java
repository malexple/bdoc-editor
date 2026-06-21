package org.example.bdoc.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.example.bdoc.model.BdocObject;
import org.example.bdoc.model.DocumentModel;
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
                    LayerModel layer = layers.get(object.layerId());
                    return layer != null && layer.isVisible();
                })
                .sorted(Comparator.comparingInt(o -> layers.get(o.layerId()).getZIndex()))
                .forEach(object -> renderObject(gc, object, stories));
    }

    private void renderObject(GraphicsContext gc, BdocObject object, Map<String, StoryModel> stories) {
        if (object instanceof VectorShape shape) {
            renderShape(gc, shape);
        } else if (object instanceof TextFrame textFrame) {
            renderTextFrame(gc, textFrame, stories.get(textFrame.storyId()));
        }
    }

    private void renderShape(GraphicsContext gc, VectorShape shape) {
        gc.setStroke(Color.web("#2F4858"));
        gc.setLineWidth(2.0);

        if (shape.shapeType() == ShapeType.RECTANGLE) {
            gc.strokeRect(shape.x(), shape.y(), shape.width(), shape.height());
        } else if (shape.shapeType() == ShapeType.ROUNDED_RECTANGLE) {
            gc.strokeRoundRect(shape.x(), shape.y(), shape.width(), shape.height(), shape.arcWidth(), shape.arcHeight());
        } else if (shape.shapeType() == ShapeType.LINE) {
            gc.strokeLine(shape.x(), shape.y(), shape.x() + shape.width(), shape.y() + shape.height());
        }
    }

    private void renderTextFrame(GraphicsContext gc, TextFrame textFrame, StoryModel story) {
        gc.setStroke(Color.web("#94A3B8"));
        gc.setLineWidth(1.0);
        gc.strokeRect(textFrame.x(), textFrame.y(), textFrame.width(), textFrame.height());

        gc.setFill(Color.web("#0F172A"));
        gc.setFont(Font.font("Serif", 20));
        gc.setTextAlign(TextAlignment.LEFT);

        String text = story != null ? story.getText() : "<missing story>";
        gc.fillText(text, textFrame.x() + 12, textFrame.y() + 32, textFrame.width() - 24);
    }
}