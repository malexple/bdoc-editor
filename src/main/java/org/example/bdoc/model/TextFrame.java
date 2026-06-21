package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "textFrame")
@XmlAccessorType(XmlAccessType.FIELD)
public class TextFrame extends BdocObject {

    @XmlAttribute
    private String storyRef;

    public TextFrame() {
    }

    public TextFrame(String id, String layerRef, String storyRef, Geometry geometry) {
        super(id, layerRef, geometry);
        this.storyRef = storyRef;
    }

    public String getStoryRef() {
        return storyRef;
    }
}