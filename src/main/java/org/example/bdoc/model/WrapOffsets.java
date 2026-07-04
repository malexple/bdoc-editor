package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class WrapOffsets {

    private final double top;
    private final double bottom;
    private final double left;
    private final double right;

    @JsonCreator
    public WrapOffsets(
            @JsonProperty("top") double top,
            @JsonProperty("bottom") double bottom,
            @JsonProperty("left") double left,
            @JsonProperty("right") double right) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
    }

    public static WrapOffsets zero() {
        return new WrapOffsets(0.0, 0.0, 0.0, 0.0);
    }

    public static WrapOffsets uniform(double value) {
        return new WrapOffsets(value, value, value, value);
    }

    public double getTop() { return top; }
    public double getBottom() { return bottom; }
    public double getLeft() { return left; }
    public double getRight() { return right; }
}