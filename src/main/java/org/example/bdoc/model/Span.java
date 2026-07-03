package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Span {

    private final String text;
    private final String characterStyleRef;

    @JsonCreator
    public Span(
            @JsonProperty("text") String text,
            @JsonProperty("characterStyleRef") String characterStyleRef) {
        this.text = text != null ? text : "";
        this.characterStyleRef = characterStyleRef;
    }

    public static Span plain(String text) {
        return new Span(text, null);
    }

    public String getText() { return text; }
    public String getCharacterStyleRef() { return characterStyleRef; }
}