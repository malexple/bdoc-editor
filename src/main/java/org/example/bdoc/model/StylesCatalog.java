package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class StylesCatalog {

    private final List<ParagraphStyle> paragraphStyles;
    private final List<CharacterStyle> characterStyles;
    private final List<ObjectStyle> objectStyles;
    private final List<Swatch> swatches;

    @JsonCreator
    public StylesCatalog(
            @JsonProperty("paragraphStyles") List<ParagraphStyle> paragraphStyles,
            @JsonProperty("characterStyles") List<CharacterStyle> characterStyles,
            @JsonProperty("objectStyles") List<ObjectStyle> objectStyles,
            @JsonProperty("swatches") List<Swatch> swatches) {
        this.paragraphStyles = paragraphStyles != null ? paragraphStyles : List.of();
        this.characterStyles = characterStyles != null ? characterStyles : List.of();
        this.objectStyles = objectStyles != null ? objectStyles : List.of();
        this.swatches = swatches != null ? swatches : List.of();
    }

    // Совместимость с Этапом 1.6 — вызов из SampleDocuments.buildStylesCatalog()
    public StylesCatalog(List<ParagraphStyle> paragraphStyles, List<CharacterStyle> characterStyles,
                         List<ObjectStyle> objectStyles) {
        this(paragraphStyles, characterStyles, objectStyles, null);
    }

    // Сохраняем старый конструктор для обратной совместимости
    public StylesCatalog(List<ParagraphStyle> paragraphStyles, List<CharacterStyle> characterStyles) {
        this(paragraphStyles, characterStyles, null, null);
    }

    public static StylesCatalog empty() {
        return new StylesCatalog(List.of(), List.of(), List.of(), List.of());
    }

    public List<ParagraphStyle> getParagraphStyles() { return paragraphStyles; }
    public List<CharacterStyle> getCharacterStyles() { return characterStyles; }
    public List<ObjectStyle> getObjectStyles() { return objectStyles; }
    public List<Swatch> getSwatches() { return swatches; }

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

    public Swatch findSwatch(String id) {
        if (id == null) return null;
        return swatches.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }
}