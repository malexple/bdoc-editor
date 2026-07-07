package org.example.bdoc.ui;

import org.example.bdoc.io.BdocContainerSerializer;
import org.example.bdoc.io.BdocContainerSerializer.Writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.io.File;
import java.io.IOException;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import org.example.bdoc.model.*;

public final class SampleDocuments {

    private SampleDocuments() {
    }

    /**
     * Записывает демонстрационный .bdoc-документ с пятью страницами.
     * Каждая страница инкапсулирована в собственный writePageN(writer) —
     * это позволяет точечно менять содержимое одной демо-страницы без
     * риска затронуть остальные (см. историю правок Этапов 1.2–1.5).
     */
    public static void writeSample(File target) throws IOException {
        BdocContainerSerializer serializer = new BdocContainerSerializer();

        try (Writer writer = serializer.beginWrite(target)) {
            writeMasterPage(writer);

            writePage1(writer);
            writePage2(writer);
            writePage3(writer);
            writePage4(writer);
            writePage5(writer);
            writePage6(writer);
            writePage7(writer);
            writePage8(writer);

            writer.finish(
                    "doc-1", "BDoc Demo", "book",
                    "0.1-composite", "ru-RU", buildStylesCatalog(),
                    buildColorProfiles(), "icc-fogra39"
            );
        }
    }

    // ==================== Общий MasterPage ====================

    /**
     * MasterPage "master-A" — общий шаблон для всех демо-страниц:
     * рамка-декор (rounded-rectangle) и колонтитул с {page_num}.
     */
    private static void writeMasterPage(Writer writer) throws IOException {
        LayerModel masterDecorLayer = new LayerModel("layer-decor", "Decoration", "decoration", true, 1.0);
        LayerModel masterFooterLayer = new LayerModel("layer-footer", "Footer", "header-footer", true, 1.0);

        VectorShape masterFrame = new VectorShape(
                "master-frame-1", "layer-decor",
                new Geometry(40.0, 40.0, 515.0, 762.0, 24.0, 24.0),
                "rounded-rectangle"
        );

        HeaderFooterRule masterFooter = new HeaderFooterRule(
                "master-footer-1", "layer-footer",
                new Geometry(70.0, 800.0, 455.0, 20.0),
                "footer", "Страница {page_num}", "footer-text"
        );

        Guide verticalGuide = new Guide("guide-v-1", "vertical", 297.5, "#3B82F6", true);
        Guide horizontalGuide = new Guide("guide-h-1", "horizontal", 40.0, "#3B82F6", true);

        MasterPage masterA = new MasterPage(
                "master-A", "Основной шаблон",
                595.0, 842.0,
                new MarginModel(40.0, 40.0, 40.0, 40.0),
                new GridModel(true, 2, 14.17, List.of()),
                new BaselineGrid(true, 40.0, 12.0),
                List.of(verticalGuide, horizontalGuide),
                List.of(masterFrame, masterFooter)
        );
        writer.writeTemplate(masterA);
    }

    // ==================== Страница 1: одна колонка ====================

    private static void writePage1(Writer writer) throws IOException {
        Paragraph body1 = new Paragraph("body", "body-text", List.of(
                Span.plain("Это тестовый документ BDoc. Параграф "),
                new Span("заголовка", "emphasis"),
                Span.plain(" наследует шрифт и цвет от body-text через basedOn, но переопределяет размер, выравнивание и цвет.")
        ));

        StoryModel story1 = new StoryModel("story-1", List.of(
                new Paragraph("heading", "heading-1", "Демонстрация стилей BDoc"),
                body1
        ));
        writer.writeStory(story1);

        LayerModel bgLayer = new LayerModel("layer-bg", "Background", "background", true, 1.0);
        LayerModel decorLayer1 = new LayerModel("layer-decor", "Decoration", "decoration", true, 1.0);
        LayerModel textLayer1 = new LayerModel("layer-text", "Text", "text", true, 1.0);
        LayerModel footerLayer1 = new LayerModel("layer-footer", "Footer", "header-footer", true, 1.0);

        VectorShape divider1 = new VectorShape(
                "shape-2", "layer-decor",
                new Geometry(70.0, 120.0, 455.0, 0.0),
                "line"
        );

        TextFrame textFrame1 = new TextFrame(
                "text-1", "layer-text",
                new Geometry(70.0, 150.0, 455.0, 180.0),
                "story-1"
        );

        HeaderFooterRule footerOverride1 = new HeaderFooterRule(
                "master-footer-1", "layer-footer",
                new Geometry(70.0, 780.0, 455.0, 20.0),
                null, null, null,
                "master-footer-1", Set.of("geometry")
        );

        List<ReadingSegment> readingOrder1 = List.of(
                new ReadingSegment(1, "text-1", "body")
        );

        PageModel page1 = new PageModel(
                "page-1", 1, 595.0, 842.0, "pt", "master-A",
                List.of(bgLayer, decorLayer1, textLayer1, footerLayer1),
                List.of(divider1, textFrame1, footerOverride1),
                readingOrder1
        );
        writer.writePage(page1);
    }

    // ==================== Страница 2: две колонки ====================

