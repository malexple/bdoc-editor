package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "textFrame")
@XmlAccessorType(XmlAccessType.FIELD)
public class TextFrame extends BdocObject {

    @XmlAttribute
    private String storyId;
    @XmlAttribute
    private double x;
    @XmlAttribute
    private double y;
    @XmlAttribute
    private double width;
    @XmlAttribute
    private double height;

    public TextFrame() {
    }

    public TextFrame(String id, String layerId, String storyId,
                     double x, double y, double width, double height) {
        super(id, layerId);
        this.storyId = storyId;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getStoryId() { return storyId; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}