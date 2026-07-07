package org.example.bdoc.render;

import org.example.bdoc.model.ParagraphStyle;
import org.example.bdoc.model.StylesCatalog;

import java.util.HashSet;
import java.util.Set;

/**
 * Разрешает цепочку наследования basedOn для ParagraphStyle
 * и сводит её в EffectiveParagraphStyle, где все поля гарантированно
 * заполнены. Цвет разрешается по модели Smart Fallback (Вопросы 4, 7):
 * colorSwatchRef -> ColorResolver -> rawColor -> системный дефолт.
 */
public final class StyleResolver {

    private static final String DEFAULT_FONT_FAMILY = "Serif";
    private static final double DEFAULT_FONT_SIZE = 20.0;
    private static final double DEFAULT_LINE_HEIGHT = 1.2;
    private static final String DEFAULT_ALIGNMENT = "left";
    private static final String DEFAULT_COLOR = "#0F172A";

    private final StylesCatalog styles;

    public StyleResolver(StylesCatalog styles) {
        this.styles = styles;
    }

    public EffectiveParagraphStyle resolve(String styleRef) {
        if (styleRef == null) {
            return defaults();
        }

        String fontFamily = null;
        Double fontSize = null;
        Double lineHeight = null;
        String alignment = null;
        String color = null;
        String colorSwatchRef = null;

        Set<String> visited = new HashSet<>();
        String currentId = styleRef;

        while (currentId != null) {
            if (!visited.add(currentId)) {
                throw new IllegalStateException(
                        "Cyclic basedOn reference detected in paragraph styles: " + currentId);
            }

            ParagraphStyle style = styles.findParagraphStyle(currentId);
            if (style == null) {
                break;
            }

            if (fontFamily == null) fontFamily = style.getFontFamily();
            if (fontSize == null) fontSize = style.getFontSize();
            if (lineHeight == null) lineHeight = style.getLineHeight();
            if (alignment == null) alignment = style.getAlignment();
            if (color == null) color = style.getColor();
            if (colorSwatchRef == null) colorSwatchRef = style.getColorSwatchRef();

            currentId = style.getBasedOn();
        }

        String resolvedColor = ColorResolver.resolve(color, colorSwatchRef, styles);

        return new EffectiveParagraphStyle(
                fontFamily != null ? fontFamily : DEFAULT_FONT_FAMILY,
                fontSize != null ? fontSize : DEFAULT_FONT_SIZE,
                lineHeight != null ? lineHeight : DEFAULT_LINE_HEIGHT,
                alignment != null ? alignment : DEFAULT_ALIGNMENT,
                resolvedColor != null ? resolvedColor : DEFAULT_COLOR
        );
    }

    private EffectiveParagraphStyle defaults() {
        return new EffectiveParagraphStyle(
                DEFAULT_FONT_FAMILY, DEFAULT_FONT_SIZE, DEFAULT_LINE_HEIGHT,
                DEFAULT_ALIGNMENT, DEFAULT_COLOR);
    }
}