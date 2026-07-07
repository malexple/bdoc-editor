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

/**
 * Отвечает ИСКЛЮЧИТЕЛЬНО за отрисовку "чистого" документа: объекты, стили,
 * мастер-страницы, prepress-guides. Рамка выделения и resize-хендлы больше
 * не рисуются здесь (см. Этап 2, вопрос 4) — они переехали в
 * SelectionToolStrategy.renderOverlay, который вызывается ядром ПОСЛЕ
 * render() отдельным проходом по тому же GraphicsContext. Благодаря этому
 * PageRenderer можно использовать и для headless-экспорта/печати, где
 * никакого UI-выделения не существует по определению.
 */
public class PageRenderer {

    private final Map<String, Image> imageCache = new HashMap<>();

    public void render(GraphicsContext gc, DocumentHandle document, PageModel page) throws IOException {
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
                        styleResolver, isRawMasterObject(object, masterPage)));

        renderPrepressGuides(gc, document, page, masterPage);
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
                              LayerModel layer, StyleResolver styleResolver, boolean isRawMaster) {
        CharacterStyleResolver characterStyleResolver = new CharacterStyleResolver(document.getStyles());

        double effectiveOpacity = ObjectStyleResolver.resolveOpacity(object, document.getStyles());
        gc.setGlobalAlpha(layer.getOpacity() * effectiveOpacity);
        gc.save();
        applyTransform(gc, object);
        try {
            if (object instanceof VectorShape shape) {
                renderShape(gc, shape, document.getStyles());
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

        // Служебные page-level индикаторы (не relevant к активному инструменту):
        // артефакты помечаются красным пунктиром, "сырые" объекты мастера — серым.
        // Это состояние документа/страницы, а не эфемерный UI-стейт инструмента,
        // поэтому оно осталось в PageRenderer.
        if (object.isArtifact()) {
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

    private void renderShape(GraphicsContext gc, VectorShape shape, StylesCatalog styles) {
        Geometry g = shape.getGeometry();

        String strokeHex = ColorResolver.resolve(shape.getStrokeColor(), shape.getStrokeColorSwatchRef(), styles);
        gc.setStroke(Color.web(strokeHex != null ? strokeHex : "#2F4858"));
        gc.setLineWidth(2.0);

        String fillHex = ColorResolver.resolve(shape.getFillColor(), shape.getFillColorSwatchRef(), styles);

        PathModel pathData = shape.getPathData();
        if (pathData != null && !pathData.getPoints().isEmpty()
                && !"primitive".equals(pathData.getContourType())) {
            renderContour(gc, pathData, fillHex);
            return;
        }

        if (fillHex != null) {
            gc.setFill(Color.web(fillHex));
        }

        switch (shape.getShapeType()) {
            case "rectangle" -> {
                if (fillHex != null) gc.fillRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());
                gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());
            }
            case "rounded-rectangle" -> {
                double arcW = ObjectStyleResolver.resolveArcWidth(shape, styles);
                double arcH = ObjectStyleResolver.resolveArcHeight(shape, styles);
                if (fillHex != null) {
                    gc.fillRoundRect(g.getX(), g.getY(), g.getWidth(), g.getHeight(), arcW, arcH);
                }
                gc.strokeRoundRect(g.getX(), g.getY(), g.getWidth(), g.getHeight(), arcW, arcH);
            }
            case "line" -> gc.strokeLine(g.getX(), g.getY(), g.getX() + g.getWidth(), g.getY() + g.getHeight());
            case "ellipse" -> {
                if (fillHex != null) gc.fillOval(g.getX(), g.getY(), g.getWidth(), g.getHeight());
                gc.strokeOval(g.getX(), g.getY(), g.getWidth(), g.getHeight());
            }
            case "polygon" -> gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight()); // fallback, если pathData пуст
            default -> throw new IllegalArgumentException("Unknown shapeType: " + shape.getShapeType());
        }
    }

    private void renderContourOrFallback(GraphicsContext gc, VectorShape shape, String fillHex) {
        PathModel pathData = shape.getPathData();
        if (pathData == null || pathData.getPoints().isEmpty()) {
            Geometry g = shape.getGeometry();
            gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());
            return;
        }
        renderContour(gc, pathData, fillHex);
    }

    /**
     * Обобщённый рендер контура PathModel: поддерживает несколько суб-контуров
     * (CompoundPath через повторяющиеся команды "M" — пример "буква О с дыркой"),
     * команды M/L/C и fillRule even-odd/non-zero. Заливка теперь берётся через
     * ColorResolver (Вопросы 5, 7) вместо хардкода — fillHex==null оставляет
     * старое поведение (полупрозрачный дефолт), заданный fillColor/swatchRef
     * даёт непрозрачную заливку сплошным цветом.
     */
    private void renderContour(GraphicsContext gc, PathModel pathData, String fillHex) {
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

        gc.setFill(fillHex != null ? Color.web(fillHex, 1.0) : Color.web("#475569", 0.6));
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

    /**
     * Editor-only визуализация Prepress-геометрии (Этап 1.8, Вопросы 6, 4).
     * Рисуется ПОСЛЕ всех объектов, чтобы служебные линии не перекрывались
     * контентом. TrimBox совпадает с границами Canvas (v0.1 не расширяет холст
     * под MediaBox — см. Вопрос 6), поэтому bleed рисуется прямо по кромке.
     */
    private void renderPrepressGuides(GraphicsContext gc, DocumentHandle document, PageModel page, MasterPage masterPage) {
        Manifest manifest = document.getManifest();
        double bleed = PrepressResolver.resolveBleedMargin(page, masterPage, manifest);
        double safety = PrepressResolver.resolveSafetyMargin(page, masterPage, manifest);
        PrintMarksSettings marks = PrepressResolver.resolvePrintMarksSettings(page, masterPage, manifest);

        double w = page.getWidth();
        double h = page.getHeight();

        gc.save();
        gc.setGlobalAlpha(1.0);

        if (bleed > 0) {
            gc.setStroke(Color.web("#F97316"));
            gc.setLineWidth(1.0);
            gc.setLineDashes(null);
            gc.strokeRect(0.5, 0.5, w - 1, h - 1);
        }

        if (safety > 0) {
            gc.setStroke(Color.web("#0EA5E9"));
            gc.setLineWidth(1.0);
            gc.setLineDashes(4.0, 3.0);
            gc.strokeRect(safety, safety, w - 2 * safety, h - 2 * safety);
            gc.setLineDashes(null);
        }

        if (marks.isShowCropMarks()) {
            renderCropMarks(gc, w, h, bleed);
        }
        if (marks.isShowRegistrationMarks()) {
            renderRegistrationMarks(gc, w, h);
        }
        if (marks.isShowColorBars()) {
            renderColorBar(gc, document, page, w, h);
        }

        gc.restore();
    }

    private void renderCropMarks(GraphicsContext gc, double w, double h, double bleed) {
        double markLength = Math.max(8.0, bleed);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.75);

        double[][] corners = {{0, 0}, {w, 0}, {0, h}, {w, h}};
        for (double[] corner : corners) {
            double cx = corner[0];
            double cy = corner[1];
            double dirX = cx == 0 ? -1 : 1;
            double dirY = cy == 0 ? -1 : 1;
            gc.strokeLine(cx, cy + dirY * 2, cx, cy + dirY * markLength);
            gc.strokeLine(cx + dirX * 2, cy, cx + dirX * markLength, cy);
        }
    }

    private void renderRegistrationMarks(GraphicsContext gc, double w, double h) {
        double r = 4.0;
        double[][] positions = {{w / 2, 0}, {w / 2, h}, {0, h / 2}, {w, h / 2}};
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.75);
        for (double[] pos : positions) {
            double cx = pos[0];
            double cy = pos[1];
            gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
            gc.strokeLine(cx - r, cy, cx + r, cy);
            gc.strokeLine(cx, cy - r, cx, cy + r);
        }
    }

    /**
     * Программная генерация шкалы цвета из реально используемых на странице
     * Swatches (Вопрос 4). Верстальщик не собирает её вручную — рендерер
     * запрашивает палитру у StylesCatalog по фактическим swatchRef объектов.
     */
    private void renderColorBar(GraphicsContext gc, DocumentHandle document, PageModel page, double w, double h) {
        Set<String> usedSwatchRefs = new LinkedHashSet<>();
        for (BdocObject object : page.getObjects()) {
            if (object instanceof VectorShape shape) {
                if (shape.getFillColorSwatchRef() != null) usedSwatchRefs.add(shape.getFillColorSwatchRef());
                if (shape.getStrokeColorSwatchRef() != null) usedSwatchRefs.add(shape.getStrokeColorSwatchRef());
            } else if (object instanceof LineObject line) {
                if (line.getStrokeColorSwatchRef() != null) usedSwatchRefs.add(line.getStrokeColorSwatchRef());
            }
        }
        if (usedSwatchRefs.isEmpty()) return;

        double chipSize = 10.0;
        double startX = 8.0;
        double y = h - chipSize - 4.0;

        gc.setLineWidth(0.5);
        gc.setStroke(Color.web("#94A3B8"));
        int i = 0;
        for (String swatchRef : usedSwatchRefs) {
            String hex = ColorResolver.resolve(null, swatchRef, document.getStyles());
            if (hex == null) continue;
            double x = startX + i * (chipSize + 2.0);
            gc.setFill(Color.web(hex));
            gc.fillRect(x, y, chipSize, chipSize);
            gc.strokeRect(x, y, chipSize, chipSize);
            i++;
        }
    }
}