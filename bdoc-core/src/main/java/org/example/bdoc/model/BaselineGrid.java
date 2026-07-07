package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Декларативное правило шага базовой сетки. Только данные —
 * притягивание строк текста к сетке реализуется в TextWrapper
 * на Этапе 2, здесь фиксируется исключительно модель.
 * startTop/increment — в pt.
 */
public final class BaselineGrid {

    private final boolean enabled;
    private final double startTop;
    private final double increment;

    @JsonCreator
    public BaselineGrid(
            @JsonProperty("enabled") boolean enabled,
            @JsonProperty("startTop") double startTop,
            @JsonProperty("increment") double increment) {
        this.enabled = enabled;
        this.startTop = startTop;
        this.increment = increment;
    }

    public static BaselineGrid disabled() {
        return new BaselineGrid(false, 0.0, 12.0);
    }

    public boolean isEnabled() { return enabled; }
    public double getStartTop() { return startTop; }
    public double getIncrement() { return increment; }
}