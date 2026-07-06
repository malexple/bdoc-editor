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
    private final List<ColorProfile> colorProfiles;
    private final String outputIntentProfileRef;

    // Этап 1.8: глобальный уровень каскада prepress-геометрии.
    private final Double defaultBleedMargin;
    private final Double defaultSafetyMargin;
    private final PrintMarksSettings defaultPrintMarksSettings;

    @JsonCreator
    public Manifest(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("documentType") String documentType,
            @JsonProperty("version") String version,
            @JsonProperty("language") String language,
            @JsonProperty("pages") List<ManifestPageEntry> pages,
            @JsonProperty("stories") List<ManifestStoryEntry> stories,
            @JsonProperty("templates") List<ManifestTemplateEntry> templates,
            @JsonProperty("colorProfiles") List<ColorProfile> colorProfiles,
            @JsonProperty("outputIntentProfileRef") String outputIntentProfileRef,
            @JsonProperty("defaultBleedMargin") Double defaultBleedMargin,
            @JsonProperty("defaultSafetyMargin") Double defaultSafetyMargin,
            @JsonProperty("defaultPrintMarksSettings") PrintMarksSettings defaultPrintMarksSettings) {
        this.id = id;
        this.title = title;
        this.documentType = documentType;
        this.version = version != null ? version : "0.1-composite";
        this.language = language;
        this.pages = pages != null ? pages : List.of();
        this.stories = stories != null ? stories : List.of();
        this.templates = templates != null ? templates : List.of();
        this.colorProfiles = colorProfiles != null ? colorProfiles : List.of();
        this.outputIntentProfileRef = outputIntentProfileRef;
        this.defaultBleedMargin = defaultBleedMargin;
        this.defaultSafetyMargin = defaultSafetyMargin;
        this.defaultPrintMarksSettings = defaultPrintMarksSettings;
    }

    // Совместимость с Этапом 1.7
    public Manifest(
            String id, String title, String documentType, String version, String language,
            List<ManifestPageEntry> pages, List<ManifestStoryEntry> stories, List<ManifestTemplateEntry> templates,
            List<ColorProfile> colorProfiles, String outputIntentProfileRef) {
        this(id, title, documentType, version, language, pages, stories, templates,
                colorProfiles, outputIntentProfileRef, null, null, null);
    }

    // Совместимость со старым вызовом в BdocContainerSerializer.Writer.finish(...)
    public Manifest(
            String id, String title, String documentType, String version, String language,
            List<ManifestPageEntry> pages, List<ManifestStoryEntry> stories, List<ManifestTemplateEntry> templates) {
        this(id, title, documentType, version, language, pages, stories, templates, null, null, null, null, null);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDocumentType() { return documentType; }
    public String getVersion() { return version; }
    public String getLanguage() { return language; }
    public List<ManifestPageEntry> getPages() { return pages; }
    public List<ManifestStoryEntry> getStories() { return stories; }
    public List<ManifestTemplateEntry> getTemplates() { return templates; }
    public List<ColorProfile> getColorProfiles() { return colorProfiles; }
    public String getOutputIntentProfileRef() { return outputIntentProfileRef; }
    public Double getDefaultBleedMargin() { return defaultBleedMargin; }
    public Double getDefaultSafetyMargin() { return defaultSafetyMargin; }
    public PrintMarksSettings getDefaultPrintMarksSettings() { return defaultPrintMarksSettings; }

    public ColorProfile findColorProfile(String id) {
        if (id == null) return null;
        return colorProfiles.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }
}