package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextFrame.class, name = "TextFrame"),
        @JsonSubTypes.Type(value = ImageFrame.class, name = "ImageFrame"),
        @JsonSubTypes.Type(value = VectorShape.class, name = "VectorShape")
})
public abstract class BdocObject {

    protected final String id;
    protected final String layerRef;
    protected final Geometry geometry;

    protected BdocObject(String id, String layerRef, Geometry geometry) {
        this.id = id;
        this.layerRef = layerRef;
        this.geometry = geometry;
    }

    public String getId() { return id; }
    public String getLayerRef() { return layerRef; }
    public Geometry getGeometry() { return geometry; }

    public abstract String getType();
}