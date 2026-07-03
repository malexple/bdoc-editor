package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Растровый фрейм. Используется как для иллюстраций,
 * так и как временная подложка-скан оригинала при реставрации.
 */
public final class ImageFrame extends BdocObject {

    private final String assetRef;

    @JsonCreator
    public ImageFrame(
            @JsonProperty("id") String id,
            @JsonProperty("layerRef") String layerRef,
            @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("assetRef") String assetRef) {
        super(id, layerRef, geometry);
        this.assetRef = assetRef;
    }

    public String getAssetRef() { return assetRef; }

    @Override
    public String getType() { return "ImageFrame"; }
}