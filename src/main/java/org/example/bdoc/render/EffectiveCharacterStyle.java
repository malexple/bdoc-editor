package org.example.bdoc.render;

public final class EffectiveCharacterStyle {

    private final String fontFamily;
    private final double fontSize;
    private final boolean bold;
    private final boolean italic;
    private final String color;
    private final boolean textOverprint;

    public EffectiveCharacterStyle(String fontFamily, double fontSize, boolean bold, boolean italic, String color) {
        this(fontFamily, fontSize, bold, italic, color, false);
    }

    public EffectiveCharacterStyle(String fontFamily, double fontSize, boolean bold, boolean italic,
                                   String color, boolean textOverprint) {
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.bold = bold;
        this.italic = italic;
        this.color = color;
        this.textOverprint = textOverprint;
    }

    public String getFontFamily() { return fontFamily; }
    public double getFontSize() { return fontSize; }
    public boolean isBold() { return bold; }
    public boolean isItalic() { return italic; }
    public String getColor() { return color; }
    public boolean isTextOverprint() { return textOverprint; }
}