package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({TextFrame.class, VectorShape.class})
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class BdocObject {

    @XmlAttribute
    protected String id;

    @XmlAttribute
    protected String layerId;

    protected BdocObject() {
    }

    protected BdocObject(String id, String layerId) {
        this.id = id;
        this.layerId = layerId;
    }

    public String getId() {
        return id;
    }

    public String getLayerId() {
        return layerId;
    }
}