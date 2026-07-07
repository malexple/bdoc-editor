package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LayerRole {
    BACKGROUND("background"),
    DECORATION("decoration"),
    TEXT("text"),
    IMAGE("image"),
    HEADER_FOOTER("header-footer"),
    ANNOTATION("annotation");

    private final String jsonValue;

    LayerRole(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String toJsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static LayerRole fromJsonValue(String value) {
        for (LayerRole role : values()) {
            if (role.jsonValue.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown LayerRole: " + value);
    }
}