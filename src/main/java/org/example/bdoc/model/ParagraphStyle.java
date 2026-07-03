package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ParagraphStyle {
    private final String id;
    private final String basedOn;
    private final String fontFamily;
    private final Double fontSize;
    private final Double lineHeight;
    private final String alignment;
    private final String color;

    @JsonCreator
    public ParagraphStyle(
            @JsonProperty("id") String id,
            @JsonProperty("basedOn") String basedOn,
            @JsonProperty("fontFamily") String fontFamily,
            @JsonProperty("fontSize") Double fontSize,
            @JsonProperty("lineHeight") Double lineHeight,
            @JsonProperty("alignment") String alignment,
            @JsonProperty("color") String color) {
        this.id = id;
        this.basedOn = basedOn;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.lineHeight = lineHeight;
        this.alignment = alignment;
        this.color = color;
    }

    public String getId() { return id; }
    public String getBasedOn() { return basedOn; }
    public String getFontFamily() { return fontFamily; }
    public Double getFontSize() { return fontSize; }
    public Double getLineHeight() { return lineHeight; }
    public String getAlignment() { return alignment; }
    public String getColor() { return color; }
}