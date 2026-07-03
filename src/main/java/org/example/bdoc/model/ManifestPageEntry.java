package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ManifestPageEntry {
    private final int index;
    private final String id;
    private final String file;

    @JsonCreator
    public ManifestPageEntry(
            @JsonProperty("index") int index,
            @JsonProperty("id") String id,
            @JsonProperty("file") String file) {
        this.index = index;
        this.id = id;
        this.file = file;
    }

    public int getIndex() { return index; }
    public String getId() { return id; }
    public String getFile() { return file; }
}