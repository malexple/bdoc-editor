package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public final class HeaderFooterRule extends BdocObject {

    private final String zone;
    private final String textTemplate;
    private final String styleRef;

    public HeaderFooterRule(String id, String layerRef, Geometry geometry,
                            String zone, String textTemplate, String styleRef) {
        super(id, layerRef, geometry);
        this.zone = zone;
        this.textTemplate = textTemplate;
        this.styleRef = styleRef;
    }

    public HeaderFooterRule(String id, String layerRef, Geometry geometry,
                            String zone, String textTemplate, String styleRef,
                            String masterSourceId, Set<String> overriddenProperties) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties);
        this.zone = zone;
        this.textTemplate = textTemplate;
        this.styleRef = styleRef;
    }

    @JsonCreator
    public HeaderFooterRule(
            @JsonProperty("id") String id,
            @JsonProperty("layerRef") String layerRef,
            @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("zone") String zone,
            @JsonProperty("textTemplate") String textTemplate,
            @JsonProperty("styleRef") String styleRef,
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
                clipGeometry, maskRef, mask, artifact, artifactType, textWrap, pathData,transform);
        this.zone = zone;
        this.textTemplate = textTemplate;
        this.styleRef = styleRef;
    }

    public String getZone() { return zone; }
    public String getTextTemplate() { return textTemplate; }
    public String getStyleRef() { return styleRef; }

    @Override
    public String getType() { return "HeaderFooterRule"; }
}