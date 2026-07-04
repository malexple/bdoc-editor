package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

public final class TableFrame extends BdocObject {

    private final int rowCount;
    private final int columnCount;
    private final List<TableRow> rows;
    private final List<TableColumn> columns;
    private final List<TableCell> cells;

    public TableFrame(String id, String layerRef, Geometry geometry,
                      int rowCount, int columnCount,
                      List<TableRow> rows, List<TableColumn> columns, List<TableCell> cells) {
        super(id, layerRef, geometry);
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.rows = rows != null ? rows : List.of();
        this.columns = columns != null ? columns : List.of();
        this.cells = cells != null ? cells : List.of();
    }

    @JsonCreator
    public TableFrame(
            @JsonProperty("id") String id,
            @JsonProperty("layerRef") String layerRef,
            @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("rowCount") int rowCount,
            @JsonProperty("columnCount") int columnCount,
            @JsonProperty("rows") List<TableRow> rows,
            @JsonProperty("columns") List<TableColumn> columns,
            @JsonProperty("cells") List<TableCell> cells,
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
                clipGeometry, maskRef, mask, artifact, artifactType, textWrap, pathData, transform);
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.rows = rows != null ? rows : List.of();
        this.columns = columns != null ? columns : List.of();
        this.cells = cells != null ? cells : List.of();
    }

    public int getRowCount() { return rowCount; }
    public int getColumnCount() { return columnCount; }
    public List<TableRow> getRows() { return rows; }
    public List<TableColumn> getColumns() { return columns; }
    public List<TableCell> getCells() { return cells; }

    @Override
    public String getType() { return "TableFrame"; }
}