package org.example.bdoc.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

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
        MasterPage masterPage = document.getMasterPage(page.getTemplateRef());
        List<BdocObject> effectiveObjects = resolveEffectiveObjects(page, masterPage);

        effectiveObjects.stream()
                .filter(object -> {
                    LayerModel layer = layers.get(object.getLayerRef());
                    return layer != null && layer.isVisible() && object.isVisible();
                })
                .sorted(Comparator.comparingInt(o -> page.getLayers().indexOf(layers.get(o.getLayerRef()))))
                .forEach(object -> renderObject(gc, object, document, layers.get(object.getLayerRef()),
                        styleResolver, selectedObject, isRawMasterObject(object, masterPage)));
    }

    /**
     * Строит итоговый список объектов страницы с учётом мастер-страницы:
     * - объекты мастера, НЕ переопределённые на странице, подмешиваются как есть
     *   (locked-статус визуально не подчёркиваем здесь — это ответственность UI);
     * - объекты мастера, у которых есть локальная копия с masterSourceId,
     *   пересобираются через mergeWithMaster: базой служит объект мастера,
     *   а свойства из overriddenProperties берутся из локальной копии;
     * - собственные объекты страницы без masterSourceId идут как есть.
     */
    public List<BdocObject> resolveEffectiveObjects(PageModel page, MasterPage masterPage) {
        if (masterPage == null) {
            return page.getObjects();
        }

        Map<String, BdocObject> overridesByMasterSourceId = new HashMap<>();
        List<BdocObject> ownObjects = new ArrayList<>();
        for (BdocObject object : page.getObjects()) {
            if (object.isMasterOverride()) {
                overridesByMasterSourceId.put(object.getMasterSourceId(), object);
            } else {
                ownObjects.add(object);
            }
        }

        List<BdocObject> result = new ArrayList<>();
        for (BdocObject masterObject : masterPage.getObjects()) {
            BdocObject override = overridesByMasterSourceId.get(masterObject.getId());
            if (override != null) {
                result.add(mergeWithMaster(masterObject, override));
            } else {
                result.add(masterObject);
            }
        }
        result.addAll(ownObjects);
        return result;
    }

    /**
     * Пересобирает объект: геометрия и специфичные для типа поля берутся
     * из override только если их имя присутствует в overriddenProperties,
     * иначе значение наследуется от объекта мастера.
     */
    private BdocObject mergeWithMaster(BdocObject masterObject, BdocObject override) {
        Set<String> overridden = override.getOverriddenProperties();
        Geometry geometry = overridden.contains("geometry") ? override.getGeometry() : masterObject.getGeometry();
        String layerRef = overridden.contains("layerRef") ? override.getLayerRef() : masterObject.getLayerRef();

        if (masterObject instanceof TextFrame master && override instanceof TextFrame overrideFrame) {
            String storyRef = overridden.contains("storyRef") ? overrideFrame.getStoryRef() : master.getStoryRef();
            return new TextFrame(override.getId(), layerRef, geometry, storyRef);
        }
        if (masterObject instanceof ImageFrame master && override instanceof ImageFrame overrideFrame) {
            String assetRef = overridden.contains("assetRef") ? overrideFrame.getAssetRef() : master.getAssetRef();
            return new ImageFrame(override.getId(), layerRef, geometry, assetRef);
        }
        if (masterObject instanceof VectorShape master && override instanceof VectorShape overrideShape) {
            String shapeType = overridden.contains("shapeType") ? overrideShape.getShapeType() : master.getShapeType();
            return new VectorShape(override.getId(), layerRef, geometry, shapeType);
        }
        if (masterObject instanceof HeaderFooterRule master && override instanceof HeaderFooterRule overrideRule) {
            String zone = overridden.contains("zone") ? overrideRule.getZone() : master.getZone();
            String textTemplate = overridden.contains("textTemplate") ? overrideRule.getTextTemplate() : master.getTextTemplate();
            String styleRef = overridden.contains("styleRef") ? overrideRule.getStyleRef() : master.getStyleRef();
            return new HeaderFooterRule(override.getId(), layerRef, geometry, zone, textTemplate, styleRef);
        }

        // Неизвестная комбинация типов — считаем override полностью самостоятельным
        return override;
    }

    private void renderObject(GraphicsContext gc, BdocObject object, DocumentHandle document,
                              LayerModel layer, StyleResolver styleResolver, BdocObject selectedObject,
                              boolean isRawMaster) {
        CharacterStyleResolver characterStyleResolver = new CharacterStyleResolver(document.getStyles());

        gc.setGlobalAlpha(layer.getOpacity());
        try {
            if (object instanceof VectorShape shape) {
                renderShape(gc, shape);
            } else if (object instanceof TextFrame textFrame) {
                renderTextFrame(gc, textFrame, document.getStory(textFrame.getStoryRef()), styleResolver, characterStyleResolver);
            } else if (object instanceof ImageFrame imageFrame) {
                renderImageFrame(gc, imageFrame, document);
            } else if (object instanceof HeaderFooterRule rule) {
                renderHeaderFooterRule(gc, rule);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to render object: " + object.getId(), e);
        } finally {
            gc.setGlobalAlpha(1.0);
        }

        if (object == selectedObject) {
            Geometry g = object.getGeometry();
            if (object.getType().equals("VectorShape") || object.getType().equals("ImageFrame")) {
                gc.setStroke(Color.web("#2563EB"));
                gc.setLineWidth(2.0);
                gc.strokeRect(g.getX() - 1, g.getY() - 1, g.getWidth() + 2, g.getHeight() + 2);

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
                gc.setStroke(Color.web("#10B981"));
                gc.setLineWidth(2.0);
                gc.strokeRect(g.getX() - 1, g.getY() - 1, g.getWidth() + 2, g.getHeight() + 2);
            }
        } else if (isRawMaster) {
            // Объект унаследован от мастера и ещё не имеет локального override —
            // рисуем тонкую пунктирную рамку, сигнализирующую "заблокировано от мастера"
            Geometry g = object.getGeometry();
            gc.setStroke(Color.web("#94A3B8"));
            gc.setLineWidth(1.0);
            gc.setLineDashes(4.0, 4.0);
            gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());
            gc.setLineDashes(null);
        }
    }

    private boolean isFromMasterCandidate(BdocObject object) {
        return false; // placeholder до реализации в BdocEditorApp
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

    private void renderHeaderFooterRule(GraphicsContext gc, HeaderFooterRule rule) {
        Geometry g = rule.getGeometry();
        gc.setStroke(Color.web("#94A3B8"));
        gc.setLineWidth(1.0);
        gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());

        gc.setFill(Color.web("#475569"));
        gc.setFont(Font.font("Serif", 11));
        String preview = rule.getTextTemplate() != null ? rule.getTextTemplate() : "";
        gc.fillText(preview, g.getX() + 4, g.getY() + g.getHeight() - 4);
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

    /**
     * Определяет, является ли объект "сырым" объектом мастера без локального
     * override — такие объекты визуально помечаются как заблокированные
     * и требуют материализации копии перед редактированием (см. BdocEditorApp).
     */
    public boolean isRawMasterObject(BdocObject object, MasterPage masterPage) {
        return masterPage != null && !object.isMasterOverride()
                && masterPage.findObject(object.getId()) != null;
    }
}