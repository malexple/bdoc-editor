package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Стиль для графических/текстовых фреймов (Этап 1.6). Все поля опциональны —
 * стиль работает как набор "галочек": то, что не задано (null), не переопределяет
 * локальное значение объекта или родительский стиль через basedOn.
 * Каскад разрешения: object → objectStyle → basedOn → системный дефолт,
 * см. ObjectStyleResolver.
 */
public final class ObjectStyle {

    private final String id;
    private final String basedOn;
    private final String defaultLayerRef;
    private final Double opacity;
    private final Double arcWidth;
    private final Double arcHeight;
    private final TextWrapModel textWrap;

    @JsonCreator
    public ObjectStyle(
            @JsonProperty("id") String id,
            @JsonProperty("basedOn") String basedOn,
            @JsonProperty("defaultLayerRef") String defaultLayerRef,
            @JsonProperty("opacity") Double opacity,
            @JsonProperty("arcWidth") Double arcWidth,
            @JsonProperty("arcHeight") Double arcHeight,
            @JsonProperty("textWrap") TextWrapModel textWrap) {
        this.id = id;
        this.basedOn = basedOn;
        this.defaultLayerRef = defaultLayerRef;
        this.opacity = opacity;
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
        this.textWrap = textWrap;
    }

    public String getId() { return id; }
    public String getBasedOn() { return basedOn; }
    public String getDefaultLayerRef() { return defaultLayerRef; }
    public Double getOpacity() { return opacity; }
    public Double getArcWidth() { return arcWidth; }
    public Double getArcHeight() { return arcHeight; }
    public TextWrapModel getTextWrap() { return textWrap; }
}