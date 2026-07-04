package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public final class VectorShape extends BdocObject {

    private final String shapeType;

    public VectorShape(String id, String layerRef, Geometry geometry, String shapeType) {
        super(id, layerRef, geometry);
        this.shapeType = shapeType;
    }

    public VectorShape(String id, String layerRef, Geometry geometry, String shapeType,
                       String masterSourceId, Set<String> overriddenProperties) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties);
        this.shapeType = shapeType;
    }

    @JsonCreator
    public VectorShape(
            @JsonProperty("id") String id,
            @JsonProperty("layerRef") String layerRef,
            @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("shapeType") String shapeType,
            @JsonProperty("masterSourceId") String masterSourceId,
            @JsonProperty("overriddenProperties") Set<String> overriddenProperties,
            @JsonProperty("visible") Boolean visible) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties, visible);
        this.shapeType = shapeType;
    }

    public String getShapeType() { return shapeType; }

    @Override
    public String getType() { return "VectorShape"; }
}