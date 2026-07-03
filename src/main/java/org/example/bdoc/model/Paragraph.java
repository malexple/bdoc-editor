package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Paragraph {

    private final String role;
    private final String styleRef;
    private final String text;

    @JsonCreator
    public Paragraph(
            @JsonProperty("role") String role,
            @JsonProperty("styleRef") String styleRef,
            @JsonProperty("text") String text) {
        this.role = role;
        this.styleRef = styleRef;
        this.text = text;
    }

    public String getRole() { return role; }
    public String getStyleRef() { return styleRef; }
    public String getText() { return text; }
}