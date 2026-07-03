package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class TextFrame extends BdocObject {

    private final String storyRef;

    @JsonCreator
    public TextFrame(
            @JsonProperty("id") String id,
            @JsonProperty("layerRef") String layerRef,
            @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("storyRef") String storyRef) {
        super(id, layerRef, geometry);
        this.storyRef = storyRef;
    }

    public String getStoryRef() { return storyRef; }

    @Override
    public String getType() { return "TextFrame"; }
}