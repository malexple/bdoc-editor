package org.example.bdoc.render;

import org.example.bdoc.model.StylesCatalog;
import org.example.bdoc.model.Swatch;

/**
 * Разрешает эффективный HEX-цвет по модели "Smart Fallback" (Вопросы 4 и 7):
 * swatchRef -> fallbackRgb Swatch (приоритет) -> расчёт по формуле CMYK ->
 * MAGENTA для Spot без фолбека -> дефолтный чёрный для Lab/остального.
 * Если swatchRef == null, возвращает rawColor как есть (откат на текстовое поле),
 * дальнейший дефолт (системный чёрный/стиль) — ответственность вызывающего кода.
 */
public final class ColorResolver {

    private static final String SPOT_WARNING_COLOR = "#FF00FF";
    private static final String PRINT_DEFAULT_BLACK = "#000000";

    private ColorResolver() {
    }

    public static String resolve(String rawColor, String swatchRef, StylesCatalog styles) {
        if (swatchRef == null || styles == null) {
            return rawColor;
        }
        Swatch swatch = styles.findSwatch(swatchRef);
        if (swatch == null) {
            return rawColor;
        }
        if (swatch.getFallbackRgb() != null) {
            return swatch.getFallbackRgb();
        }
        if ("CMYK".equalsIgnoreCase(swatch.getColorSpace())) {
            return resolveCmyk(swatch);
        }
        if ("Spot".equalsIgnoreCase(swatch.getColorSpace())) {
            return SPOT_WARNING_COLOR;
        }
        return PRINT_DEFAULT_BLACK;
    }

    private static String resolveCmyk(Swatch swatch) {
        double c = nz(swatch.getC()) / 100.0;
        double m = nz(swatch.getM()) / 100.0;
        double y = nz(swatch.getY()) / 100.0;
        double k = nz(swatch.getK()) / 100.0;

        int r = (int) Math.round(255 * (1 - c) * (1 - k));
        int g = (int) Math.round(255 * (1 - m) * (1 - k));
        int b = (int) Math.round(255 * (1 - y) * (1 - k));

        return String.format("#%02X%02X%02X", clamp(r), clamp(g), clamp(b));
    }

    private static double nz(Double v) { return v != null ? v : 0.0; }
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}