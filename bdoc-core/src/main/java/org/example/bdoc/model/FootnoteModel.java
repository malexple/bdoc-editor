package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Сноска, привязанная к конкретному Span через якорь. На Этапе 1 текст
 * сноски хранится в модели, но НЕ верстается автоматически внизу страницы —
 * рендерится только надстрочный маркер (number) в потоке текста.
 * Полноценная вёрстка футера со сносками — отдельная задача Этапа 2.
 */
public final class FootnoteModel {

    private final String number;
    private final String text;

    @JsonCreator
    public FootnoteModel(
            @JsonProperty("number") String number,
            @JsonProperty("text") String text) {
        this.number = number;
        this.text = text != null ? text : "";
    }

    public String getNumber() { return number; }
    public String getText() { return text; }
}