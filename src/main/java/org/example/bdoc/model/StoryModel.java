package org.example.bdoc.model;

public class StoryModel {
    private final String id;
    private final String text;

    public StoryModel(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}