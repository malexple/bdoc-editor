package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Geometry {
    // Убираем final для координат X и Y, чтобы фреймы можно было двигать
    private double x;
    private double y;
    private double width;
    private double height;
    private Double arcWidth;
    private Double arcHeight;

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

    // Сеттеры для интерактивного перемещения мышкой
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }

    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }
}
