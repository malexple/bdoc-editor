package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ManifestTemplateEntry {
    private final String id;
    private final String file;

    @JsonCreator
    public ManifestTemplateEntry(
            @JsonProperty("id") String id,
            @JsonProperty("file") String file) {
        this.id = id;
        this.file = file;
    }

    public String getId() { return id; }
    public String getFile() { return file; }
}