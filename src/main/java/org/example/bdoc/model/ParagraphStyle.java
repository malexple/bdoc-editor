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
    private final String colorSwatchRef;

    @JsonCreator
    public ParagraphStyle(
            @JsonProperty("id") String id,
            @JsonProperty("basedOn") String basedOn,
            @JsonProperty("fontFamily") String fontFamily,
            @JsonProperty("fontSize") Double fontSize,
            @JsonProperty("lineHeight") Double lineHeight,
            @JsonProperty("alignment") String alignment,
            @JsonProperty("color") String color,
            @JsonProperty("colorSwatchRef") String colorSwatchRef) {
        this.id = id;
        this.basedOn = basedOn;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.lineHeight = lineHeight;
        this.alignment = alignment;
        this.color = color;
        this.colorSwatchRef = colorSwatchRef;
    }

    // Совместимость со старыми вызовами в SampleDocuments (7 параметров)
    public ParagraphStyle(String id, String basedOn, String fontFamily, Double fontSize,
                          Double lineHeight, String alignment, String color) {
        this(id, basedOn, fontFamily, fontSize, lineHeight, alignment, color, null);
    }

    public String getId() { return id; }
    public String getBasedOn() { return basedOn; }
    public String getFontFamily() { return fontFamily; }
    public Double getFontSize() { return fontSize; }
    public Double getLineHeight() { return lineHeight; }
    public String getAlignment() { return alignment; }
    public String getColor() { return color; }
    public String getColorSwatchRef() { return colorSwatchRef; }
}