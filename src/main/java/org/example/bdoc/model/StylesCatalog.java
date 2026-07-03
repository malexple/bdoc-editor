package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class StylesCatalog {
    private final List<ParagraphStyle> paragraphStyles;
    private final List<CharacterStyle> characterStyles;

    @JsonCreator
    public StylesCatalog(
            @JsonProperty("paragraphStyles") List<ParagraphStyle> paragraphStyles,
            @JsonProperty("characterStyles") List<CharacterStyle> characterStyles) {
        this.paragraphStyles = paragraphStyles != null ? paragraphStyles : List.of();
        this.characterStyles = characterStyles != null ? characterStyles : List.of();
    }

    public static StylesCatalog empty() {
        return new StylesCatalog(List.of(), List.of());
    }

    public List<ParagraphStyle> getParagraphStyles() { return paragraphStyles; }
    public List<CharacterStyle> getCharacterStyles() { return characterStyles; }

    public ParagraphStyle findParagraphStyle(String id) {
        return paragraphStyles.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }
}