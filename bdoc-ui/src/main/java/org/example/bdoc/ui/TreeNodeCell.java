package org.example.bdoc.ui;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.HBox;
import org.example.bdoc.model.BdocObject;
import org.example.bdoc.model.MasterPage;
import org.example.bdoc.model.PageModel;
import java.io.IOException;

public class TreeNodeCell extends TreeCell<TreeNodeData> {
    private final BdocEditorApp app;

    public TreeNodeCell(BdocEditorApp app) {
        this.app = app;
    }

    @Override
    protected void updateItem(TreeNodeData data, boolean empty) {
        super.updateItem(data, empty);
        if (empty || data == null) {
            setText(null); setGraphic(null); setStyle("");
            return;
        }
        switch (data.kind) {
            case DOCUMENT -> {
                setText(app.getDocument() != null ? "Document: " + app.getDocument().getTitle() : "Document");
                setGraphic(null);
                setStyle("-fx-font-weight: bold;");
            }
            case PAGE -> {
                setText("Page " + data.pageIndex);
                setGraphic(null);
                setStyle("");
            }
            case LAYER -> {
                CheckBox eye = new CheckBox(data.layer.getName() + " [" + data.layer.getRole() + "]");
                eye.setSelected(data.layer.isVisible());
                eye.setOnAction(e -> { data.layer.setVisible(eye.isSelected()); app.renderCurrentPage(); });
                setGraphic(eye);
                setText(null);
                setStyle("-fx-font-weight: bold;");
            }
            case OBJECT -> {
                CheckBox eye = new CheckBox();
                eye.setSelected(data.object.isVisible());
                String label = data.isMasterLocked ? data.object.getId() + " [" + data.object.getType() + "]"
                        : data.object.getId() + " [" + data.object.getType() + "]";
                eye.setText(label);
                eye.setOnAction(e -> {
                    try {
                        PageModel page = app.getDocument().loadPage(data.pageIndex);
                        MasterPage master = app.getDocument().getMasterPage(page.getTemplateRef());
                        BdocObject target = data.isMasterLocked
                                ? app.materializeOverrideIfNeeded(data.object)
                                : data.object;
                        target.setVisible(eye.isSelected());
                        app.renderCurrentPage();
                        if (data.isMasterLocked) app.refreshTree();
                    } catch (IOException ex) {
                        app.showError("Visibility toggle error", ex.getMessage());
                    }
                });
                HBox box = new HBox(eye);
                setGraphic(box);
                setText(null);
                setStyle(data.isMasterLocked ? "-fx-opacity: 0.55; -fx-font-style: italic;" : "");
            }
        }
    }
}