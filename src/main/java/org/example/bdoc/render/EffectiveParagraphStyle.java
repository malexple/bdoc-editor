package org.example.bdoc.render;

public final class EffectiveParagraphStyle {

    private final String fontFamily;
    private final double fontSize;
    private final double lineHeight;
    private final String alignment;
    private final String color;

    public EffectiveParagraphStyle(String fontFamily, double fontSize, double lineHeight,
                                   String alignment, String color) {
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.lineHeight = lineHeight;
        this.alignment = alignment;
        this.color = color;
    }

    public String getFontFamily() { return fontFamily; }
    public double getFontSize() { return fontSize; }
    public double getLineHeight() { return lineHeight; }
    public String getAlignment() { return alignment; }
    public String getColor() { return color; }
}