package org.example.bdoc.model;

public class LayerModel {
    private final String id;
    private final String name;
    private final LayerRole role;
    private final boolean visible;
    private final int zIndex;

    public LayerModel(String id, String name, LayerRole role, boolean visible, int zIndex) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.visible = visible;
        this.zIndex = zIndex;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LayerRole getRole() {
        return role;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getZIndex() {
        return zIndex;
    }
}