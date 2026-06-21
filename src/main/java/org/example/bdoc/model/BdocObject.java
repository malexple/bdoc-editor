package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({TextFrame.class, VectorShape.class})
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class BdocObject {

    @XmlAttribute
    protected String id;

    @XmlAttribute
    protected String layerRef;

    @XmlElement(name = "geometry")
    protected Geometry geometry;

    protected BdocObject() {
    }

    protected BdocObject(String id, String layerRef, Geometry geometry) {
        this.id = id;
        this.layerRef = layerRef;
        this.geometry = geometry;
    }

    public String getId() {
        return id;
    }

    public String getLayerRef() {
        return layerRef;
    }

    public Geometry getGeometry() {
        return geometry;
    }
}