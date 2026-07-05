package org.example.bdoc.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;
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
        gc.save();
        applyTransform(gc, object);
        try {
            if (object instanceof VectorShape shape) {
                renderShape(gc, shape);
            } else if (object instanceof TextFrame textFrame) {
                renderTextFrame(gc, textFrame, document.getStory(textFrame.getStoryRef()), styleResolver, characterStyleResolver);
            } else if (object instanceof ImageFrame imageFrame) {
                renderImageFrame(gc, imageFrame, document);
            } else if (object instanceof HeaderFooterRule rule) {
                renderHeaderFooterRule(gc, rule);
            } else if (object instanceof LineObject line) {
                renderLineObject(gc, line);
            } else if (object instanceof TableFrame table) {
                renderTableFrame(gc, table, document);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to render object: " + object.getId(), e);
        }

        // Выделение и артефактные рамки рисуются в ТОЙ ЖЕ трансформированной
        // системе координат — благодаря этому синие хендлы и пунктир артефакта
        // визуально поворачиваются вместе с объектом. Обратный пересчёт клика
        // мыши в локальные координаты повёрнутого объекта — задача Этапа 2.
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
            } else if (object instanceof Group) {
                gc.setStroke(Color.web("#7C3AED"));
                gc.setLineWidth(2.0);
                gc.setLineDashes(6.0, 3.0);
                gc.strokeRect(g.getX() - 2, g.getY() - 2, g.getWidth() + 4, g.getHeight() + 4);
                gc.setLineDashes(null);
            }
        } else if (object.isArtifact()) {
            Geometry g = object.getGeometry();
            gc.setStroke(Color.web("#DC2626"));
            gc.setLineWidth(1.0);
            gc.setLineDashes(2.0, 2.0);
            gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());
            gc.setLineDashes(null);
        } else if (isRawMaster) {
            Geometry g = object.getGeometry();
            gc.setStroke(Color.web("#94A3B8"));
            gc.setLineWidth(1.0);
            gc.setLineDashes(4.0, 4.0);
            gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());
            gc.setLineDashes(null);
        }

        gc.restore();
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Накладывает Transform объекта на GraphicsContext перед рисованием.
     * Поворот и масштаб выполняются вокруг центра bounding box (Geometry),
     * поэтому сам объект продолжает рисоваться обычными абсолютными
     * координатами g.getX()/g.getY() — вращение "бесплатно" для остальных
     * renderXxx-методов, им не нужно знать о Transform вообще.
     */
    private void applyTransform(GraphicsContext gc, BdocObject object) {
        TransformModel t = object.getTransform();
        if (t == null || t.isIdentity()) {
            return;
        }
        Geometry g = object.getGeometry();
        double centerX = g.getX() + g.getWidth() / 2.0;
        double centerY = g.getY() + g.getHeight() / 2.0;

        gc.translate(centerX, centerY);
        gc.rotate(t.getRotationDegrees());
        gc.scale(t.getScaleX(), t.getScaleY());
        gc.translate(-centerX, -centerY);
        gc.translate(t.getTranslateX(), t.getTranslateY());
    }

    private boolean isFromMasterCandidate(BdocObject object) {
        return false; // placeholder до реализации в BdocEditorApp
    }

    private void renderShape(GraphicsContext gc, VectorShape shape) {
        Geometry g = shape.getGeometry();
        gc.setStroke(Color.web("#2F4858"));
        gc.setLineWidth(2.0);

        PathModel pathData = shape.getPathData();
        if (pathData != null && !pathData.getPoints().isEmpty()
                && !"primitive".equals(pathData.getContourType())) {
            renderContour(gc, pathData);
            return;
        }

        switch (shape.getShapeType()) {
            case "rectangle" -> gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());
            case "rounded-rectangle" -> gc.strokeRoundRect(
                    g.getX(), g.getY(), g.getWidth(), g.getHeight(),
                    g.getArcWidth() != null ? g.getArcWidth() : 0,
                    g.getArcHeight() != null ? g.getArcHeight() : 0);
            case "line" -> gc.strokeLine(g.getX(), g.getY(), g.getX() + g.getWidth(), g.getY() + g.getHeight());
            case "ellipse" -> gc.strokeOval(g.getX(), g.getY(), g.getWidth(), g.getHeight());
            case "polygon" -> gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight()); // fallback, если pathData пуст
            default -> throw new IllegalArgumentException("Unknown shapeType: " + shape.getShapeType());
        }
    }

    private void renderContourOrFallback(GraphicsContext gc, VectorShape shape) {
        PathModel pathData = shape.getPathData();
        if (pathData == null || pathData.getPoints().isEmpty()) {
            Geometry g = shape.getGeometry();
            gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());
            return;
        }
        renderContour(gc, pathData);
    }

    /**
     * Обобщённый рендер контура PathModel: поддерживает несколько суб-контуров
     * (CompoundPath через повторяющиеся команды "M" в одном points-массиве —
     * пример "буква О с дыркой"), команды M/L/C и fillRule even-odd/non-zero.
     * Заливка не включена явно (у VectorShape пока нет отдельного fillColor),
     * поэтому используется только stroke — включение fill предусмотрено на
     * будущее без изменения сигнатуры метода.
     */
    private void renderContour(GraphicsContext gc, PathModel pathData) {
        gc.save();
        gc.setFillRule("even-odd".equals(pathData.getFillRule()) ? FillRule.EVEN_ODD : FillRule.NON_ZERO);
        gc.beginPath();
        for (PathPoint p : pathData.getPoints()) {
            switch (p.getCommand()) {
                case "M" -> gc.moveTo(p.getX(), p.getY());
                case "L" -> gc.lineTo(p.getX(), p.getY());
                case "C" -> gc.bezierCurveTo(p.getX1(), p.getY1(), p.getX2(), p.getY2(), p.getX(), p.getY());
                default -> { }
            }
        }
        gc.closePath();

        gc.setFill(Color.web("#475569", 0.6));
        gc.fill();
        gc.stroke();
        gc.restore();
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

    private void renderLineObject(GraphicsContext gc, LineObject line) {
        gc.setStroke(Color.web(line.getStrokeColor() != null ? line.getStrokeColor() : "#000000"));
        gc.setLineWidth(line.getStrokeWidth());

        switch (line.getStrokePattern()) {
            case "dashed" -> gc.setLineDashes(6.0, 4.0);
            case "dotted" -> gc.setLineDashes(1.5, 3.0);
            case "double" -> {
                drawDoubleStroke(gc, line);
                drawLineCaps(gc, line);
                gc.setLineDashes(null);
                return;
            }
            default -> gc.setLineDashes(null);
        }

        gc.strokeLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
        gc.setLineDashes(null);
        drawLineCaps(gc, line);
    }

    private void drawDoubleStroke(GraphicsContext gc, LineObject line) {
        double dx = line.getX2() - line.getX1();
        double dy = line.getY2() - line.getY1();
        double len = Math.hypot(dx, dy);
        if (len == 0) return;
        double offsetX = -dy / len * (line.getStrokeWidth() * 0.8);
        double offsetY = dx / len * (line.getStrokeWidth() * 0.8);
        gc.setLineWidth(Math.max(0.5, line.getStrokeWidth() / 2.5));
        gc.strokeLine(line.getX1() + offsetX, line.getY1() + offsetY, line.getX2() + offsetX, line.getY2() + offsetY);
        gc.strokeLine(line.getX1() - offsetX, line.getY1() - offsetY, line.getX2() - offsetX, line.getY2() - offsetY);
    }

    private void drawLineCaps(GraphicsContext gc, LineObject line) {
        if ("arrow".equals(line.getEndCap())) {
            drawArrowHead(gc, line.getX1(), line.getY1(), line.getX2(), line.getY2(), line.getStrokeWidth());
        }
        if ("arrow".equals(line.getStartCap())) {
            drawArrowHead(gc, line.getX2(), line.getY2(), line.getX1(), line.getY1(), line.getStrokeWidth());
        }
    }

    private void drawArrowHead(GraphicsContext gc, double fromX, double fromY, double toX, double toY, double strokeWidth) {
        double angle = Math.atan2(toY - fromY, toX - fromX);
        double arrowLength = 8 + strokeWidth * 2;
        double arrowAngle = Math.toRadians(25);
        double x1 = toX - arrowLength * Math.cos(angle - arrowAngle);
        double y1 = toY - arrowLength * Math.sin(angle - arrowAngle);
        double x2 = toX - arrowLength * Math.cos(angle + arrowAngle);
        double y2 = toY - arrowLength * Math.sin(angle + arrowAngle);
        gc.fillPolygon(new double[]{toX, x1, x2}, new double[]{toY, y1, y2}, 3);
    }

    private void renderTableFrame(GraphicsContext gc, TableFrame table, DocumentHandle document) {
        Geometry g = table.getGeometry();
        double totalWidthRatio = table.getColumns().stream().mapToDouble(TableColumn::getWidthRatio).sum();
        double totalHeightRatio = table.getRows().stream().mapToDouble(TableRow::getHeightRatio).sum();
        if (totalWidthRatio <= 0) totalWidthRatio = table.getColumnCount();
        if (totalHeightRatio <= 0) totalHeightRatio = table.getRowCount();

        double[] colWidths = new double[table.getColumnCount()];
        double[] rowHeights = new double[table.getRowCount()];
        for (int c = 0; c < table.getColumnCount(); c++) {
            double ratio = c < table.getColumns().size() ? table.getColumns().get(c).getWidthRatio() : 1.0;
            colWidths[c] = g.getWidth() * (ratio / totalWidthRatio);
        }
        for (int r = 0; r < table.getRowCount(); r++) {
            double ratio = r < table.getRows().size() ? table.getRows().get(r).getHeightRatio() : 1.0;
            rowHeights[r] = g.getHeight() * (ratio / totalHeightRatio);
        }

        double[] colStartX = new double[table.getColumnCount() + 1];
        colStartX[0] = g.getX();
        for (int c = 0; c < table.getColumnCount(); c++) {
            colStartX[c + 1] = colStartX[c] + colWidths[c];
        }
        double[] rowStartY = new double[table.getRowCount() + 1];
        rowStartY[0] = g.getY();
        for (int r = 0; r < table.getRowCount(); r++) {
            rowStartY[r + 1] = rowStartY[r] + rowHeights[r];
        }

        gc.setStroke(Color.web("#94A3B8"));
        gc.setLineWidth(1.0);
        gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());
        for (int c = 1; c < table.getColumnCount(); c++) {
            gc.strokeLine(colStartX[c], g.getY(), colStartX[c], g.getY() + g.getHeight());
        }
        for (int r = 1; r < table.getRowCount(); r++) {
            gc.strokeLine(g.getX(), rowStartY[r], g.getX() + g.getWidth(), rowStartY[r]);
        }

        gc.setFill(Color.web("#1E293B"));
        gc.setFont(Font.font("Georgia", 13));
        for (TableCell cell : table.getCells()) {
            if (cell.getStoryRef() == null) continue;
            StoryModel cellStory = document.getStory(cell.getStoryRef());
            if (cellStory == null) continue;
            double cellX = colStartX[cell.getColIndex()];
            double cellY = rowStartY[cell.getRowIndex()];
            double cellW = colWidths[cell.getColIndex()];
            double cellH = rowHeights[cell.getRowIndex()];
            String text = cellStory.getJoinedText();
            gc.fillText(text, cellX + 6, cellY + cellH / 2 + 4, cellW - 12);
        }
    }
}