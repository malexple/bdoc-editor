package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "page")
@XmlAccessorType(XmlAccessType.FIELD)
public class PageModel {

    @XmlAttribute
    private String id;
    @XmlAttribute
    private int index;
    @XmlAttribute
    private double width;
    @XmlAttribute
    private double height;

    @XmlElementWrapper(name = "layers")
    @XmlElementRef
    private List<LayerModel> layers = new ArrayList<>();

    @XmlElementWrapper(name = "objects")
    @XmlElementRefs({
            @XmlElementRef(type = TextFrame.class),
            @XmlElementRef(type = VectorShape.class)
    })
    private List<BdocObject> objects = new ArrayList<>();

    public PageModel() {
    }

    public PageModel(String id, int index, double width, double height) {
        this.id = id;
        this.index = index;
        this.width = width;
        this.height = height;
    }

    public String getId() { return id; }
    public int getIndex() { return index; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public List<LayerModel> getLayers() { return layers; }
    public List<BdocObject> getObjects() { return objects; }

    public void addLayer(LayerModel layer) { layers.add(layer); }
    public void addObject(BdocObject object) { objects.add(object); }
}