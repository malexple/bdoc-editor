package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Span {
    private final String text;
    private final String characterStyleRef;

    private final String inlineAssetRef;
    private final Double inlineWidth;
    private final Double inlineHeight;

    private final FootnoteModel footnote;
    private final ReferenceModel reference;

    @JsonCreator
    public Span(
            @JsonProperty("text") String text,
            @JsonProperty("characterStyleRef") String characterStyleRef,
            @JsonProperty("inlineAssetRef") String inlineAssetRef,
            @JsonProperty("inlineWidth") Double inlineWidth,
            @JsonProperty("inlineHeight") Double inlineHeight,
            @JsonProperty("footnote") FootnoteModel footnote,
            @JsonProperty("reference") ReferenceModel reference) {
        this.text = text != null ? text : "";
        this.characterStyleRef = characterStyleRef;
        this.inlineAssetRef = inlineAssetRef;
        this.inlineWidth = inlineWidth;
        this.inlineHeight = inlineHeight;
        this.footnote = footnote;
        this.reference = reference;
    }

    // Сохраняем старый конструктор для обратной совместимости тестов и SampleDocuments
    public Span(String text, String characterStyleRef) {
        this(text, characterStyleRef, null, null, null, null, null);
    }

    public static Span plain(String text) {
        return new Span(text, null);
    }

    public String getText() { return text; }
    public String getCharacterStyleRef() { return characterStyleRef; }
    public String getInlineAssetRef() { return inlineAssetRef; }
    public Double getInlineWidth() { return inlineWidth; }
    public Double getInlineHeight() { return inlineHeight; }
    public FootnoteModel getFootnote() { return footnote; }
    public ReferenceModel getReference() { return reference; }
}