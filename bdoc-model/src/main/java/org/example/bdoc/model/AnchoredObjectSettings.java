package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Декларативная заготовка привязки объекта к слову в тексте (Этап 1.6).
 * Реальный пересчёт X/Y при редактировании текста откладывается на Этап 2
 * (когда TextWrapper станет полноценным Layout Manager). Сейчас координаты
 * объекта хранятся как обычные абсолютные X/Y в Geometry — offsetX/offsetY
 * "спят" в файле и не участвуют в рендере.
 *
 * targetSpanIndex — не UUID конкретного Span (у Span нет id, это дешёвый
 * иммутабельный текстовый отрезок), а порядковый индекс span'а при плоском
 * обходе всех параграфов истории storyRef.
 */
public final class AnchoredObjectSettings {

    private final boolean enabled;
    private final String storyRef;
    private final int targetSpanIndex;
    private final String positionMode; // "inline" | "custom"
    private final Double offsetX;
    private final Double offsetY;

    @JsonCreator
    public AnchoredObjectSettings(
            @JsonProperty("enabled") boolean enabled,
            @JsonProperty("storyRef") String storyRef,
            @JsonProperty("targetSpanIndex") int targetSpanIndex,
            @JsonProperty("positionMode") String positionMode,
            @JsonProperty("offsetX") Double offsetX,
            @JsonProperty("offsetY") Double offsetY) {
        this.enabled = enabled;
        this.storyRef = storyRef;
        this.targetSpanIndex = targetSpanIndex;
        this.positionMode = positionMode;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public boolean isEnabled() { return enabled; }
    public String getStoryRef() { return storyRef; }
    public int getTargetSpanIndex() { return targetSpanIndex; }
    public String getPositionMode() { return positionMode; }
    public Double getOffsetX() { return offsetX; }
    public Double getOffsetY() { return offsetY; }
}