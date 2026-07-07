package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Декларативная колонная сетка. v0.1 покрывает равные колонки
 * с одинаковым gutter; customGuides — архитектурный крючок под
 * неравные колонки в будущих версиях (рендерер v0.1 их игнорирует).
 * Все значения в pt.
 */
public final class GridModel {

    private final boolean enabled;
    private final int columnCount;
    private final double gutter;
    private final List<Double> customGuides;

    @JsonCreator
    public GridModel(
            @JsonProperty("enabled") boolean enabled,
            @JsonProperty("columnCount") int columnCount,
            @JsonProperty("gutter") double gutter,
            @JsonProperty("customGuides") List<Double> customGuides) {
        this.enabled = enabled;
        this.columnCount = columnCount;
        this.gutter = gutter;
        this.customGuides = customGuides != null ? customGuides : List.of();
    }

    public static GridModel disabled() {
        return new GridModel(false, 1, 0.0, List.of());
    }

    public boolean isEnabled() { return enabled; }
    public int getColumnCount() { return columnCount; }
    public double getGutter() { return gutter; }
    public List<Double> getCustomGuides() { return customGuides; }
}