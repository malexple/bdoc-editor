package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class LayerModel {

    private final String id;
    private final String name;
    private final String role;
    private final boolean visible;
    private final double opacity;

    @JsonCreator
    public LayerModel(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("role") String role,
            @JsonProperty("visible") boolean visible,
            @JsonProperty("opacity") Double opacity) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.visible = visible;
        this.opacity = opacity != null ? opacity : 1.0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public boolean isVisible() { return visible; }
    public double getOpacity() { return opacity; }
}