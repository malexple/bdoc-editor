package org.example.bdoc.model;

import java.util.ArrayList;
import java.util.List;

public class PageModel {
    private final String id;
    private final int index;
    private final double width;
    private final double height;
    private final List<LayerModel> layers = new ArrayList<>();
    private final List<BdocObject> objects = new ArrayList<>();

    public PageModel(String id, int index, double width, double height) {
        this.id = id;
        this.index = index;
        this.width = width;
        this.height = height;
    }

    public String getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public List<LayerModel> getLayers() {
        return layers;
    }

    public List<BdocObject> getObjects() {
        return objects;
    }

    public void addLayer(LayerModel layer) {
        layers.add(layer);
    }

    public void addObject(BdocObject object) {
        objects.add(object);
    }
}