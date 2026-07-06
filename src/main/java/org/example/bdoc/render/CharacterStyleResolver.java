package org.example.bdoc.render;

import org.example.bdoc.model.CharacterStyle;
import org.example.bdoc.model.StylesCatalog;

import java.util.HashSet;
import java.util.Set;

public final class CharacterStyleResolver {

    private final StylesCatalog styles;

    public CharacterStyleResolver(StylesCatalog styles) {
        this.styles = styles;
    }

    /**
     * Разрешает CharacterStyle span'а, используя эффективный стиль параграфа
     * как базу для незаданных полей. Цвет разрешается по модели Smart Fallback:
     * colorSwatchRef -> ColorResolver -> rawColor -> цвет параграфа (уже резолвлен).
     * textOverprint (Этап 1.8, Вопрос 5) — nullable Boolean, каскадится по
     * цепочке basedOn так же, как fontFamily/fontSize; если не задан нигде,
     * дефолт false (не наложение, обычная выворотка).
     */
    public EffectiveCharacterStyle resolve(String characterStyleRef, EffectiveParagraphStyle paragraphFallback) {
        String fontFamily = null;
        Double fontSize = null;
        Boolean bold = null;
        Boolean italic = null;
        String color = null;
        String colorSwatchRef = null;
        Boolean textOverprint = null;

        Set<String> visited = new HashSet<>();
        String currentId = characterStyleRef;

        while (currentId != null) {
            if (!visited.add(currentId)) {
                throw new IllegalStateException(
                        "Cyclic basedOn reference detected in character styles: " + currentId);
            }

            CharacterStyle style = styles.findCharacterStyle(currentId);
            if (style == null) {
                break;
            }

            if (fontFamily == null) fontFamily = style.getFontFamily();
            if (fontSize == null) fontSize = style.getFontSize();
            if (bold == null && style.isBold()) bold = true;
            if (italic == null && style.isItalic()) italic = true;
            if (color == null) color = style.getColor();
            if (colorSwatchRef == null) colorSwatchRef = style.getColorSwatchRef();
            if (textOverprint == null) textOverprint = style.getTextOverprint();

            currentId = style.getBasedOn();
        }

        String resolvedColor = ColorResolver.resolve(color, colorSwatchRef, styles);

        return new EffectiveCharacterStyle(
                fontFamily != null ? fontFamily : paragraphFallback.getFontFamily(),
                fontSize != null ? fontSize : paragraphFallback.getFontSize(),
                bold != null && bold,
                italic != null && italic,
                resolvedColor != null ? resolvedColor : paragraphFallback.getColor(),
                textOverprint != null && textOverprint
        );
    }
}