    private static void writePage2(Writer writer) throws IOException {
        StoryModel story2Left = new StoryModel("story-2-left", List.of(
                new Paragraph("heading", "heading-1", "Левая колонка"),
                new Paragraph("body", "body-text",
                        "Это начало статьи. Без явного readingOrder экспортёр или скринридер "
                                + "прочитал бы верхнюю строку этой колонки и верхнюю строку правой "
                                + "колонки как одно предложение, разрушив смысл текста.")
        ));
        writer.writeStory(story2Left);

        StoryModel story2Right = new StoryModel("story-2-right", List.of(
                new Paragraph("heading", "heading-1", "Правая колонка"),
                new Paragraph("body", "body-text",
                        "Это продолжение статьи из левой колонки. ReadingOrder явно указывает, "
                                + "что этот фрейм читается вторым, независимо от того, как объекты "
                                + "расположены геометрически на холсте.")
        ));
        writer.writeStory(story2Right);

        LayerModel decorLayer2 = new LayerModel("layer-decor", "Decoration", "decoration", true, 1.0);
        LayerModel textLayer2 = new LayerModel("layer-text", "Text", "text", true, 1.0);
        LayerModel footerLayer2 = new LayerModel("layer-footer", "Footer", "header-footer", true, 1.0);

        VectorShape columnDivider = new VectorShape(
                "shape-col-divider", "layer-decor",
                new Geometry(297.5, 120.0, 0.0, 680.0),
                "line"
        );

        TextFrame leftColumn = new TextFrame(
                "text-2-left", "layer-text",
                new Geometry(70.0, 120.0, 210.0, 400.0),
                "story-2-left"
        );

        TextFrame rightColumn = new TextFrame(
                "text-2-right", "layer-text",
                new Geometry(315.0, 120.0, 210.0, 400.0),
                "story-2-right"
        );

        List<ReadingSegment> readingOrder2 = List.of(
                new ReadingSegment(1, "text-2-left", "body"),
                new ReadingSegment(2, "text-2-right", "body")
        );

        PageModel page2 = new PageModel(
                "page-2", 2, 595.0, 842.0, "pt", "master-A",
                List.of(decorLayer2, textLayer2, footerLayer2),
                List.of(columnDivider, leftColumn, rightColumn),
                readingOrder2
        );
        writer.writePage(page2);
    }

    // ==================== Страница 3: heading + body + pull-quote ====================

    private static void writePage3(Writer writer) throws IOException {
        StoryModel story3Heading = new StoryModel("story-3-heading", List.of(
                new Paragraph("heading", "heading-1", "Заголовок статьи")
        ));
        writer.writeStory(story3Heading);

        StoryModel story3Body = new StoryModel("story-3-body", List.of(
                new Paragraph("body", "body-text",
                        "Основной текст статьи. Врезанная цитата ниже семантически относится "
                                + "именно к этому месту повествования, поэтому её сегмент в readingOrder "
                                + "стоит сразу после этого абзаца, а не в конце страницы.")
        ));
        writer.writeStory(story3Body);

        StoryModel story3Quote = new StoryModel("story-3-quote", List.of(
                new Paragraph("quote", "pull-quote", "«Порядок чтения — это карта смысла документа.»")
        ));
        writer.writeStory(story3Quote);

        LayerModel decorLayer3 = new LayerModel("layer-decor", "Decoration", "decoration", true, 1.0);
        LayerModel textLayer3 = new LayerModel("layer-text", "Text", "text", true, 1.0);
        LayerModel footerLayer3 = new LayerModel("layer-footer", "Footer", "header-footer", true, 1.0);

        VectorShape quoteBackdrop = new VectorShape(
                "shape-quote-backdrop", "layer-decor",
                new Geometry(140.0, 400.0, 315.0, 90.0, 8.0, 8.0),
                "rounded-rectangle"
        );

        TextFrame headingFrame = new TextFrame(
                "text-3-heading", "layer-text",
                new Geometry(70.0, 120.0, 455.0, 50.0),
                "story-3-heading"
        );

        TextFrame bodyFrame = new TextFrame(
                "text-3-body", "layer-text",
                new Geometry(70.0, 180.0, 455.0, 200.0),
                "story-3-body"
        );

        TextFrame quoteFrame = new TextFrame(
                "text-3-quote", "layer-text",
                new Geometry(150.0, 410.0, 295.0, 70.0),
                "story-3-quote"
        );

        List<ReadingSegment> readingOrder3 = List.of(
                new ReadingSegment(1, "text-3-heading", "heading"),
                new ReadingSegment(2, "text-3-body", "body"),
                new ReadingSegment(3, "text-3-quote", "pull-quote")
        );

        PageModel page3 = new PageModel(
                "page-3", 3, 595.0, 842.0, "pt", "master-A",
                List.of(decorLayer3, textLayer3, footerLayer3),
                List.of(quoteBackdrop, headingFrame, bodyFrame, quoteFrame),
                readingOrder3
        );
        writer.writePage(page3);
    }

    // ==================== Страница 4: Group, Table, маска, примитивы ====================

