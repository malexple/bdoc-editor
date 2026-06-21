package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "vectorShape")
@XmlAccessorType(XmlAccessType.FIELD)
public class VectorShape extends BdocObject {

    @XmlAttribute(name = "type")
    private ShapeType shapeType;

    public VectorShape() {
    }

    public VectorShape(String id, String layerRef, ShapeType shapeType, Geometry geometry) {
        super(id, layerRef, geometry);
        this.shapeType = shapeType;
    }

    public ShapeType getShapeType() {
        return shapeType;
    }
}