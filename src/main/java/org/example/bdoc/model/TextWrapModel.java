package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Обтекание текста задаётся на объекте-препятствии (ImageFrame, Group,
 * VectorShape), а не на TextFrame — TextWrapper на Этапе 2 будет опрашивать
 * все объекты страницы на предмет непустого textWrap и строить карту
 * "запретных зон" для верстки строк.
 *
 * mode: "none" | "boundingBox" | "contour". Для "contour" используется
 * pathData; для "boundingBox" pathData игнорируется и достаточно Geometry
 * самого объекта, расширенной на offsets.
 */
public final class TextWrapModel {

    private final String mode;
    private final WrapOffsets offsets;
    private final PathModel pathData;

    @JsonCreator
    public TextWrapModel(
            @JsonProperty("mode") String mode,
            @JsonProperty("offsets") WrapOffsets offsets,
            @JsonProperty("pathData") PathModel pathData) {
        this.mode = mode != null ? mode : "none";
        this.offsets = offsets != null ? offsets : WrapOffsets.zero();
        this.pathData = pathData;
    }

    public static TextWrapModel disabled() {
        return new TextWrapModel("none", WrapOffsets.zero(), null);
    }

    public String getMode() { return mode; }
    public WrapOffsets getOffsets() { return offsets; }
    public PathModel getPathData() { return pathData; }

    public boolean isEnabled() { return !"none".equals(mode); }
}