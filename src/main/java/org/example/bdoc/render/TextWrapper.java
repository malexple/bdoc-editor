package org.example.bdoc.render;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбивает строку текста на строки, вписывающиеся в заданную ширину,
 * используя реальное измерение глифов выбранного шрифта.
 */
final class TextWrapper {

    private TextWrapper() {
    }

    static List<String> wrap(String text, Font font, double maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }

        Text measurer = new Text();
        measurer.setFont(font);

        for (String rawLine : text.split("\n", -1)) {
            String[] words = rawLine.split(" ");
            StringBuilder current = new StringBuilder();

            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                measurer.setText(candidate);
                double width = measurer.getLayoutBounds().getWidth();

                if (width > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            lines.add(current.toString());
        }

        return lines;
    }
}