package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class LineObject extends BdocObject {

    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;
    private final double strokeWidth;
    private final String strokeColor;
    private final String strokePattern;
    private final String startCap;
    private final String endCap;
    private final String strokeColorSwatchRef;
    private final boolean strokeOverprint;

    public LineObject(String id, String layerRef, double x1, double y1, double x2, double y2,
                      double strokeWidth, String strokeColor, String strokePattern,
                      String startCap, String endCap) {
        super(id, layerRef, boundingBoxOf(x1, y1, x2, y2));
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
        this.strokeWidth = strokeWidth;
        this.strokeColor = strokeColor;
        this.strokePattern = strokePattern != null ? strokePattern : "solid";
        this.startCap = startCap != null ? startCap : "none";
        this.endCap = endCap != null ? endCap : "none";
        this.strokeColorSwatchRef = null;
        this.strokeOverprint = false;
    }

    public LineObject(String id, String layerRef, double x1, double y1, double x2, double y2,
                      double strokeWidth, String strokeColor, String strokePattern,
                      String startCap, String endCap,
                      Boolean visible, PathModel pathData, TransformModel transform,
                      String strokeColorSwatchRef) {
        this(id, layerRef, x1, y1, x2, y2, strokeWidth, strokeColor, strokePattern,
                startCap, endCap, visible, pathData, transform, strokeColorSwatchRef, false);
    }

    @JsonCreator
    public LineObject(
            @JsonProperty("id") String id,
            @JsonProperty("layerRef") String layerRef,
            @JsonProperty("x1") double x1,
            @JsonProperty("y1") double y1,
            @JsonProperty("x2") double x2,
            @JsonProperty("y2") double y2,
            @JsonProperty("strokeWidth") double strokeWidth,
            @JsonProperty("strokeColor") String strokeColor,
            @JsonProperty("strokePattern") String strokePattern,
            @JsonProperty("startCap") String startCap,
            @JsonProperty("endCap") String endCap,
            @JsonProperty("visible") Boolean visible,
            @JsonProperty("pathData") PathModel pathData,
            @JsonProperty("transform") TransformModel transform,
            @JsonProperty("strokeColorSwatchRef") String strokeColorSwatchRef,
            @JsonProperty("strokeOverprint") Boolean strokeOverprint) {
        super(id, layerRef, boundingBoxOf(x1, y1, x2, y2));
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
        this.strokeWidth = strokeWidth;
        this.strokeColor = strokeColor;
        this.strokePattern = strokePattern != null ? strokePattern : "solid";
        this.startCap = startCap != null ? startCap : "none";
        this.endCap = endCap != null ? endCap : "none";
        this.strokeColorSwatchRef = strokeColorSwatchRef;
        this.strokeOverprint = strokeOverprint != null ? strokeOverprint : false;
        if (visible != null) {
            this.visible = visible;
        }
    }

    private static Geometry boundingBoxOf(double x1, double y1, double x2, double y2) {
        double minX = Math.min(x1, x2);
        double minY = Math.min(y1, y2);
        double width = Math.abs(x2 - x1);
        double height = Math.abs(y2 - y1);
        return new Geometry(minX, minY, width, height);
    }

    @JsonIgnore
    @Override
    public Geometry getGeometry() { return super.getGeometry(); }

    public double getX1() { return x1; }
    public double getY1() { return y1; }
    public double getX2() { return x2; }
    public double getY2() { return y2; }
    public double getStrokeWidth() { return strokeWidth; }
    public String getStrokeColor() { return strokeColor; }
    public String getStrokePattern() { return strokePattern; }
    public String getStartCap() { return startCap; }
    public String getEndCap() { return endCap; }
    public String getStrokeColorSwatchRef() { return strokeColorSwatchRef; }
    public boolean isStrokeOverprint() { return strokeOverprint; }

    @Override
    public String getType() { return "LineObject"; }
}