    private static void writePage4(Writer writer) throws IOException {
        StoryModel storyCaption = new StoryModel("story-caption", List.of(
                new Paragraph("caption", "footer-text", "Рис. 1 — декоративная иконка с подписью")
        ));
        writer.writeStory(storyCaption);

        StoryModel storyCell00 = new StoryModel("story-cell-0-0", List.of(
                new Paragraph("cell", "body-text", "Товар")
        ));
        writer.writeStory(storyCell00);

        StoryModel storyCell01 = new StoryModel("story-cell-0-1", List.of(
                new Paragraph("cell", "body-text", "Цена")
        ));
        writer.writeStory(storyCell01);

        StoryModel storyCell10 = new StoryModel("story-cell-1-0", List.of(
                new Paragraph("cell", "body-text", "Букварь, 1913")
        ));
        writer.writeStory(storyCell10);

        StoryModel storyCell11 = new StoryModel("story-cell-1-1", List.of(
                new Paragraph("cell", "body-text", "1200 руб.")
        ));
        writer.writeStory(storyCell11);

        LayerModel decorLayer4 = new LayerModel("layer-decor", "Decoration", "decoration", true, 1.0);
        LayerModel textLayer4 = new LayerModel("layer-text", "Text", "text", true, 1.0);
        LayerModel footerLayer4 = new LayerModel("layer-footer", "Footer", "header-footer", true, 1.0);

        TextFrame headingFrame4 = new TextFrame(
                "text-4-heading", "layer-text",
                new Geometry(70.0, 120.0, 455.0, 50.0),
                "story-3-heading"
        );

        LineObject divider4 = new LineObject(
                "line-divider-4", "layer-decor",
                70.0, 170.0, 525.0, 170.0,
                1.5, "#64748B", "double",
                "none", "arrow"
        );

        VectorShape iconDecor = new VectorShape(
                "icon-decor-1", "layer-decor",
                new Geometry(70.0, 200.0, 60.0, 60.0, 8.0, 8.0),
                "rounded-rectangle"
        );

        TextFrame captionFrame = new TextFrame(
                "text-caption-1", "layer-text",
                new Geometry(140.0, 210.0, 200.0, 40.0),
                "story-caption"
        );

        Group cardGroup = new Group(
                "group-card-1", "layer-text",
                new Geometry(70.0, 200.0, 270.0, 60.0),
                List.of("icon-decor-1", "text-caption-1")
        );

        VectorShape maskStar = new VectorShape(
                "mask-star-1", "layer-decor",
                new Geometry(360.0, 200.0, 80.0, 80.0),
                "rectangle",
                null, null, true,
                null, null, true, false, null, null,
                new PathModel("bezier", List.of(
                        PathPoint.moveTo(400.0, 200.0),
                        PathPoint.lineTo(420.0, 240.0),
                        PathPoint.cubicTo(430.0, 250.0, 440.0, 250.0, 440.0, 240.0)
                )),
                null
        );

        VectorShape maskedShape = new VectorShape(
                "shape-masked-icon", "layer-decor",
                new Geometry(360.0, 200.0, 80.0, 80.0),
                "rectangle",
                null, null, true,
                null, "mask-star-1", false, false, null, null, null,
                null
        );

        TableFrame priceTable = new TableFrame(
                "table-info-1", "layer-text",
                new Geometry(70.0, 300.0, 400.0, 100.0),
                2, 2,
                List.of(new TableRow(1.0), new TableRow(1.0)),
                List.of(new TableColumn(1.0), new TableColumn(2.0)),
                List.of(
                        new TableCell(0, 0, "story-cell-0-0"),
                        new TableCell(0, 1, "story-cell-0-1"),
                        new TableCell(1, 0, "story-cell-1-0"),
                        new TableCell(1, 1, "story-cell-1-1")
                )
        );

        HeaderFooterRule footerArtifact4 = new HeaderFooterRule(
                "master-footer-1", "layer-footer",
                new Geometry(70.0, 780.0, 455.0, 20.0),
                null, null, null,
                "master-footer-1", Set.of("geometry"), true,
                null, null, false, true, "pagination", null, null,
                null
        );

        List<ReadingSegment> readingOrder4 = List.of(
                new ReadingSegment(1, "text-4-heading", "heading"),
                new ReadingSegment(2, "text-caption-1", "caption"),
                new ReadingSegment(3, "table-info-1", "table")
        );

        VectorShape ellipseDemo = new VectorShape(
                "shape-ellipse-1", "layer-decor",
                new Geometry(70.0, 480.0, 160.0, 100.0),
                "ellipse"
        );

        VectorShape rotatedRectDemo = new VectorShape(
                "shape-rotated-1", "layer-decor",
                new Geometry(280.0, 480.0, 120.0, 80.0),
                "rectangle",
                null, null, true,
                null, null, false, false, null, null, null,
                new TransformModel(0.0, 0.0, 30.0, 1.0, 1.0)
        );

        VectorShape polygonDemo = new VectorShape(
                "shape-polygon-1", "layer-decor",
                new Geometry(413.0, 510.0, 94.0, 90.0),
                "polygon",
                null, null, true,
                null, null, false, false, null, null,
                new PathModel("polygon", List.of(
                        PathPoint.moveTo(460.0, 510.0),
                        PathPoint.lineTo(507.0, 545.0),
                        PathPoint.lineTo(489.0, 600.0),
                        PathPoint.lineTo(431.0, 600.0),
                        PathPoint.lineTo(413.0, 545.0)
                )),
                null
        );

        VectorShape letterODemo = new VectorShape(
                "shape-letter-o-1", "layer-decor",
                new Geometry(220.0, 620.0, 100.0, 100.0),
                "polygon",
                null, null, true,
                null, null, false, false, null, null,
                new PathModel("compound", List.of(
                        PathPoint.moveTo(270.0, 620.0),
                        PathPoint.lineTo(305.4, 634.6),
                        PathPoint.lineTo(320.0, 670.0),
                        PathPoint.lineTo(305.4, 705.4),
                        PathPoint.lineTo(270.0, 720.0),
                        PathPoint.lineTo(234.6, 705.4),
                        PathPoint.lineTo(220.0, 670.0),
                        PathPoint.lineTo(234.6, 634.6),
                        PathPoint.moveTo(270.0, 645.0),
                        PathPoint.lineTo(287.7, 652.3),
                        PathPoint.lineTo(295.0, 670.0),
                        PathPoint.lineTo(287.7, 687.7),
                        PathPoint.lineTo(270.0, 695.0),
                        PathPoint.lineTo(252.3, 687.7),
                        PathPoint.lineTo(245.0, 670.0),
                        PathPoint.lineTo(252.3, 652.3)
                ), "even-odd"),
                null
        );

        PageModel page4 = new PageModel(
                "page-4", 4, 595.0, 842.0, "pt", "master-A",
                List.of(decorLayer4, textLayer4, footerLayer4),
                List.of(headingFrame4, divider4, iconDecor, captionFrame, cardGroup,
                        maskStar, maskedShape, priceTable,
                        ellipseDemo, rotatedRectDemo, polygonDemo, letterODemo,
                        footerArtifact4),
                readingOrder4
        );
        writer.writePage(page4);
    }

