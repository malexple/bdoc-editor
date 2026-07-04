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
        @JsonSubTypes.Type(value = HeaderFooterRule.class, name = "HeaderFooterRule"),
        @JsonSubTypes.Type(value = Group.class, name = "Group"),
        @JsonSubTypes.Type(value = LineObject.class, name = "LineObject"),
        @JsonSubTypes.Type(value = TableFrame.class, name = "TableFrame")
})
public abstract class BdocObject {

    protected final String id;
    protected final String layerRef;
    protected final Geometry geometry;
    protected final String masterSourceId;
    protected final Set<String> overriddenProperties;
    protected boolean visible;

    protected final Geometry clipGeometry;
    protected final String maskRef;
    protected final boolean mask;
    protected final boolean artifact;
    protected final String artifactType;
    protected final TextWrapModel textWrap;

    // Произвольный контур (маска, кастомная форма VectorShape). Не связан с
    // textWrap.pathData — то поле описывает контур обтекания текста вокруг
    // объекта, а это поле описывает форму самого объекта, когда он выступает
    // как маска (mask=true) или как контурная фигура. null по умолчанию,
    // а не PathModel.primitive() — так проще отличить "контур не задан"
    // от "контур задан, но примитивный", что важно для сериализации.
    protected final PathModel pathData;

    protected BdocObject(String id, String layerRef, Geometry geometry) {
        this(id, layerRef, geometry, null, null, true, null, null, false, false, null, null, null);
    }

    protected BdocObject(String id, String layerRef, Geometry geometry,
                         String masterSourceId, Set<String> overriddenProperties) {
        this(id, layerRef, geometry, masterSourceId, overriddenProperties, true, null, null, false, false, null, null, null);
    }

    protected BdocObject(String id, String layerRef, Geometry geometry,
                         String masterSourceId, Set<String> overriddenProperties, Boolean visible) {
        this(id, layerRef, geometry, masterSourceId, overriddenProperties, visible, null, null, false, false, null, null, null);
    }

    protected BdocObject(String id, String layerRef, Geometry geometry,
                         String masterSourceId, Set<String> overriddenProperties, Boolean visible,
                         Geometry clipGeometry, String maskRef, Boolean mask,
                         Boolean artifact, String artifactType, TextWrapModel textWrap) {
        this(id, layerRef, geometry, masterSourceId, overriddenProperties, visible,
                clipGeometry, maskRef, mask, artifact, artifactType, textWrap, null);
    }

    protected BdocObject(String id, String layerRef, Geometry geometry,
                         String masterSourceId, Set<String> overriddenProperties, Boolean visible,
                         Geometry clipGeometry, String maskRef, Boolean mask,
                         Boolean artifact, String artifactType, TextWrapModel textWrap,
                         PathModel pathData) {
        this.id = id;
        this.layerRef = layerRef;
        this.geometry = geometry;
        this.masterSourceId = masterSourceId;
        this.overriddenProperties = overriddenProperties != null ? overriddenProperties : Set.of();
        this.visible = visible != null ? visible : true;
        this.clipGeometry = clipGeometry;
        this.maskRef = maskRef;
        this.mask = mask != null ? mask : false;
        this.artifact = artifact != null ? artifact : false;
        this.artifactType = artifactType;
        this.textWrap = textWrap != null ? textWrap : TextWrapModel.disabled();
        this.pathData = pathData;
    }

    public String getId() { return id; }
    public String getLayerRef() { return layerRef; }
    public Geometry getGeometry() { return geometry; }
    public String getMasterSourceId() { return masterSourceId; }
    public Set<String> getOverriddenProperties() { return overriddenProperties; }
    public boolean isMasterOverride() { return masterSourceId != null; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public Geometry getClipGeometry() { return clipGeometry; }
    public String getMaskRef() { return maskRef; }
    public boolean isMask() { return mask; }
    public boolean isArtifact() { return artifact; }
    public String getArtifactType() { return artifactType; }
    public TextWrapModel getTextWrap() { return textWrap; }
    public PathModel getPathData() { return pathData; }

    public abstract String getType();
}