package org.example.bdoc.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageRenderer {

    private final Map<String, Image> imageCache = new HashMap<>();

    public void render(GraphicsContext gc, DocumentHandle document, PageModel page, BdocObject selectedObject) throws IOException {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, page.getWidth(), page.getHeight());

        Map<String, LayerModel> layers = new HashMap<>();
        for (LayerModel layer : page.getLayers()) {
            layers.put(layer.getId(), layer);
        }

        StyleResolver styleResolver = new StyleResolver(document.getStyles());

        page.getObjects().stream()
                .filter(object -> {
                    LayerModel layer = layers.get(object.getLayerRef());
                    return layer != null && layer.isVisible();
                })
                .sorted(Comparator.comparingInt(o -> page.getLayers().indexOf(layers.get(o.getLayerRef()))))
                .forEach(object -> renderObject(gc, object, document, layers.get(object.getLayerRef()), styleResolver, selectedObject));
    }

    private void renderObject(GraphicsContext gc, BdocObject object, DocumentHandle document,
                              LayerModel layer, StyleResolver styleResolver, BdocObject selectedObject) {
        CharacterStyleResolver characterStyleResolver = new CharacterStyleResolver(document.getStyles());

        gc.setGlobalAlpha(layer.getOpacity());
        try {
            if (object instanceof VectorShape shape) {
                renderShape(gc, shape);
            } else if (object instanceof TextFrame textFrame) {
                renderTextFrame(gc, textFrame, document.getStory(textFrame.getStoryRef()), styleResolver, characterStyleResolver);
            } else if (object instanceof ImageFrame imageFrame) {
                renderImageFrame(gc, imageFrame, document);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to render object: " + object.getId(), e);
        } finally {
            gc.setGlobalAlpha(1.0);
        }

        if (object == selectedObject) {
            gc.setStroke(Color.web("#2563EB")); // Яркий синий цвет выделения Adobe InDesign
            gc.setLineWidth(2.0);
            Geometry g = object.getGeometry();
            // Если мы просто выделили объект стрелкой — рисуем синий контур InDesign
            if (object.getType().equals("VectorShape") || object.getType().equals("ImageFrame")) {
                gc.setStroke(Color.web("#2563EB"));
                gc.setLineWidth(2.0);
                gc.strokeRect(g.getX() - 1, g.getY() - 1, g.getWidth() + 2, g.getHeight() + 2);
                // Рисуем габаритную рамку выделения
//            gc.strokeRect(g.getX() - 1, g.getY() - 1, g.getWidth() + 2, g.getHeight() + 2);

                // Рисуем маленькие белые квадратики-маркеры по углам фрейма
                gc.setFill(Color.WHITE);
                gc.setStroke(Color.web("#2563EB"));
                gc.setLineWidth(1.0);

                double size = 6.0;
                gc.fillRect(g.getX() - size / 2, g.getY() - size / 2, size, size);
                gc.strokeRect(g.getX() - size / 2, g.getY() - size / 2, size, size);

                gc.fillRect(g.getX() + g.getWidth() - size / 2, g.getY() - size / 2, size, size);
                gc.strokeRect(g.getX() + g.getWidth() - size / 2, g.getY() - size / 2, size, size);

                gc.fillRect(g.getX() - size / 2, g.getY() + g.getHeight() - size / 2, size, size);
                gc.strokeRect(g.getX() - size / 2, g.getY() + g.getHeight() - size / 2, size, size);

                gc.fillRect(g.getX() + g.getWidth() - size / 2, g.getY() + g.getHeight() - size / 2, size, size);
                gc.strokeRect(g.getX() + g.getWidth() - size / 2, g.getY() + g.getHeight() - size / 2, size, size);
            } else if (object instanceof TextFrame) {
                // Мы можем сделать рамку зеленой/фиолетовой при редактировании текста
                gc.setStroke(Color.web("#10B981")); // Изумрудно-зеленый DTP цвет текстового фокуса
                gc.setLineWidth(2.0);
                gc.strokeRect(g.getX() - 1, g.getY() - 1, g.getWidth() + 2, g.getHeight() + 2);
            }
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

    private void renderTextFrame(GraphicsContext gc, TextFrame textFrame, StoryModel story,
                                 StyleResolver styleResolver, CharacterStyleResolver characterStyleResolver) {
        Geometry g = textFrame.getGeometry();

        gc.setStroke(Color.web("#94A3B8"));
        gc.setLineWidth(1.0);
        gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());

        if (story == null) {
            gc.setFill(Color.web("#0F172A"));
            gc.setFont(Font.font("Serif", 20));
            gc.fillText("<missing story>", g.getX() + 12, g.getY() + 32);
            return;
        }

        double paddingX = 12.0;
        double cursorY = g.getY() + paddingX;
        double maxWidth = g.getWidth() - paddingX * 2;
        double frameBottom = g.getY() + g.getHeight();

        outer:
        for (Paragraph paragraph : story.getParagraphs()) {
            EffectiveParagraphStyle paragraphStyle = styleResolver.resolve(paragraph.getStyleRef());

            List<TextWrapper.SpanInput> spanInputs = paragraph.getSpans().stream()
                    .map(span -> new TextWrapper.SpanInput(
                            span.getText(),
                            characterStyleResolver.resolve(span.getCharacterStyleRef(), paragraphStyle)))
                    .toList();

            List<List<TextWrapper.Run>> lines = TextWrapper.wrapSpans(spanInputs, maxWidth);
            double lineAdvance = paragraphStyle.getFontSize() * paragraphStyle.getLineHeight();

            for (List<TextWrapper.Run> line : lines) {
                if (cursorY + lineAdvance > frameBottom) {
                    break outer;
                }
                cursorY += lineAdvance;
                renderLine(gc, line, g, paddingX, cursorY, paragraphStyle.getAlignment());
            }

            cursorY += lineAdvance * 0.3;
        }
    }

    private void renderLine(GraphicsContext gc, List<TextWrapper.Run> runs, Geometry g,
                            double paddingX, double baselineY, String alignment) {
        if (runs.isEmpty()) {
            return;
        }

        Text measurer = new Text();
        double totalWidth = 0;
        for (TextWrapper.Run run : runs) {
            measurer.setFont(run.font);
            measurer.setText(run.text);
            totalWidth += measurer.getLayoutBounds().getWidth();
        }

        double startX = switch (alignment) {
            case "center" -> g.getX() + (g.getWidth() - totalWidth) / 2.0;
            case "right" -> g.getX() + g.getWidth() - paddingX - totalWidth;
            default -> g.getX() + paddingX;
        };

        gc.setTextAlign(TextAlignment.LEFT);
        double cursorX = startX;
        for (TextWrapper.Run run : runs) {
            gc.setFont(run.font);
            gc.setFill(Color.web(run.color));
            gc.fillText(run.text, cursorX, baselineY);

            measurer.setFont(run.font);
            measurer.setText(run.text);
            cursorX += measurer.getLayoutBounds().getWidth();
        }
    }

    private TextAlignment resolveAlignment(String alignment) {
        return switch (alignment) {
            case "center" -> TextAlignment.CENTER;
            case "right" -> TextAlignment.RIGHT;
            case "justify" -> TextAlignment.JUSTIFY;
            default -> TextAlignment.LEFT;
        };
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