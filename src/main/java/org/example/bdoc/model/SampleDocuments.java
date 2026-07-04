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
     * Записывает демонстрационный .bdoc-документ в указанный файл.
     * В новой архитектуре документ не существует "в памяти" сам по себе —
     * он всегда сохраняется как контейнер и затем открывается через DocumentHandle.
     *
     * Демонстрирует Этап 1.1: MasterPage с колонтитулом и декоративной линией,
     * страницу, наследующую эти объекты через templateRef, и один объект
     * с частичным override (колонтитул сдвинут локально, но текст всё ещё
     * наследуется от мастера).
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

            CharacterStyle boldEmphasis = new CharacterStyle(
                    "emphasis", null, null, null, true, false, "#B91C1C"
            );

            Paragraph body = new Paragraph("body", "body-text", List.of(
                    Span.plain("Это тестовый документ BDoc. Параграф "),
                    new Span("заголовка", "emphasis"),
                    Span.plain(" наследует шрифт и цвет от body-text через basedOn, но переопределяет размер, выравнивание и цвет.")
            ));

            StoryModel story = new StoryModel("story-1", List.of(
                    new Paragraph("heading", "heading-1", "Демонстрация стилей BDoc"),
                    body
            ));
            writer.writeStory(story);

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

            LayerModel bgLayer = new LayerModel("layer-bg", "Background", "background", true, 1.0);
            LayerModel decorLayer = new LayerModel("layer-decor", "Decoration", "decoration", true, 1.0);
            LayerModel textLayer = new LayerModel("layer-text", "Text", "text", true, 1.0);
            LayerModel footerLayer = new LayerModel("layer-footer", "Footer", "header-footer", true, 1.0);

            VectorShape divider = new VectorShape(
                    "shape-2", "layer-decor",
                    new Geometry(70.0, 120.0, 455.0, 0.0),
                    "line"
            );

            TextFrame textFrame = new TextFrame(
                    "text-1", "layer-text",
                    new Geometry(70.0, 150.0, 455.0, 180.0),
                    "story-1"
            );

            HeaderFooterRule footerOverride = new HeaderFooterRule(
                    "master-footer-1", "layer-footer",
                    new Geometry(70.0, 780.0, 455.0, 20.0),
                    null, null, null,
                    "master-footer-1", Set.of("geometry")
            );

            PageModel page = new PageModel(
                    "page-1", 1, 595.0, 842.0, "pt", "master-A",
                    List.of(bgLayer, decorLayer, textLayer, footerLayer),
                    List.of(divider, textFrame, footerOverride)
            );
            writer.writePage(page);

            StylesCatalog styles = new StylesCatalog(
                    List.of(bodyStyle, headingStyle, footerStyle),
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