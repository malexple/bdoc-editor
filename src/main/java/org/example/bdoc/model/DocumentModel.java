package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "document")
@XmlAccessorType(XmlAccessType.FIELD)
public class DocumentModel {

    @XmlAttribute
    private String id;
    @XmlAttribute
    private String title;
    @XmlAttribute
    private DocumentType documentType;

    @XmlElementWrapper(name = "pages")
    @XmlElement(name = "page")
    private List<PageModel> pages = new ArrayList<>();

    @XmlElementWrapper(name = "stories")
    @XmlElement(name = "story")
    private List<StoryModel> stories = new ArrayList<>();

    public DocumentModel() {
    }

    public DocumentModel(String id, String title, DocumentType documentType) {
        this.id = id;
        this.title = title;
        this.documentType = documentType;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public DocumentType getDocumentType() { return documentType; }
    public List<PageModel> getPages() { return pages; }
    public List<StoryModel> getStories() { return stories; }

    public void addPage(PageModel page) { pages.add(page); }
    public void addStory(StoryModel story) { stories.add(story); }
}