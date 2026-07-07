package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DocumentType {
    BOOK("book"),
    JOURNAL("journal"),
    ARTICLE("article"),
    MANUSCRIPT("manuscript");

    private final String jsonValue;

    DocumentType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String toJsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static DocumentType fromJsonValue(String value) {
        for (DocumentType type : values()) {
            if (type.jsonValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown DocumentType: " + value);
    }
}