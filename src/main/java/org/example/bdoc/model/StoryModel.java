package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

@XmlRootElement(name = "story")
@XmlAccessorType(XmlAccessType.FIELD)
public class StoryModel {

    @XmlAttribute
    private String id;

    @XmlValue
    private String text;

    public StoryModel() {
    }

    public StoryModel(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public String getId() { return id; }
    public String getText() { return text; }
}