package org.example.bdoc.model;

public record VectorShape(
        String id,
        String layerId,
        ShapeType shapeType,
        double x,
        double y,
        double width,
        double height,
        double arcWidth,
        double arcHeight
) implements BdocObject {
}