package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public final class VectorShape extends BdocObject {

    private final String shapeType;

    // Этап 1.7 (Вопрос 5): raw HEX-цвета для обратной совместимости
    // + опциональные ссылки на Swatch, приоритетные при резолве через ColorResolver.
    private final String fillColor;
    private final String strokeColor;
    private final String fillColorSwatchRef;
    private final String strokeColorSwatchRef;

    public VectorShape(String id, String layerRef, Geometry geometry, String shapeType) {
        super(id, layerRef, geometry);
        this.shapeType = shapeType;
        this.fillColor = null;
        this.strokeColor = null;
        this.fillColorSwatchRef = null;
        this.strokeColorSwatchRef = null;
    }

    public VectorShape(String id, String layerRef, Geometry geometry, String shapeType,
                       String masterSourceId, Set<String> overriddenProperties) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties);
        this.shapeType = shapeType;
        this.fillColor = null;
        this.strokeColor = null;
        this.fillColorSwatchRef = null;
        this.strokeColorSwatchRef = null;
    }

    /**
     * Совместимость с Этапом 1.6: старая сигнатура из 15 параметров
     * (без цвета), которой пользуются существующие вызовы в SampleDocuments
     * (maskStar, maskedShape, rotatedRectDemo, polygonDemo, letterODemo).
     * Делегирует в новый @JsonCreator-конструктор с цветами = null.
     */
    public VectorShape(
            String id, String layerRef, Geometry geometry, String shapeType,
            String masterSourceId, Set<String> overriddenProperties,
            Boolean visible, Geometry clipGeometry, String maskRef,
            Boolean mask, Boolean artifact, String artifactType,
            TextWrapModel textWrap, PathModel pathData, TransformModel transform) {
        this(id, layerRef, geometry, shapeType, masterSourceId, overriddenProperties,
                visible, clipGeometry, maskRef, mask, artifact, artifactType,
                textWrap, pathData, transform, null, null, null, null);
    }

    /**
     * Точка входа для Jackson (JSON/CBOR): сигнатура Этапа 1.6 плюс 4 поля
     * цвета в конце. Старые файлы без этих полей читаются нормально —
     * Jackson подставит null (FAIL_ON_UNKNOWN_PROPERTIES отключён).
     */
    @JsonCreator
    public VectorShape(
            @JsonProperty("id") String id,
            @JsonProperty("layerRef") String layerRef,
            @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("shapeType") String shapeType,
            @JsonProperty("masterSourceId") String masterSourceId,
            @JsonProperty("overriddenProperties") Set<String> overriddenProperties,
            @JsonProperty("visible") Boolean visible,
            @JsonProperty("clipGeometry") Geometry clipGeometry,
            @JsonProperty("maskRef") String maskRef,
            @JsonProperty("mask") Boolean mask,
            @JsonProperty("artifact") Boolean artifact,
            @JsonProperty("artifactType") String artifactType,
            @JsonProperty("textWrap") TextWrapModel textWrap,
            @JsonProperty("pathData") PathModel pathData,
            @JsonProperty("transform") TransformModel transform,
            @JsonProperty("fillColor") String fillColor,
            @JsonProperty("strokeColor") String strokeColor,
            @JsonProperty("fillColorSwatchRef") String fillColorSwatchRef,
            @JsonProperty("strokeColorSwatchRef") String strokeColorSwatchRef) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties, visible,
                clipGeometry, maskRef, mask, artifact, artifactType, textWrap, pathData, transform);
        this.shapeType = shapeType;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.fillColorSwatchRef = fillColorSwatchRef;
        this.strokeColorSwatchRef = strokeColorSwatchRef;
    }

    /**
     * Полный конструктор Этапа 1.6 (objectStyleRef/opacity/anchoredSettings),
     * используется в SampleDocuments (styledCardInherited, styledCardOverride,
     * anchoredIcon). Цвет здесь не программируется этой сигнатурой.
     */
    public VectorShape(
            String id, String layerRef, Geometry geometry, String shapeType,
            String masterSourceId, Set<String> overriddenProperties,
            Boolean visible, Geometry clipGeometry, String maskRef,
            Boolean mask, Boolean artifact, String artifactType,
            TextWrapModel textWrap, PathModel pathData, TransformModel transform,
            String objectStyleRef, Double opacity, AnchoredObjectSettings anchoredSettings) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties, visible,
                clipGeometry, maskRef, mask, artifact, artifactType, textWrap, pathData, transform,
                objectStyleRef, opacity, anchoredSettings);
        this.shapeType = shapeType;
        this.fillColor = null;
        this.strokeColor = null;
        this.fillColorSwatchRef = null;
        this.strokeColorSwatchRef = null;
    }

    /** Удобный конструктор для SampleDocuments: минимум параметров + цвет. */
    public VectorShape(String id, String layerRef, Geometry geometry, String shapeType,
                       String fillColor, String strokeColor,
                       String fillColorSwatchRef, String strokeColorSwatchRef) {
        super(id, layerRef, geometry);
        this.shapeType = shapeType;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.fillColorSwatchRef = fillColorSwatchRef;
        this.strokeColorSwatchRef = strokeColorSwatchRef;
    }

    public String getShapeType() { return shapeType; }
    public String getFillColor() { return fillColor; }
    public String getStrokeColor() { return strokeColor; }
    public String getFillColorSwatchRef() { return fillColorSwatchRef; }
    public String getStrokeColorSwatchRef() { return strokeColorSwatchRef; }

    @Override
    public String getType() { return "VectorShape"; }
}