package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class Paragraph {

    private final String role;
    private final String styleRef;
    private final List<Span> spans;

    @JsonCreator
    public Paragraph(
            @JsonProperty("role") String role,
            @JsonProperty("styleRef") String styleRef,
            @JsonProperty("spans") List<Span> spans) {
        this.role = role;
        this.styleRef = styleRef;
        this.spans = spans != null ? spans : List.of();
    }

    /**
     * Удобный конструктор для параграфа без inline-разметки —
     * весь текст одним span'ом без characterStyleRef.
     */
    public Paragraph(String role, String styleRef, String plainText) {
        this(role, styleRef, List.of(Span.plain(plainText)));
    }

    public String getRole() { return role; }
    public String getStyleRef() { return styleRef; }
    public List<Span> getSpans() { return spans; }

    @JsonIgnore
    public String getPlainText() {
        StringBuilder sb = new StringBuilder();
        for (Span span : spans) {
            sb.append(span.getText());
        }
        return sb.toString();
    }
}