    // ==================== Страница 5: Footnote, Reference, Threading (Этап 1.5) ====================

    /**
     * Демонстрирует три из пяти расширений Этапа 1.5:
     *  - Footnote: надстрочный маркер "¹" в потоке текста, полный текст сноски
     *    хранится в модели и виден только в Properties Pane (см. решение по п.3).
     *  - Reference: слово-ссылка с targetType="url" на внешний адрес.
     *  - Story threading: text-5-a.nextFrameRef -> text-5-b, симметрично
     *    text-5-b.previousFrameRef -> text-5-a. Оба фрейма ссылаются на ОДНУ
     *    и ту же story-5 и рендерятся от начала истории (упрощённая MVP-модель
     *    без реального перетекания — см. договорённость по п.7).
     *  InlineObject (картинка в тексте) сознательно НЕ показан здесь: требует
     *  метод записи бинарного ресурса в архив, отсутствующий в текущем API
     *  Writer — добавить после уточнения BdocContainerSerializer.
     */
    private static void writePage5(Writer writer) throws IOException {
        writer.writeResource("resources/icons/dropcap-A.png", generateDropCapIcon());

        StoryModel story5 = new StoryModel("story-5", List.of(
                new Paragraph("heading", "heading-1", "Текстовые расширения (Этап 1.5)"),
                new Paragraph("body", "body-text", List.of(
                        new Span("", null, "resources/icons/dropcap-A.png", 24.0, 24.0, null, null),
                        Span.plain(" абзаце показана сноска"),
                        new Span("¹", "footnote-marker", null, null, null,
                                new FootnoteModel("1",
                                        "Пояснение к тексту, набранное во времена дореволюционной орфографии."),
                                null),
                        Span.plain(" и перекрёстная ссылка на "),
                        new Span("статью в интернете", "reference-link", null, null, null,
                                null, new ReferenceModel("url", "https://ru.wikipedia.org/wiki/BDoc")),
                        Span.plain(", а сам текст истории продолжается во втором связанном фрейме через nextFrameRef.")
                ))
        ));
        writer.writeStory(story5);

        LayerModel decorLayer5 = new LayerModel("layer-decor", "Decoration", "decoration", true, 1.0);
        LayerModel textLayer5 = new LayerModel("layer-text", "Text", "text", true, 1.0);
        LayerModel footerLayer5 = new LayerModel("layer-footer", "Footer", "header-footer", true, 1.0);

        VectorShape threadDivider = new VectorShape(
                "shape-thread-divider-5", "layer-decor",
                new Geometry(297.5, 190.0, 0.0, 300.0),
                "line"
        );

        // Первый фрейм цепочки: nextFrameRef указывает на "text-5-b".
        TextFrame textFrame5A = new TextFrame(
                "text-5-a", "layer-text",
                new Geometry(70.0, 190.0, 210.0, 300.0),
                "story-5",
                null, null, true,
                null, null, false, false, null, null, null, null,
                "text-5-b", null
        );

        // Второй фрейм цепочки: previousFrameRef указывает обратно на "text-5-a".
        TextFrame textFrame5B = new TextFrame(
                "text-5-b", "layer-text",
                new Geometry(315.0, 190.0, 210.0, 300.0),
                "story-5",
                null, null, true,
                null, null, false, false, null, null, null, null,
                null, "text-5-a"
        );

        List<ReadingSegment> readingOrder5 = List.of(
                new ReadingSegment(1, "text-5-a", "body"),
                new ReadingSegment(2, "text-5-b", "body")
        );

        PageModel page5 = new PageModel(
                "page-5", 5, 595.0, 842.0, "pt", "master-A",
                List.of(decorLayer5, textLayer5, footerLayer5),
                List.of(threadDivider, textFrame5A, textFrame5B),
                readingOrder5
        );
        writer.writePage(page5);
    }

    // ==================== Стили документа ====================

    private static StylesCatalog buildStylesCatalog() {
        ParagraphStyle bodyStyle = new ParagraphStyle(
                "body-text", null, "Georgia", 16.0, 1.4, "left", "#1E293B"
        );
        ParagraphStyle headingStyle = new ParagraphStyle(
                "heading-1", "body-text", "Georgia", 26.0, 1.2, "center", "#0F172A"
        );
        ParagraphStyle footerStyle = new ParagraphStyle(
                "footer-text", null, "Georgia", 10.0, 1.0, "center", "#64748B"
        );
        ParagraphStyle quoteStyle = new ParagraphStyle(
                "pull-quote", "body-text", "Georgia", 20.0, 1.3, "center", "#B45309"
        );

        CharacterStyle boldEmphasis = new CharacterStyle(
                "emphasis", null, null, null, true, false, "#B91C1C"
        );
        CharacterStyle footnoteMarker = new CharacterStyle(
                "footnote-marker", null, null, 11.0, false, false, "#0F172A"
        );
        CharacterStyle referenceLink = new CharacterStyle(
                "reference-link", null, null, null, false, false, "#2563EB"
        );

        return new StylesCatalog(
                List.of(bodyStyle, headingStyle, footerStyle, quoteStyle),
                List.of(boldEmphasis, footnoteMarker, referenceLink),
                buildObjectStyles(),
                buildSwatches()
        );
    }

