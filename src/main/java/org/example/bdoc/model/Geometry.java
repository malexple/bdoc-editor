package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Geometry {

    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final Double arcWidth;
    private final Double arcHeight;

    @JsonCreator
    public Geometry(
            @JsonProperty("x") double x,
            @JsonProperty("y") double y,
            @JsonProperty("width") double width,
            @JsonProperty("height") double height,
            @JsonProperty("arcWidth") Double arcWidth,
            @JsonProperty("arcHeight") Double arcHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
    }

    public Geometry(double x, double y, double width, double height) {
        this(x, y, width, height, null, null);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public Double getArcWidth() { return arcWidth; }
    public Double getArcHeight() { return arcHeight; }
}