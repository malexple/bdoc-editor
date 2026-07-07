package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class TableRow {
    private final double heightRatio;

    @JsonCreator
    public TableRow(@JsonProperty("heightRatio") double heightRatio) {
        this.heightRatio = heightRatio;
    }

    public double getHeightRatio() { return heightRatio; }
}