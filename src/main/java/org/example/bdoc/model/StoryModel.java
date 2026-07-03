package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class StoryModel {

    private final String id;
    private final List<Paragraph> paragraphs;

    @JsonCreator
    public StoryModel(
            @JsonProperty("id") String id,
            @JsonProperty("paragraphs") List<Paragraph> paragraphs) {
        this.id = id;
        this.paragraphs = paragraphs != null ? paragraphs : List.of();
    }

    public String getId() { return id; }
    public List<Paragraph> getParagraphs() { return paragraphs; }

    @JsonIgnore
    public String getJoinedText() {
        return paragraphs.stream()
                .map(Paragraph::getPlainText)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}