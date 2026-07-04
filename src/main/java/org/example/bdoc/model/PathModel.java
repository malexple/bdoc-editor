package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Универсальный контур, используемый и для маски/обтекания текста, и в
 * будущем для произвольных векторных фигур. На Этапе 1 рендерер понимает
 * только contourType "primitive" (то есть контур не используется, объект
 * рисуется по обычной Geometry); "bezier" сохраняется и читается, но не
 * рендерится — это архитектурный задел под Этап 2/3.
 */
public final class PathModel {

    private final String contourType;
    private final List<PathPoint> points;

    @JsonCreator
    public PathModel(
            @JsonProperty("contourType") String contourType,
            @JsonProperty("points") List<PathPoint> points) {
        this.contourType = contourType != null ? contourType : "primitive";
        this.points = points != null ? points : List.of();
    }

    public static PathModel primitive() {
        return new PathModel("primitive", List.of());
    }

    public String getContourType() { return contourType; }
    public List<PathPoint> getPoints() { return points; }
}