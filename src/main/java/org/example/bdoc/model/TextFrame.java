package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public final class TextFrame extends BdocObject {

    private final String storyRef;

    public TextFrame(String id, String layerRef, Geometry geometry, String storyRef) {
        super(id, layerRef, geometry);
        this.storyRef = storyRef;
    }

    public TextFrame(String id, String layerRef, Geometry geometry, String storyRef,
                     String masterSourceId, Set<String> overriddenProperties) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties);
        this.storyRef = storyRef;
    }

    @JsonCreator
    public TextFrame(
            @JsonProperty("id") String id,
            @JsonProperty("layerRef") String layerRef,
            @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("storyRef") String storyRef,
            @JsonProperty("masterSourceId") String masterSourceId,
            @JsonProperty("overriddenProperties") Set<String> overriddenProperties,
            @JsonProperty("visible") Boolean visible) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties, visible);
        this.storyRef = storyRef;
    }

    public String getStoryRef() { return storyRef; }

    @Override
    public String getType() { return "TextFrame"; }
}