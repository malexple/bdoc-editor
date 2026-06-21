package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "layer")
@XmlAccessorType(XmlAccessType.FIELD)
public class LayerModel {

    @XmlAttribute
    private String id;
    @XmlAttribute
    private String name;
    @XmlAttribute
    private LayerRole role;
    @XmlAttribute
    private boolean visible;
    @XmlAttribute
    private int zIndex;

    public LayerModel() {
    }

    public LayerModel(String id, String name, LayerRole role, boolean visible, int zIndex) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.visible = visible;
        this.zIndex = zIndex;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public LayerRole getRole() { return role; }
    public boolean isVisible() { return visible; }
    public int getZIndex() { return zIndex; }
}