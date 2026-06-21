package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Geometry {

    @XmlAttribute
    private double x;

    @XmlAttribute
    private double y;

    @XmlAttribute
    private double width;

    @XmlAttribute
    private double height;

    @XmlAttribute
    private Double arcWidth;

    @XmlAttribute
    private Double arcHeight;

    public Geometry() {
    }

    public Geometry(double x, double y, double width, double height) {
        this(x, y, width, height, null, null);
    }

    public Geometry(double x, double y, double width, double height, Double arcWidth, Double arcHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Double getArcWidth() {
        return arcWidth;
    }

    public Double getArcHeight() {
        return arcHeight;
    }
}