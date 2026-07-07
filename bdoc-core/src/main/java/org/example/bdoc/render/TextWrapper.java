package org.example.bdoc.render;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

final class TextWrapper {

    private TextWrapper() {
    }

    /**
     * Один визуальный фрагмент строки: текст плюс шрифт/цвет, с которым его рисовать.
     */
    static final class Run {
        final String text;
        final Font font;
        final String color;

        Run(String text, Font font, String color) {
            this.text = text;
            this.font = font;
            this.color = color;
        }
    }

    /**
     * Один "слово + пробел после него" с привязанным стилем — минимальная единица переноса.
     */
    private static final class Token {
        final String text;
        final EffectiveCharacterStyle style;
        final boolean forcedBreakAfter;

        Token(String text, EffectiveCharacterStyle style, boolean forcedBreakAfter) {
            this.text = text;
            this.style = style;
            this.forcedBreakAfter = forcedBreakAfter;
        }
    }

    static Font toFont(EffectiveCharacterStyle style) {
        FontWeight weight = style.isBold() ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = style.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR;
        return Font.font(style.getFontFamily(), weight, posture, style.getFontSize());
    }

    /**
     * Разбивает список (текст span'а, стиль span'а) на строки не шире maxWidth,
     * возвращая для каждой строки список Run для рендера.
     */
    static List<List<Run>> wrapSpans(List<SpanInput> spanInputs, double maxWidth) {
        List<Token> tokens = tokenize(spanInputs);
        List<List<Run>> lines = new ArrayList<>();
        List<Token> currentLineTokens = new ArrayList<>();
        double currentWidth = 0;

        Text measurer = new Text();

        for (Token token : tokens) {
            measurer.setFont(toFont(token.style));
            measurer.setText(token.text + " ");
            double tokenWidth = measurer.getLayoutBounds().getWidth();

            if (currentWidth + tokenWidth > maxWidth && !currentLineTokens.isEmpty()) {
                lines.add(toRuns(currentLineTokens));
                currentLineTokens = new ArrayList<>();
                currentWidth = 0;
            }

            currentLineTokens.add(token);
            currentWidth += tokenWidth;

            if (token.forcedBreakAfter) {
                lines.add(toRuns(currentLineTokens));
                currentLineTokens = new ArrayList<>();
                currentWidth = 0;
            }
        }

        if (!currentLineTokens.isEmpty()) {
            lines.add(toRuns(currentLineTokens));
        }

        return lines;
    }

    private static List<Token> tokenize(List<SpanInput> spanInputs) {
        List<Token> tokens = new ArrayList<>();
        for (SpanInput input : spanInputs) {
            String[] linesInSpan = input.text().split("\n", -1);
            for (int i = 0; i < linesInSpan.length; i++) {
                boolean lastLineOfSpan = i == linesInSpan.length - 1;
                for (String word : linesInSpan[i].split(" ")) {
                    if (!word.isEmpty()) {
                        boolean forcedBreak = !lastLineOfSpan;
                        tokens.add(new Token(word, input.style(), false));
                        if (forcedBreak) {
                            // помечаем разрыв на последнем токене этой под-строки
                        }
                    }
                }
                if (!lastLineOfSpan && !tokens.isEmpty()) {
                    Token last = tokens.remove(tokens.size() - 1);
                    tokens.add(new Token(last.text, last.style, true));
                }
            }
        }
        return tokens;
    }

    private static List<Run> toRuns(List<Token> lineTokens) {
        List<Run> runs = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        EffectiveCharacterStyle currentStyle = null;

        for (int i = 0; i < lineTokens.size(); i++) {
            Token token = lineTokens.get(i);
            boolean styleChanged = currentStyle != null && !sameVisualStyle(currentStyle, token.style);

            if (styleChanged) {
                runs.add(new Run(buffer.toString(), toFont(currentStyle), currentStyle.getColor()));
                buffer = new StringBuilder();
                buffer.append(" ").append(token.text);
            } else if (currentStyle != null) {
                buffer.append(" ").append(token.text);
            } else {
                buffer.append(token.text);
            }

            currentStyle = token.style;
        }

        if (currentStyle != null) {
            runs.add(new Run(buffer.toString(), toFont(currentStyle), currentStyle.getColor()));
        }

        return runs;
    }

    private static boolean sameVisualStyle(EffectiveCharacterStyle a, EffectiveCharacterStyle b) {
        return a.getFontFamily().equals(b.getFontFamily())
                && a.getFontSize() == b.getFontSize()
                && a.isBold() == b.isBold()
                && a.isItalic() == b.isItalic()
                && a.getColor().equals(b.getColor());
    }

    record SpanInput(String text, EffectiveCharacterStyle style) {
    }
}