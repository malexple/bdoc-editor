package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CharacterStyle {
    private final String id;
    private final String basedOn;
    private final String fontFamily;
    private final Double fontSize;
    private final boolean bold;
    private final boolean italic;
    private final String color;
    private final String colorSwatchRef;
    private final Boolean textOverprint; // nullable для каскада наследования

    @JsonCreator
    public CharacterStyle(
            @JsonProperty("id") String id,
            @JsonProperty("basedOn") String basedOn,
            @JsonProperty("fontFamily") String fontFamily,
            @JsonProperty("fontSize") Double fontSize,
            @JsonProperty("bold") boolean bold,
            @JsonProperty("italic") boolean italic,
            @JsonProperty("color") String color,
            @JsonProperty("colorSwatchRef") String colorSwatchRef,
            @JsonProperty("textOverprint") Boolean textOverprint) {
        this.id = id;
        this.basedOn = basedOn;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.bold = bold;
        this.italic = italic;
        this.color = color;
        this.colorSwatchRef = colorSwatchRef;
        this.textOverprint = textOverprint;
    }

    public CharacterStyle(String id, String basedOn, String fontFamily, Double fontSize,
                          boolean bold, boolean italic, String color, String colorSwatchRef) {
        this(id, basedOn, fontFamily, fontSize, bold, italic, color, colorSwatchRef, null);
    }

    public CharacterStyle(String id, String basedOn, String fontFamily, Double fontSize,
                          boolean bold, boolean italic, String color) {
        this(id, basedOn, fontFamily, fontSize, bold, italic, color, null, null);
    }

    public String getId() { return id; }
    public String getBasedOn() { return basedOn; }
    public String getFontFamily() { return fontFamily; }
    public Double getFontSize() { return fontSize; }
    public boolean isBold() { return bold; }
    public boolean isItalic() { return italic; }
    public String getColor() { return color; }
    public String getColorSwatchRef() { return colorSwatchRef; }
    public Boolean getTextOverprint() { return textOverprint; }
}