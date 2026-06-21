package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class StoryModel {

    @XmlAttribute
    private String id;

    @XmlElement(name = "paragraph")
    private List<Paragraph> paragraphs = new ArrayList<>();

    public StoryModel() {
    }

    public StoryModel(String id) {
        this.id = id;
    }

    public StoryModel(String id, List<Paragraph> paragraphs) {
        this.id = id;
        this.paragraphs = paragraphs;
    }

    public String getId() {
        return id;
    }

    public List<Paragraph> getParagraphs() {
        return paragraphs;
    }

    public void addParagraph(Paragraph paragraph) {
        paragraphs.add(paragraph);
    }

    public String getJoinedText() {
        return paragraphs.stream()
                .map(Paragraph::getText)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}