package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public final class VectorShape extends BdocObject {

    private final String shapeType;
    private final String fillColor;
    private final String strokeColor;
    private final String fillColorSwatchRef;
    private final String strokeColorSwatchRef;

    // Этап 1.8 (Вопрос 5): overprint-флаги, дефолт false.
    private final boolean fillOverprint;
    private final boolean strokeOverprint;

    public VectorShape(String id, String layerRef, Geometry geometry, String shapeType) {
        super(id, layerRef, geometry);
        this.shapeType = shapeType;
        this.fillColor = null;
        this.strokeColor = null;
        this.fillColorSwatchRef = null;
        this.strokeColorSwatchRef = null;
        this.fillOverprint = false;
        this.strokeOverprint = false;
    }

    public VectorShape(String id, String layerRef, Geometry geometry, String shapeType,
                       String masterSourceId, Set<String> overriddenProperties) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties);
        this.shapeType = shapeType;
        this.fillColor = null;
        this.strokeColor = null;
        this.fillColorSwatchRef = null;
        this.strokeColorSwatchRef = null;
        this.fillOverprint = false;
        this.strokeOverprint = false;
    }

    public VectorShape(
            String id, String layerRef, Geometry geometry, String shapeType,
            String masterSourceId, Set<String> overriddenProperties,
            Boolean visible, Geometry clipGeometry, String maskRef,
            Boolean mask, Boolean artifact, String artifactType,
            TextWrapModel textWrap, PathModel pathData, TransformModel transform) {
        this(id, layerRef, geometry, shapeType, masterSourceId, overriddenProperties,
                visible, clipGeometry, maskRef, mask, artifact, artifactType,
                textWrap, pathData, transform, null, null, null, null, false, false);
    }

    /**
     * Точка входа для Jackson: сигнатура Этапа 1.6 + 4 цветовых поля (1.7)
     * + 2 overprint-флага (1.8). Старые файлы без этих полей читаются
     * нормально — Jackson подставит null/false.
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
            @JsonProperty("strokeColorSwatchRef") String strokeColorSwatchRef,
            @JsonProperty("fillOverprint") Boolean fillOverprint,
            @JsonProperty("strokeOverprint") Boolean strokeOverprint) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties, visible,
                clipGeometry, maskRef, mask, artifact, artifactType, textWrap, pathData, transform);
        this.shapeType = shapeType;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.fillColorSwatchRef = fillColorSwatchRef;
        this.strokeColorSwatchRef = strokeColorSwatchRef;
        this.fillOverprint = fillOverprint != null ? fillOverprint : false;
        this.strokeOverprint = strokeOverprint != null ? strokeOverprint : false;
    }

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
        this.fillOverprint = false;
        this.strokeOverprint = false;
    }

    /** Удобный конструктор для SampleDocuments: минимум параметров + цвет (Этап 1.7). */
    public VectorShape(String id, String layerRef, Geometry geometry, String shapeType,
                       String fillColor, String strokeColor,
                       String fillColorSwatchRef, String strokeColorSwatchRef) {
        super(id, layerRef, geometry);
        this.shapeType = shapeType;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.fillColorSwatchRef = fillColorSwatchRef;
        this.strokeColorSwatchRef = strokeColorSwatchRef;
        this.fillOverprint = false;
        this.strokeOverprint = false;
    }

    /** Удобный конструктор для SampleDocuments с overprint-флагами (Этап 1.8). */
    public VectorShape(String id, String layerRef, Geometry geometry, String shapeType,
                       String fillColor, String strokeColor,
                       String fillColorSwatchRef, String strokeColorSwatchRef,
                       boolean fillOverprint, boolean strokeOverprint) {
        super(id, layerRef, geometry);
        this.shapeType = shapeType;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.fillColorSwatchRef = fillColorSwatchRef;
        this.strokeColorSwatchRef = strokeColorSwatchRef;
        this.fillOverprint = fillOverprint;
        this.strokeOverprint = strokeOverprint;
    }

    public String getShapeType() { return shapeType; }
    public String getFillColor() { return fillColor; }
    public String getStrokeColor() { return strokeColor; }
    public String getFillColorSwatchRef() { return fillColorSwatchRef; }
    public String getStrokeColorSwatchRef() { return strokeColorSwatchRef; }
    public boolean isFillOverprint() { return fillOverprint; }
    public boolean isStrokeOverprint() { return strokeOverprint; }

    @Override
    public String getType() { return "VectorShape"; }
}