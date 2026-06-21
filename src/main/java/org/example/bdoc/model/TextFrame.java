package org.example.bdoc.model;

public record TextFrame(
        String id,
        String layerId,
        String storyId,
        double x,
        double y,
        double width,
        double height
) implements BdocObject {
}