    private static HeaderFooterRule buildFooterOverride() {
        return new HeaderFooterRule(
                "master-footer-1", "layer-footer",
                new Geometry(70.0, 780.0, 455.0, 20.0),
                null, null, null,
                "master-footer-1",
                Set.of("geometry")
        );
    }

    /**
     * Генерирует минимальную демо-иконку буквицы "А" 24x24px в памяти,
     * без внешних файлов-зависимостей — специально для демонстрации
     * InlineObject (Span.inlineAssetRef) в сэмпле. В реальном документе
     * сюда попадали бы настоящие ассеты из resources/.
     */
    private static byte[] generateDropCapIcon() throws IOException {
        Canvas canvas = new Canvas(24, 24);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.rgb(0x47, 0x55, 0x69));
        gc.fillRoundRect(0, 0, 24, 24, 4, 4);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
        gc.fillText("A", 7, 17);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage fxImage = canvas.snapshot(params, null);

        BufferedImage awtImage = SwingFXUtils.fromFXImage(fxImage, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(awtImage, "png", baos);
        return baos.toByteArray();
    }

    private static List<ObjectStyle> buildObjectStyles() {
        ObjectStyle cardStyleBase = new ObjectStyle(
                "card-style-base", null, "layer-decor",
                null, 16.0, 16.0, null
        );
        ObjectStyle cardStyleHighlighted = new ObjectStyle(
                "card-style-highlighted", "card-style-base", null,
                0.6, null, null, null
        );
        return List.of(cardStyleBase, cardStyleHighlighted);
    }

    // ==================== Страница 6: ObjectStyle, каскад, AnchoredObjectSettings (Этап 1.6) ====================

    // ==================== Страница 6: ObjectStyle, каскад, AnchoredObjectSettings (Этап 1.6) ====================

    /**
     * Демонстрирует все четыре аспекта Этапа 1.6:
     *  1. ObjectStyle с basedOn: "card-style-highlighted" наследует
     *     arcWidth/arcHeight от "card-style-base", переопределяя только opacity.
     *  2. Фрейм БЕЗ локальных полей — целиком берёт arcWidth/arcHeight/opacity из стиля.
     *  3. Фрейм с локальным Geometry.arcWidth=0.0 — острый угол, несмотря на objectStyleRef
     *     (локальное значение перебивает каскад стиля).
     *  4. AnchoredObjectSettings — декоративная иконка привязана к story-5 (span[2],
     *     т.е. footnote-маркер), positionMode="custom", смещение offsetX/offsetY пока
     *     не участвует в рендере (математика anchored objects отложена на Этап 2).
     */
    private static void writePage6(Writer writer) throws IOException {
        StoryModel story6 = new StoryModel("story-6", List.of(
                new Paragraph("heading", "heading-1", "ObjectStyle и AnchoredObjectSettings (Этап 1.6)"),
                new Paragraph("body", "body-text",
                        "1) card-style-highlighted наследует arcWidth/arcHeight от card-style-base через basedOn. "
                                + "2) Левая карточка целиком берёт вид из стиля. "
                                + "3) Правая карточка переопределяет arcWidth=0 локально, игнорируя стиль. "
                                + "4) Иконка снизу привязана к story-5 через AnchoredObjectSettings.")
        ));
        writer.writeStory(story6);

        LayerModel decorLayer6 = new LayerModel("layer-decor", "Decoration", "decoration", true, 1.0);
        LayerModel textLayer6 = new LayerModel("layer-text", "Text", "text", true, 1.0);
        LayerModel footerLayer6 = new LayerModel("layer-footer", "Footer", "header-footer", true, 1.0);

        // Пункты 1-2: фрейм полностью наследует вид из "card-style-highlighted"
        // (arcWidth/arcHeight из card-style-base через basedOn, opacity=0.6 своя).
        VectorShape styledCardInherited = new VectorShape(
                "shape-styled-inherited", "layer-decor",
                new Geometry(70.0, 120.0, 200.0, 120.0),
                "rounded-rectangle",
                null, null, true,
                null, null, false, false, null,
                null, null, null,
                "card-style-highlighted", null, null
        );

        // Пункт 3: тот же objectStyleRef, но локальный Geometry.arcWidth=0.0
        // явно перебивает каскад стиля — угол острый, несмотря на стиль.
        VectorShape styledCardOverride = new VectorShape(
                "shape-styled-override", "layer-decor",
                new Geometry(325.0, 120.0, 200.0, 120.0, 0.0, 0.0),
                "rectangle",
                null, null, true,
                null, null, false, false, null,
                null, null, null,
                "card-style-highlighted", null, null
        );

        // Пункт 4: декоративная иконка, привязанная к story-5 (span с индексом 2 —
        // footnote-маркер "¹"), positionMode="custom", offsetX/Y хранятся, но пока
        // не влияют на рендер (реальная математика — Этап 2).
        AnchoredObjectSettings anchorSettings = new AnchoredObjectSettings(
                true, "story-5", 2, "custom", 15.0, -10.0
        );

        VectorShape anchoredIcon = new VectorShape(
                "shape-anchored-icon", "layer-decor",
                new Geometry(240.0, 280.0, 40.0, 40.0, 6.0, 6.0),
                "rounded-rectangle",
                null, null, true,
                null, null, false, false, null,
                null, null, null,
                null, null, anchorSettings
        );

        TextFrame captionFrame6 = new TextFrame(
                "text-6-caption", "layer-text",
                new Geometry(70.0, 350.0, 455.0, 150.0),
                "story-6"
        );

        List<ReadingSegment> readingOrder6 = List.of(
                new ReadingSegment(1, "text-6-caption", "body")
        );

        PageModel page6 = new PageModel(
                "page-6", 6, 595.0, 842.0, "pt", "master-A",
                List.of(decorLayer6, textLayer6, footerLayer6),
                List.of(styledCardInherited, styledCardOverride, anchoredIcon, captionFrame6),
                readingOrder6
        );
        writer.writePage(page6);
    }



