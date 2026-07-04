package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public final class ImageFrame extends BdocObject {

    private final String assetRef;

    public ImageFrame(String id, String layerRef, Geometry geometry, String assetRef) {
        super(id, layerRef, geometry);
        this.assetRef = assetRef;
    }

    public ImageFrame(String id, String layerRef, Geometry geometry, String assetRef,
                      String masterSourceId, Set<String> overriddenProperties) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties);
        this.assetRef = assetRef;
    }

    @JsonCreator
    public ImageFrame(
            @JsonProperty("id") String id,
            @JsonProperty("layerRef") String layerRef,
            @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("assetRef") String assetRef,
            @JsonProperty("masterSourceId") String masterSourceId,
            @JsonProperty("overriddenProperties") Set<String> overriddenProperties,
            @JsonProperty("visible") Boolean visible,
            @JsonProperty("clipGeometry") Geometry clipGeometry,
            @JsonProperty("maskRef") String maskRef,
            @JsonProperty("mask") Boolean mask,
            @JsonProperty("artifact") Boolean artifact,
            @JsonProperty("artifactType") String artifactType,
            @JsonProperty("textWrap") TextWrapModel textWrap,
            @JsonProperty("pathData") PathModel pathData,
            @JsonProperty("transform") TransformModel transform) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties, visible,
                clipGeometry, maskRef, mask, artifact, artifactType, textWrap, pathData, transform);
        this.assetRef = assetRef;
    }

    public String getAssetRef() { return assetRef; }

    @Override
    public String getType() { return "ImageFrame"; }
}