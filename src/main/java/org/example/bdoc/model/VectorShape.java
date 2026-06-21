package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "vectorShape")
@XmlAccessorType(XmlAccessType.FIELD)
public class VectorShape extends BdocObject {

    @XmlAttribute
    private ShapeType shapeType;
    @XmlAttribute
    private double x;
    @XmlAttribute
    private double y;
    @XmlAttribute
    private double width;
    @XmlAttribute
    private double height;
    @XmlAttribute
    private double arcWidth;
    @XmlAttribute
    private double arcHeight;

    public VectorShape() {
    }

    public VectorShape(String id, String layerId, ShapeType shapeType,
                       double x, double y, double width, double height,
                       double arcWidth, double arcHeight) {
        super(id, layerId);
        this.shapeType = shapeType;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
    }

    public ShapeType getShapeType() { return shapeType; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getArcWidth() { return arcWidth; }
    public double getArcHeight() { return arcHeight; }
}