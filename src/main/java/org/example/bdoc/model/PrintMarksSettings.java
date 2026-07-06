package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Декларативная конфигурация технических меток (Вопрос 4, Вариант Б).
 * Сами метки НЕ хранятся как объекты страницы — PageRenderer/экспортёр
 * рисует их программно по геометрии bleedMargin. Разрешается целиком
 * (не по каждому полю) по трёхуровневому каскаду Page -> MasterPage -> Manifest.
 */
public final class PrintMarksSettings {

    private final boolean showCropMarks;
    private final boolean showRegistrationMarks;
    private final boolean showColorBars;

    @JsonCreator
    public PrintMarksSettings(
            @JsonProperty("showCropMarks") boolean showCropMarks,
            @JsonProperty("showRegistrationMarks") boolean showRegistrationMarks,
            @JsonProperty("showColorBars") boolean showColorBars) {
        this.showCropMarks = showCropMarks;
        this.showRegistrationMarks = showRegistrationMarks;
        this.showColorBars = showColorBars;
    }

    public static PrintMarksSettings disabled() {
        return new PrintMarksSettings(false, false, false);
    }

    public static PrintMarksSettings all() {
        return new PrintMarksSettings(true, true, true);
    }

    public boolean isShowCropMarks() { return showCropMarks; }
    public boolean isShowRegistrationMarks() { return showRegistrationMarks; }
    public boolean isShowColorBars() { return showColorBars; }
}