package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class Manifest {
    private final String id;
    private final String title;
    private final String documentType;
    private final String version;
    private final String language;
    private final List<ManifestPageEntry> pages;
    private final List<ManifestStoryEntry> stories;
    private final List<ManifestTemplateEntry> templates;

    @JsonCreator
    public Manifest(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("documentType") String documentType,
            @JsonProperty("version") String version,
            @JsonProperty("language") String language,
            @JsonProperty("pages") List<ManifestPageEntry> pages,
            @JsonProperty("stories") List<ManifestStoryEntry> stories,
            @JsonProperty("templates") List<ManifestTemplateEntry> templates) {
        this.id = id;
        this.title = title;
        this.documentType = documentType;
        this.version = version != null ? version : "0.1-composite";
        this.language = language;
        this.pages = pages != null ? pages : List.of();
        this.stories = stories != null ? stories : List.of();
        this.templates = templates != null ? templates : List.of();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDocumentType() { return documentType; }
    public String getVersion() { return version; }
    public String getLanguage() { return language; }
    public List<ManifestPageEntry> getPages() { return pages; }
    public List<ManifestStoryEntry> getStories() { return stories; }
    public List<ManifestTemplateEntry> getTemplates() { return templates; }
}