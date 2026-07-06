package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Метаданные ICC/Output Intent (Вопрос 3, Вопрос 6). Парсинг сырых
 * ICC-байтов на Этапе 1 не требуется — достаточно строковых метаданных,
 * идентифицирующих исходное цветовое пространство скана/оборудования.
 */
public final class ColorProfile {

    private final String id;
    private final String name;
    private final String colorSpace; // "CMYK", "RGB", "Gray" и т.д.
    private final String description;

    @JsonCreator
    public ColorProfile(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("colorSpace") String colorSpace,
            @JsonProperty("description") String description) {
        this.id = id;
        this.name = name;
        this.colorSpace = colorSpace;
        this.description = description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getColorSpace() { return colorSpace; }
    public String getDescription() { return description; }
}