    /**
     * Каталог образцов для демонстрации ColorResolver (Вопросы 1, 2, 4, 7):
     *  - swatch-blue-tint: чистый RGB, без каскада CMYK.
     *  - swatch-brand-navy: CMYK С заданным fallbackRgb — резолвится мгновенно,
     *    формула не считается (fallbackRgb имеет наивысший приоритет).
     *  - swatch-brand-red-approx: CMYK БЕЗ fallbackRgb — резолвится черновой
     *    формулой R=255*(1-c)*(1-k) на лету.
     *  - swatch-pantone-gold-nofallback: Spot без fallbackRgb — сигнализирует
     *    едкой MAGENTA, что плашечный цвет требует калибровки превью.
     *  - swatch-pantone-red-calibrated: Spot С fallbackRgb — калиброванный превью.
     *  - swatch-archive-lab: Lab — не поддерживается на лету, дефолтный чёрный.
     */
    private static List<Swatch> buildSwatches() {
        return List.of(
                Swatch.rgb("swatch-blue-tint", "Синий акцент (RGB)", "#3B82F6"),
                Swatch.cmyk("swatch-brand-navy", "Тёмно-синий (CMYK, с фолбеком)",
                        85.0, 60.0, 0.0, 40.0, "#1E3A5F"),
                Swatch.cmyk("swatch-brand-red-approx", "Бренд-красный (расчётный CMYK)",
                        10.0, 90.0, 80.0, 5.0, null),
                Swatch.spot("swatch-pantone-gold-nofallback", "Pantone 872 C (без фолбека)", null),
                Swatch.spot("swatch-pantone-red-calibrated", "Pantone 186 C (калиброван)", "#C8102E"),
                new Swatch("swatch-archive-lab", "Архивный Lab-образец", "Lab", null, null, null, null, null),
                Swatch.cmyk("swatch-cyan-cmyk-test", "Полиграфический циан (без фолбека)",
                        100.0, 10.0, 0.0, 10.0, null)
        );
    }

    /**
     * Каталог ICC-профилей документа (Вопросы 3, 6). icc-fogra39 назначен
     * глобальным outputIntentProfileRef всей книги, icc-scanner-old привязан
     * индивидуально к одному ImageFrame на page7 (архивный скан).
     */
    private static List<ColorProfile> buildColorProfiles() {
        return List.of(
                new ColorProfile("icc-fogra39", "Coated FOGRA39",
                        "CMYK", "Европейский стандарт офсетной печати (Output Intent книги)"),
                new ColorProfile("icc-scanner-old", "Legacy Scanner RGB",
                        "RGB", "Специфическое цветовое пространство старого сканера архива")
        );
    }
    // ==================== Страница 7: Swatches, ColorProfile, ColorResolver (Этап 1.7) ====================

