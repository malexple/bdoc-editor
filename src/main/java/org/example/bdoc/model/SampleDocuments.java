package org.example.bdoc.model;

import org.example.bdoc.io.BdocContainerSerializer;

import java.util.List;
import java.util.Set;
import java.io.File;
import java.io.IOException;

public final class SampleDocuments {

    private SampleDocuments() {
    }

    /**
     * Записывает демонстрационный .bdoc-документ с тремя страницами.
     *
     * Каждая страница показывает свой аспект ReadingOrder (Этап 1.2):
     *  - Страница 1: одна колонка — простейший readingOrder из одного сегмента.
     *  - Страница 2: две колонки текста — наглядно показывает, почему без
     *    ReadingOrder скринридер/скрипт-экспортёр читал бы контент вперемешку
     *    (геометрически верхние строки обеих колонок идут "в одну строку").
     *  - Страница 3: heading + body + pull-quote (разные роли сегментов) и
     *    декоративный элемент, сознательно НЕ включённый в readingOrder —
     *    демонстрация выборочного охвата (декор остаётся вне потока чтения).
     *
     * Все три страницы наследуют общий MasterPage "master-A" — колонтитул
     * с {page_num} автоматически показывает верный номер на каждой странице.
     */
    public static void writeSample(File target) throws IOException {
        BdocContainerSerializer serializer = new BdocContainerSerializer();

        try (BdocContainerSerializer.Writer writer = serializer.beginWrite(target)) {
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

            // ---------- Страница 1: одна колонка ----------

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

            // ---------- Страница 2: две колонки (левая/правая) ----------

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

            // ---------- Страница 3: heading + body + pull-quote + декор ----------

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

            // ---------- MasterPage (общий для всех трёх страниц) ----------

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

            // ---------- Страница 1: содержимое ----------

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

            // ---------- Страница 2: две колонки ----------

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

            // Явный порядок: сначала левая колонка (sequence 1), затем правая (sequence 2).
            // Геометрический алгоритм "сверху-вниз, слева-направо" здесь совпал бы с
            // намерением автора, но на практике многоколоночная верстка часто требует
            // ручной коррекции — именно поэтому порядок хранится явно, а не вычисляется.
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

            // ---------- Страница 3: heading + body + pull-quote + декор без readingOrder ----------

            LayerModel decorLayer3 = new LayerModel("layer-decor", "Decoration", "decoration", true, 1.0);
            LayerModel textLayer3 = new LayerModel("layer-text", "Text", "text", true, 1.0);
            LayerModel footerLayer3 = new LayerModel("layer-footer", "Footer", "header-footer", true, 1.0);

            // Декоративная плашка-подложка под цитатой — участвует в рендеринге,
            // но сознательно НЕ добавлена в readingOrder (правило выборочного охвата:
            // чистый декор игнорируется скринридерами/скриптами очистки).
            VectorShape quoteBackdrop = new VectorShape(
                    "shape-quote-backdrop", "layer-decor",
                    new Geometry(140.0, 400.0, 315.0, 90.0, 8.0, 8.0),
                    "rounded-rectangle"
            );

            TextFrame headingFrame = new TextFrame(
                    "text-3-heading", "layer-text",
                    new Geometry(70.0, 120.0, 455.0, 50.0),  // было 40.0
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

            // ---------- Страница 4: Group, TableFrame, LineObject, маска, артефакт ----------

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

            // Заголовок страницы
            TextFrame headingFrame4 = new TextFrame(
                    "text-4-heading", "layer-text",
                    new Geometry(70.0, 120.0, 455.0, 50.0),  // было 40.0
                    "story-3-heading"
            );

            // LineObject: наклонная декоративная линейка с двойной обводкой и стрелкой на конце
            LineObject divider4 = new LineObject(
                    "line-divider-4", "layer-decor",
                    70.0, 170.0, 525.0, 170.0,
                    1.5, "#64748B", "double",
                    "none", "arrow"
            );

            // Group: иконка (VectorShape) + подпись (TextFrame), объединённые в карточку
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

            // Маска: VectorShape со звездообразным контуром (contourType=bezier, рендер
            // контура откладывается на Этап 2), maskRef ссылается на неё с другого объекта
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

            // TableFrame: прайс-лист 2x2, вторая колонка вдвое шире первой
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

            // Артефакт: локальный override колонтитула с явным artifactType=pagination —
            // валидатор гарантирует, что этот объект нельзя добавить в readingOrder
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

            PageModel page4 = new PageModel(
                    "page-4", 4, 595.0, 842.0, "pt", "master-A",
                    List.of(decorLayer4, textLayer4, footerLayer4),
                    List.of(headingFrame4, divider4, iconDecor, captionFrame, cardGroup,
                            maskStar, maskedShape, priceTable, footerArtifact4),
                    readingOrder4
            );
            writer.writePage(page4);

            StylesCatalog styles = new StylesCatalog(
                    List.of(bodyStyle, headingStyle, footerStyle, quoteStyle),
                    List.of(boldEmphasis)
            );

            writer.finish(
                    "doc-1", "BDoc Demo", "book",
                    "0.1-composite", "ru-RU", styles
            );
        }
    }

    /**
     * Строит объект-override для колонтитула master-footer-1: локальная
     * геометрия отличается от мастера (сдвинут на 20pt выше), но zone/
     * textTemplate/styleRef не переопределены и берутся с MasterPage
     * динамически через PageRenderer.mergeWithMaster.
     */
    private static HeaderFooterRule buildFooterOverride() {
        return new HeaderFooterRule(
                "master-footer-1", "layer-footer",
                new Geometry(70.0, 780.0, 455.0, 20.0),
                null, null, null,
                "master-footer-1",
                Set.of("geometry")
        );
    }
}