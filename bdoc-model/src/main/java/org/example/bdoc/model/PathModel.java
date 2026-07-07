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
    private final String fillRule;

    public PathModel(String contourType, List<PathPoint> points) {
        this(contourType, points, null);
    }

    @JsonCreator
    public PathModel(
            @JsonProperty("contourType") String contourType,
            @JsonProperty("points") List<PathPoint> points,
            @JsonProperty("fillRule") String fillRule) {
        this.contourType = contourType != null ? contourType : "primitive";
        this.points = points != null ? points : List.of();
        this.fillRule = fillRule != null ? fillRule : "non-zero";
    }

    public static PathModel primitive() {
        return new PathModel("primitive", List.of(), "non-zero");
    }

    public String getContourType() { return contourType; }
    public List<PathPoint> getPoints() { return points; }
    public String getFillRule() { return fillRule; }
}