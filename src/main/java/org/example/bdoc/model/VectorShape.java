package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class VectorShape extends BdocObject {

    private final String shapeType;

    @JsonCreator
    public VectorShape(
            @JsonProperty("id") String id,
            @JsonProperty("layerRef") String layerRef,
            @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("shapeType") String shapeType) {
        super(id, layerRef, geometry);
        this.shapeType = shapeType;
    }

    public String getShapeType() { return shapeType; }

    @Override
    public String getType() { return "VectorShape"; }
}