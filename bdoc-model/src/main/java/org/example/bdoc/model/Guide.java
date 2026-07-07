package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Именованная направляющая линия. Хранится в pt.
 * orientation: "horizontal" | "vertical".
 * Горизонтальная направляющая — position — это Y от верха страницы.
 * Вертикальная направляющая — position — это X от левого края страницы.
 */
public final class Guide {

    private final String id;
    private final String orientation;
    private final double position;
    private final String color;
    private final boolean snap;

    @JsonCreator
    public Guide(
            @JsonProperty("id") String id,
            @JsonProperty("orientation") String orientation,
            @JsonProperty("position") double position,
            @JsonProperty("color") String color,
            @JsonProperty("snap") Boolean snap) {
        this.id = id;
        this.orientation = orientation;
        this.position = position;
        this.color = color != null ? color : "#3B82F6";
        this.snap = snap != null ? snap : true;
    }

    public String getId() { return id; }
    public String getOrientation() { return orientation; }
    public double getPosition() { return position; }
    public String getColor() { return color; }
    public boolean isSnap() { return snap; }
}