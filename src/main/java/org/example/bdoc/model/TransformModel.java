package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Преобразование, накладываемое НАД Geometry: перенос, поворот (градусы,
 * вокруг центра bounding box), масштаб. Geometry остаётся неизменным
 * bounding box в локальных координатах при rotation=0 — Transform не
 * искажает сами x/y/width/height, только визуальное положение при рендере.
 */
public final class TransformModel {

    private final double translateX;
    private final double translateY;
    private final double rotationDegrees;
    private final double scaleX;
    private final double scaleY;

    @JsonCreator
    public TransformModel(
            @JsonProperty("translateX") Double translateX,
            @JsonProperty("translateY") Double translateY,
            @JsonProperty("rotationDegrees") Double rotationDegrees,
            @JsonProperty("scaleX") Double scaleX,
            @JsonProperty("scaleY") Double scaleY) {
        this.translateX = translateX != null ? translateX : 0.0;
        this.translateY = translateY != null ? translateY : 0.0;
        this.rotationDegrees = rotationDegrees != null ? rotationDegrees : 0.0;
        this.scaleX = scaleX != null ? scaleX : 1.0;
        this.scaleY = scaleY != null ? scaleY : 1.0;
    }

    public static TransformModel identity() {
        return new TransformModel(0.0, 0.0, 0.0, 1.0, 1.0);
    }

    public boolean isIdentity() {
        return translateX == 0.0 && translateY == 0.0
                && rotationDegrees == 0.0 && scaleX == 1.0 && scaleY == 1.0;
    }

    public double getTranslateX() { return translateX; }
    public double getTranslateY() { return translateY; }
    public double getRotationDegrees() { return rotationDegrees; }
    public double getScaleX() { return scaleX; }
    public double getScaleY() { return scaleY; }
}