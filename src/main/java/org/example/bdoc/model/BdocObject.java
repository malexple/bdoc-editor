package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Set;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextFrame.class, name = "TextFrame"),
        @JsonSubTypes.Type(value = ImageFrame.class, name = "ImageFrame"),
        @JsonSubTypes.Type(value = VectorShape.class, name = "VectorShape"),
        @JsonSubTypes.Type(value = HeaderFooterRule.class, name = "HeaderFooterRule")
})
public abstract class BdocObject {

    protected final String id;
    protected final String layerRef;
    protected final Geometry geometry;
    protected final String masterSourceId;
    protected final Set<String> overriddenProperties;

    /** Обычный, самостоятельный объект страницы — не связан с мастером. */
    protected BdocObject(String id, String layerRef, Geometry geometry) {
        this(id, layerRef, geometry, null, null);
    }

    /**
     * Объект — частичный override объекта мастер-страницы.
     * masterSourceId — id объекта на MasterPage, от которого унаследованы свойства.
     * overriddenProperties — имена полей, которые локально переопределены
     * (например, "geometry"); все остальные поля берёт PageRenderer с мастера.
     */
    protected BdocObject(String id, String layerRef, Geometry geometry,
                         String masterSourceId, Set<String> overriddenProperties) {
        this.id = id;
        this.layerRef = layerRef;
        this.geometry = geometry;
        this.masterSourceId = masterSourceId;
        this.overriddenProperties = overriddenProperties != null ? overriddenProperties : Set.of();
    }

    public String getId() { return id; }
    public String getLayerRef() { return layerRef; }
    public Geometry getGeometry() { return geometry; }
    public String getMasterSourceId() { return masterSourceId; }
    public Set<String> getOverriddenProperties() { return overriddenProperties; }
    public boolean isMasterOverride() { return masterSourceId != null; }

    public abstract String getType();
}