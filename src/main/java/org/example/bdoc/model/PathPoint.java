package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Одна команда SVG-подобного пути: M (move to), L (line to), C (cubic bezier).
 * Для M/L используются только x/y. Для C дополнительно нужны контрольные точки
 * x1/y1 (первая) и x2/y2 (вторая) — конечная точка кривой хранится в x/y.
 */
public final class PathPoint {

    private final String command;
    private final double x;
    private final double y;
    private final Double x1;
    private final Double y1;
    private final Double x2;
    private final Double y2;

    @JsonCreator
    public PathPoint(
            @JsonProperty("command") String command,
            @JsonProperty("x") double x,
            @JsonProperty("y") double y,
            @JsonProperty("x1") Double x1,
            @JsonProperty("y1") Double y1,
            @JsonProperty("x2") Double x2,
            @JsonProperty("y2") Double y2) {
        this.command = command;
        this.x = x;
        this.y = y;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public static PathPoint moveTo(double x, double y) {
        return new PathPoint("M", x, y, null, null, null, null);
    }

    public static PathPoint lineTo(double x, double y) {
        return new PathPoint("L", x, y, null, null, null, null);
    }

    public static PathPoint cubicTo(double x1, double y1, double x2, double y2, double x, double y) {
        return new PathPoint("C", x, y, x1, y1, x2, y2);
    }

    public String getCommand() { return command; }
    public double getX() { return x; }
    public double getY() { return y; }
    public Double getX1() { return x1; }
    public Double getY1() { return y1; }
    public Double getX2() { return x2; }
    public Double getY2() { return y2; }
}