    /**
     * Демонстрирует все аспекты Этапа 1.7:
     *  1. Пять цветных карточек VectorShape, каждая раскрашена через отдельный
     *     Swatch — визуально видно разницу между RGB, CMYK с фолбеком, CMYK без
     *     фолбека (расчёт формулой), Spot без фолбека (едкая магента) и Lab (чёрный).
     *  2. Шестая карточка raw HEX без swatchRef — подтверждает обратную
     *     совместимость Smart Fallback (Вопрос 4).
     *  3. Составная буква «О» с fillRule=even-odd теперь получает реальную
     *     заливку через fillColorSwatchRef (было невозможно до Вопроса 5).
     *  4. LineObject окрашен через strokeColorSwatchRef на калиброванный Pantone.
     *  5. ImageFrame ссылается на индивидуальный profileRef (icc-scanner-old),
     *     отдельный от глобального outputIntentProfileRef книги (icc-fogra39).
     */
    private static void writePage7(Writer writer) throws IOException {
        writer.writeResource("resources/scans/archive-page-scan.png", generateDropCapIcon());

        StoryModel story7 = new StoryModel("story-7", List.of(
                new Paragraph("heading", "heading-1", "Палитра образцов (Этап 1.7)"),
                new Paragraph("body", "body-text",
                        "Шесть карточек сверху показывают разные пути ColorResolver: RGB, CMYK с фолбеком, "
                                + "CMYK без фолбека (формула на лету), Spot без калибровки (магента-предупреждение), "
                                + "Lab (дефолтный чёрный) и обычный HEX без ссылки на образец. Буква «О» ниже залита "
                                + "через swatchRef с учётом fillRule=even-odd.")
        ));
        writer.writeStory(story7);

        LayerModel decorLayer7 = new LayerModel("layer-decor", "Decoration", "decoration", true, 1.0);
        LayerModel textLayer7 = new LayerModel("layer-text", "Text", "text", true, 1.0);
        LayerModel footerLayer7 = new LayerModel("layer-footer", "Footer", "header-footer", true, 1.0);

        TextFrame headingFrame7 = new TextFrame(
                "text-7-heading", "layer-text",
                new Geometry(70.0, 120.0, 455.0, 50.0),
                "story-7"
        );

        double cardY = 190.0;
        double cardW = 70.0;
        double cardH = 70.0;
        double gap = 15.0;

        VectorShape cardRgb = new VectorShape(
                "shape-card-rgb", "layer-decor",
                new Geometry(70.0, cardY, cardW, cardH, 8.0, 8.0),
                "rounded-rectangle",
                null, "#0F172A", "swatch-blue-tint", null
        );

        VectorShape cardCmykFallback = new VectorShape(
                "shape-card-cmyk-fallback", "layer-decor",
                new Geometry(70.0 + (cardW + gap), cardY, cardW, cardH, 8.0, 8.0),
                "rounded-rectangle",
                null, "#0F172A", "swatch-brand-navy", null
        );

        VectorShape cardCmykFormula = new VectorShape(
                "shape-card-cmyk-formula", "layer-decor",
                new Geometry(70.0 + (cardW + gap) * 2, cardY, cardW, cardH, 8.0, 8.0),
                "rounded-rectangle",
                null, "#0F172A", "swatch-brand-red-approx", null
        );

        VectorShape cardSpotNoFallback = new VectorShape(
                "shape-card-spot-nofallback", "layer-decor",
                new Geometry(70.0 + (cardW + gap) * 3, cardY, cardW, cardH, 8.0, 8.0),
                "rounded-rectangle",
                null, "#0F172A", "swatch-pantone-gold-nofallback", null
        );

        VectorShape cardLab = new VectorShape(
                "shape-card-lab", "layer-decor",
                new Geometry(70.0 + (cardW + gap) * 4, cardY, cardW, cardH, 8.0, 8.0),
                "rounded-rectangle",
                null, "#0F172A", "swatch-archive-lab", null
        );

        // Обратная совместимость (Вопрос 4): чистый HEX без swatchRef продолжает работать.
        VectorShape cardRawHex = new VectorShape(
                "shape-card-raw-hex", "layer-decor",
                new Geometry(70.0 + (cardW + gap) * 5, cardY, cardW, cardH, 8.0, 8.0),
                "rounded-rectangle",
                "#F97316", "#0F172A", null, null
        );

        // Составная буква «О» (fillRule=even-odd) — теперь с реальной заливкой
        // через fillColorSwatchRef, чего не хватало до Вопроса 5.
        VectorShape letterOFilled = new VectorShape(
                "shape-letter-o-filled", "layer-decor",
                new Geometry(220.0, 300.0, 100.0, 100.0),
                "polygon",
                null, null, true,
                null, null, false, false, null, null,
                new PathModel("compound", List.of(
                        PathPoint.moveTo(270.0, 300.0),
                        PathPoint.lineTo(305.4, 314.6),
                        PathPoint.lineTo(320.0, 350.0),
                        PathPoint.lineTo(305.4, 385.4),
                        PathPoint.lineTo(270.0, 400.0),
                        PathPoint.lineTo(234.6, 385.4),
                        PathPoint.lineTo(220.0, 350.0),
                        PathPoint.lineTo(234.6, 314.6),
                        PathPoint.moveTo(270.0, 325.0),
                        PathPoint.lineTo(287.7, 332.3),
                        PathPoint.lineTo(295.0, 350.0),
                        PathPoint.lineTo(287.7, 367.7),
                        PathPoint.lineTo(270.0, 375.0),
                        PathPoint.lineTo(252.3, 367.7),
                        PathPoint.lineTo(245.0, 350.0),
                        PathPoint.lineTo(252.3, 332.3)
                ), "even-odd"),
                null,
                null, "#0F172A", "swatch-brand-navy", null,
                null, null
        );

        // LineObject окрашен через strokeColorSwatchRef на калиброванный Pantone.
        LineObject dividerCalibrated = new LineObject(
                "line-divider-7", "layer-decor",
                70.0, 430.0, 525.0, 430.0,
                2.0, "#000000", "solid",
                "none", "none",
                null, null, null,
                "swatch-pantone-red-calibrated"
        );

        // ImageFrame с индивидуальным profileRef, отдельным от outputIntentProfileRef книги.
        ImageFrame archiveScan = new ImageFrame(
                "image-7-archive-scan", "layer-decor",
                new Geometry(70.0, 460.0, 100.0, 100.0),
                "resources/scans/archive-page-scan.png",
                null, null, true,
                null, null, false, false, null, null, null, null,
                "icc-scanner-old"
        );

        TextFrame captionFrame7 = new TextFrame(
                "text-7-caption", "layer-text",
                new Geometry(190.0, 460.0, 335.0, 100.0),
                "story-7"
        );

        List<ReadingSegment> readingOrder7 = List.of(
                new ReadingSegment(1, "text-7-heading", "heading"),
                new ReadingSegment(2, "text-7-caption", "body")
        );

        PageModel page7 = new PageModel(
                "page-7", 7, 595.0, 842.0, "pt", "master-A",
                List.of(decorLayer7, textLayer7, footerLayer7),
                List.of(headingFrame7,
                        cardRgb, cardCmykFallback, cardCmykFormula, cardSpotNoFallback, cardLab, cardRawHex,
                        letterOFilled, dividerCalibrated, archiveScan, captionFrame7),
                readingOrder7
        );
        writer.writePage(page7);
    }

    /**
     * Генерирует compound-контур "восьмиугольник с восьмиугольной дыркой"
     * (аналог демо-буквы «О» из Этапа 1.7, но с прямыми гранями). Внутреннее
     * кольцо вычитается благодаря fillRule=even-odd — направление обхода
     * колец для even-odd не важно, важна только чётность пересечений луча.
     */
    private static PathModel buildOctagonWithHole(double cx, double cy, double outerRadius, double innerRadius) {
        List<PathPoint> points = new ArrayList<>();
        points.addAll(buildOctagonRing(cx, cy, outerRadius));
        points.addAll(buildOctagonRing(cx, cy, innerRadius));
        return new PathModel("compound", points, "even-odd");
    }

