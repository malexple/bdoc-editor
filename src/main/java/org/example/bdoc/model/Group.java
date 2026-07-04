package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

public final class Group extends BdocObject {

    private final List<String> childObjectIds;

    public Group(String id, String layerRef, Geometry geometry, List<String> childObjectIds) {
        super(id, layerRef, geometry);
        this.childObjectIds = childObjectIds != null ? childObjectIds : List.of();
    }

    @JsonCreator
    public Group(
            @JsonProperty("id") String id,
            @JsonProperty("layerRef") String layerRef,
            @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("childObjectIds") List<String> childObjectIds,
            @JsonProperty("masterSourceId") String masterSourceId,
            @JsonProperty("overriddenProperties") Set<String> overriddenProperties,
            @JsonProperty("visible") Boolean visible,
            @JsonProperty("clipGeometry") Geometry clipGeometry,
            @JsonProperty("maskRef") String maskRef,
            @JsonProperty("mask") Boolean mask,
            @JsonProperty("artifact") Boolean artifact,
            @JsonProperty("artifactType") String artifactType,
            @JsonProperty("textWrap") TextWrapModel textWrap,
            @JsonProperty("pathData") PathModel pathData) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties, visible,
                clipGeometry, maskRef, mask, artifact, artifactType, textWrap, pathData);
        this.childObjectIds = childObjectIds != null ? childObjectIds : List.of();
    }

    public List<String> getChildObjectIds() { return childObjectIds; }

    @Override
    public String getType() { return "Group"; }
}