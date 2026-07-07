package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ссылка внутри текстового Span — гиперссылка на внешний URL, перекрёстная
 * ссылка на объект страницы или переход на другую страницу документа.
 * Единая модель с targetType избегает разрастания классов и упрощает
 * будущее расширение (например, targetType="footnote" или "story").
 */
public final class ReferenceModel {

    private final String targetType; // "url" | "object" | "page"
    private final String target;

    @JsonCreator
    public ReferenceModel(
            @JsonProperty("targetType") String targetType,
            @JsonProperty("target") String target) {
        this.targetType = targetType;
        this.target = target;
    }

    public String getTargetType() { return targetType; }
    public String getTarget() { return target; }
}