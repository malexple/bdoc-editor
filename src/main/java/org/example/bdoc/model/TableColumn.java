package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class TableColumn {
    private final double widthRatio;

    @JsonCreator
    public TableColumn(@JsonProperty("widthRatio") double widthRatio) {
        this.widthRatio = widthRatio;
    }

    public double getWidthRatio() { return widthRatio; }
}