package org.example.bdoc.ui.properties;

import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.example.bdoc.model.BdocObject;
import org.example.bdoc.model.Paragraph;
import org.example.bdoc.model.StoryModel;
import org.example.bdoc.model.TextFrame;
import org.example.bdoc.ui.EditorContext;

public final class TextEditorPropertiesPanelFactory implements PropertiesPanelFactory {

    @Override
    public boolean supports(BdocObject object) {
        return object instanceof TextFrame;
    }

    @Override
    public void buildPanel(VBox container, BdocObject object, EditorContext context) {
        TextFrame textFrame = (TextFrame) object;
        StoryModel story = context.getDocument().getStory(textFrame.getStoryRef());

        Label titleLabel = new Label("Text Frame Content");
        titleLabel.getStyleClass().add("bdoc-section-title");

        Label infoLabel = new Label("Frame ID: " + textFrame.getId() + "\nStory Ref: " + textFrame.getStoryRef());
        infoLabel.getStyleClass().add("bdoc-muted-label");

        TextArea storyTextArea = new TextArea();
        storyTextArea.setPrefHeight(300);
        storyTextArea.setWrapText(true);
        if (story != null) {
            storyTextArea.setText(story.getJoinedText());
        }

        storyTextArea.textProperty().addListener((obs, oldText, newText) -> {
            if (story == null) return;
            context.runWriteAction(() -> {
                story.getParagraphs().clear();
                for (String line : newText.split("\n")) {
                    story.getParagraphs().add(new Paragraph("body", "body-text", line));
                }
            });
            context.renderCurrentPage();
        });

        Label hintLabel = new Label("Tip: You can paste cleaned OCR text here.");
        hintLabel.setWrapText(true);
        hintLabel.getStyleClass().add("bdoc-hint-label");

        container.getChildren().addAll(titleLabel, infoLabel, new Separator(), storyTextArea, hintLabel);
    }
}