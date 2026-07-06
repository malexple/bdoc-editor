package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Плоский POJO-образец цвета (Вопрос 2). fallbackRgb — HEX-строка с наивысшим
 * приоритетом при резолве, обязателен для Spot/CMYK, чтобы PageRenderer
 * мог рисовать на Canvas мгновенно, без математики цветоделения.
 */
public final class Swatch {

    private final String id;
    private final String name;
    private final String colorSpace; // "RGB", "CMYK", "Spot", "Lab"
    private final String fallbackRgb;
    private final Double c, m, y, k; // 0.0-100.0, используются только при colorSpace == CMYK

    @JsonCreator
    public Swatch(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("colorSpace") String colorSpace,
            @JsonProperty("fallbackRgb") String fallbackRgb,
            @JsonProperty("c") Double c,
            @JsonProperty("m") Double m,
            @JsonProperty("y") Double y,
            @JsonProperty("k") Double k) {
        this.id = id;
        this.name = name;
        this.colorSpace = colorSpace;
        this.fallbackRgb = fallbackRgb;
        this.c = c;
        this.m = m;
        this.y = y;
        this.k = k;
    }

    public static Swatch rgb(String id, String name, String hex) {
        return new Swatch(id, name, "RGB", hex, null, null, null, null);
    }

    public static Swatch cmyk(String id, String name, double c, double m, double y, double k, String fallbackRgb) {
        return new Swatch(id, name, "CMYK", fallbackRgb, c, m, y, k);
    }

    public static Swatch spot(String id, String name, String fallbackRgb) {
        return new Swatch(id, name, "Spot", fallbackRgb, null, null, null, null);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getColorSpace() { return colorSpace; }
    public String getFallbackRgb() { return fallbackRgb; }
    public Double getC() { return c; }
    public Double getM() { return m; }
    public Double getY() { return y; }
    public Double getK() { return k; }
}