    private static List<PathPoint> buildOctagonRing(double cx, double cy, double radius) {
        List<PathPoint> ring = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(22.5 + i * 45.0);
            double x = cx + radius * Math.cos(angle);
            double y = cy + radius * Math.sin(angle);
            ring.add(i == 0 ? PathPoint.moveTo(x, y) : PathPoint.lineTo(x, y));
        }
        return ring;
    }

    // ==================== Страница 8: Prepress-геометрия (Этап 1.8) ====================

    /**
     * Демонстрирует все аспекты Этапа 1.8:
     *  1. bleedMargin=3мм и safetyMargin=5мм заданы прямо на PageModel —
     *     PageRenderer.renderPrepressGuides() нарисует оранжевую линию по
     *     кромке холста и голубой пунктирный прямоугольник внутри.
     *  2. shape-cmyk-test — восьмиугольник с дыркой, залитый через
     *     swatch-cyan-cmyk-test (CMYK без fallbackRgb) — цвет считается
     *     формулой на лету, центр фигуры остаётся прозрачным (even-odd).
     *  3. TextFrame стоит строго внутри safetyMargin — валидатор пропускает
     *     страницу без ошибок. Сценарий нарушения — см. Javadoc метода
     *     buildSafetyViolationSample() ниже.
     */
    private static void writePage8(Writer writer) throws IOException {
        StoryModel story8 = new StoryModel("story-8", List.of(
                new Paragraph("heading", "heading-1", "Prepress-геометрия (Этап 1.8)"),
                new Paragraph("body", "body-text",
                        "Оранжевая линия по периметру — вылет под обрез (bleedMargin = 3 мм). "
                                + "Голубой пунктир — безопасная зона (safetyMargin = 5 мм). Восьмиугольник в центре "
                                + "залит по формуле CMYK на лету (Swatch без fallbackRgb), а его внутренняя дыра "
                                + "прозрачна благодаря fillRule=even-odd.")
        ));
        writer.writeStory(story8);

        LayerModel decorLayer8 = new LayerModel("layer-decor", "Decoration", "decoration", true, 1.0);
        LayerModel textLayer8 = new LayerModel("layer-text", "Text", "text", true, 1.0);
        LayerModel footerLayer8 = new LayerModel("layer-footer", "Footer", "header-footer", true, 1.0);

        TextFrame headingFrame8 = new TextFrame(
                "text-8-heading", "layer-text",
                new Geometry(70.0, 120.0, 455.0, 50.0),
                "story-8"
        );

        double pageWidth = 595.0;
        double pageHeight = 842.0;
        double centerX = pageWidth / 2.0;
        double centerY = pageHeight / 2.0;
        double outerRadius = 90.0;
        double innerRadius = 45.0;

        VectorShape shapeCmykTest = new VectorShape(
                "shape-cmyk-test", "layer-decor",
                new Geometry(centerX - outerRadius, centerY - outerRadius, outerRadius * 2, outerRadius * 2),
                "polygon",
                null, null, true,
                null, null, false, false, null, null,
                buildOctagonWithHole(centerX, centerY, outerRadius, innerRadius),
                null,
                null, "#0F172A", "swatch-cyan-cmyk-test", null,
                null, null
        );

        double safety = Unit.MM.toPoints(5.0);

        // TextFrame стоит строго внутри safetyMargin (запас 20pt от границы),
        // поэтому Preflight-валидатор пропускает страницу без ошибок.
        TextFrame captionFrame8 = new TextFrame(
                "text-8-caption", "layer-text",
                new Geometry(safety + 20.0, pageHeight - 160.0, pageWidth - (safety + 20.0) * 2, 100.0),
                "story-8"
        );

        List<ReadingSegment> readingOrder8 = List.of(
                new ReadingSegment(1, "text-8-heading", "heading"),
                new ReadingSegment(2, "text-8-caption", "body")
        );

        PageModel page8 = new PageModel(
                "page-8", 8, pageWidth, pageHeight, "pt", "master-A",
                List.of(decorLayer8, textLayer8, footerLayer8),
                List.of(headingFrame8, shapeCmykTest, captionFrame8),
                readingOrder8,
                Unit.MM.toPoints(3.0),
                safety,
                null
        );
        writer.writePage(page8);
    }

    /**
     * ДЕМОНСТРАЦИЯ ВАЛИДАЦИИ (не вызывается из writeSample() автоматически,
     * так как намеренно ломает Preflight и должен использоваться только
     * в JUnit-тесте на отрицательный сценарий, например:
     * assertThrows(BdocValidationException.class, () -> validator.validate(doc)))
     *
     * Возвращает PageModel, где TextFrame сдвинут на x=5.0 — глубоко внутрь
     * safetyMargin=5мм (14.17pt) от левого края. BdocIntegrityValidator
     * должен выбросить ошибку "Text content is too close to the page trim edge!".
     */
    public static PageModel buildSafetyViolationSample() {
        LayerModel textLayer = new LayerModel("layer-text", "Text", "text", true, 1.0);

        TextFrame violatingFrame = new TextFrame(
                "text-violation", "layer-text",
                new Geometry(5.0, 120.0, 300.0, 50.0), // x=5.0 залезает под safetyMargin=14.17pt
                "story-8"
        );

        return new PageModel(
                "page-violation", 1, 595.0, 842.0, "pt", null,
                List.of(textLayer),
                List.of(violatingFrame),
                List.of(),
                Unit.MM.toPoints(3.0),
                Unit.MM.toPoints(5.0),
                null
        );
    }
}