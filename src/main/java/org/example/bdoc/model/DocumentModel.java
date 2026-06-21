package org.example.bdoc.model;

import java.util.ArrayList;
import java.util.List;

public class DocumentModel {
    private String id;
    private String title;
    private DocumentType documentType;
    private final List<PageModel> pages = new ArrayList<>();
    private final List<StoryModel> stories = new ArrayList<>();

    public DocumentModel(String id, String title, DocumentType documentType) {
        this.id = id;
        this.title = title;
        this.documentType = documentType;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public List<PageModel> getPages() {
        return pages;
    }

    public List<StoryModel> getStories() {
        return stories;
    }

    public void addPage(PageModel page) {
        pages.add(page);
    }

    public void addStory(StoryModel story) {
        stories.add(story);
    }
}