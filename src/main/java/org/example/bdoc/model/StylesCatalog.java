package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class StylesCatalog {

    private final List<ParagraphStyle> paragraphStyles;
    private final List<CharacterStyle> characterStyles;
    private final List<ObjectStyle> objectStyles;

    @JsonCreator
    public StylesCatalog(
            @JsonProperty("paragraphStyles") List<ParagraphStyle> paragraphStyles,
            @JsonProperty("characterStyles") List<CharacterStyle> characterStyles,
            @JsonProperty("objectStyles") List<ObjectStyle> objectStyles) {
        this.paragraphStyles = paragraphStyles != null ? paragraphStyles : List.of();
        this.characterStyles = characterStyles != null ? characterStyles : List.of();
        this.objectStyles = objectStyles != null ? objectStyles : List.of();
    }

    // Сохраняем старый конструктор для обратной совместимости всех вызовов в SampleDocuments
    public StylesCatalog(List<ParagraphStyle> paragraphStyles, List<CharacterStyle> characterStyles) {
        this(paragraphStyles, characterStyles, null);
    }

    public static StylesCatalog empty() {
        return new StylesCatalog(List.of(), List.of(), List.of());
    }

    public List<ParagraphStyle> getParagraphStyles() { return paragraphStyles; }
    public List<CharacterStyle> getCharacterStyles() { return characterStyles; }
    public List<ObjectStyle> getObjectStyles() { return objectStyles; }

    public ParagraphStyle findParagraphStyle(String id) {
        return paragraphStyles.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }

    public CharacterStyle findCharacterStyle(String id) {
        return characterStyles.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }

    public ObjectStyle findObjectStyle(String id) {
        if (id == null) return null;
        return objectStyles.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }
}