package org.example.bdoc.ui;

import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.BdocObject;
import org.example.bdoc.model.LayerModel;
import org.example.bdoc.model.MasterPage;
import org.example.bdoc.model.PageModel;
import org.example.bdoc.render.PageRenderer;

import java.io.IOException;
import java.util.List;

public final class BdocDocumentTreeController {

    public interface Callbacks {
        DocumentHandle getDocumentForTree();
        int getCurrentPageIndexForTree();
        void onPageSelectedFromTree(int pageIndex);
        void onObjectSelectedFromTree(int pageIndex, BdocObject object);
        BdocObject materializeOverrideForTree(PageModel page, MasterPage masterPage, BdocObject object);
        void renderCurrentPageFromTree();
        void refreshTreeFromTree();
        void showErrorFromTree(String title, String message);
    }

    private final TreeView<EditorTreeNodeData> treeView;
    private final PageRenderer pageRenderer;
    private final Callbacks callbacks;

    public BdocDocumentTreeController(
            TreeView<EditorTreeNodeData> treeView,
            PageRenderer pageRenderer,
            Callbacks callbacks
    ) {
        this.treeView = treeView;
        this.pageRenderer = pageRenderer;
        this.callbacks = callbacks;

        this.treeView.setCellFactory(tv -> new TreeNodeCell());
        this.treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                onTreeSelectionChanged(newItem.getValue());
            }
        });
    }

    public void refreshTree(DocumentHandle document, int currentPageIndex) {
        if (document == null) {
            treeView.setRoot(null);
            return;
        }

        TreeItem<EditorTreeNodeData> root = new TreeItem<>(EditorTreeNodeData.document());
        root.setExpanded(true);

        for (int pageIndex = 1; pageIndex <= document.getPageCount(); pageIndex++) {
            TreeItem<EditorTreeNodeData> pageItem = new TreeItem<>(EditorTreeNodeData.page(pageIndex));
            pageItem.setExpanded(pageIndex == currentPageIndex);

            try {
                PageModel page = document.loadPage(pageIndex);
                MasterPage masterPage = document.getMasterPage(page.getTemplateRef());
                List<BdocObject> effectiveObjects = pageRenderer.resolveEffectiveObjects(page, masterPage);

                for (LayerModel layer : page.getLayers()) {
                    TreeItem<EditorTreeNodeData> layerItem = new TreeItem<>(EditorTreeNodeData.layer(pageIndex, layer));
                    layerItem.setExpanded(true);

                    for (BdocObject object : effectiveObjects) {
                        if (!object.getLayerRef().equals(layer.getId())) {
                            continue;
                        }
                        boolean isMasterLocked = pageRenderer.isRawMasterObject(object, masterPage);
                        layerItem.getChildren().add(
                                new TreeItem<>(EditorTreeNodeData.object(pageIndex, layer, object, isMasterLocked))
                        );
                    }

                    pageItem.getChildren().add(layerItem);
                }
            } catch (IOException ex) {
                callbacks.showErrorFromTree("Tree build error",
                        "Failed to load page " + pageIndex + ": " + ex.getMessage());
            }

            root.getChildren().add(pageItem);
        }

        treeView.setRoot(root);
        treeView.setShowRoot(true);
    }

    public void selectTreeNodeFor(BdocObject object) {
        if (object == null || treeView.getRoot() == null) {
            return;
        }
        findAndSelectRecursive(treeView.getRoot(), object);
    }

    private boolean findAndSelectRecursive(TreeItem<EditorTreeNodeData> item, BdocObject target) {
        if (item.getValue() != null
                && item.getValue().kind == EditorTreeNodeData.NodeKind.OBJECT
                && item.getValue().object == target) {
            treeView.getSelectionModel().select(item);
            return true;
        }

        for (TreeItem<EditorTreeNodeData> child : item.getChildren()) {
            if (findAndSelectRecursive(child, target)) {
                return true;
            }
        }
        return false;
    }

    private void onTreeSelectionChanged(EditorTreeNodeData data) {
        if (data == null || callbacks.getDocumentForTree() == null) {
            return;
        }

        if (data.kind == EditorTreeNodeData.NodeKind.PAGE) {
            callbacks.onPageSelectedFromTree(data.pageIndex);
            return;
        }

        if (data.kind == EditorTreeNodeData.NodeKind.OBJECT) {
            callbacks.onObjectSelectedFromTree(data.pageIndex, data.object);
        }
    }

    private final class TreeNodeCell extends TreeCell<EditorTreeNodeData> {
        @Override
        protected void updateItem(EditorTreeNodeData data, boolean empty) {
            super.updateItem(data, empty);

            if (empty || data == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
                return;
            }

            DocumentHandle document = callbacks.getDocumentForTree();

            switch (data.kind) {
                case DOCUMENT -> {
                    setText(document != null ? "Document: " + document.getTitle() : "Document");
                    setGraphic(null);
                    setStyle("-fx-font-weight: bold;");
                }
                case PAGE -> {
                    setText("Page " + data.pageIndex);
                    setGraphic(null);
                    setStyle("");
                }
                case LAYER -> {
                    CheckBox eyeBox = new CheckBox(data.layer.getName() + " (" + data.layer.getRole() + ")");
                    eyeBox.setSelected(data.layer.isVisible());
                    eyeBox.setOnAction(e -> {
                        data.layer.setVisible(eyeBox.isSelected());
                        callbacks.renderCurrentPageFromTree();
                    });
                    setGraphic(eyeBox);
                    setText(null);
                    setStyle("-fx-font-weight: bold;");
                }
                case OBJECT -> {
                    CheckBox eyeBox = new CheckBox();
                    eyeBox.setSelected(data.object.isVisible());

                    String label = (data.masterLocked ? "🔒 " : "")
                            + data.object.getId()
                            + " ["
                            + data.object.getType()
                            + "]";

                    eyeBox.setText(label);
                    eyeBox.setOnAction(e -> {
                        try {
                            PageModel page = document.loadPage(data.pageIndex);
                            MasterPage masterPage = document.getMasterPage(page.getTemplateRef());

                            BdocObject target = data.masterLocked
                                    ? callbacks.materializeOverrideForTree(page, masterPage, data.object)
                                    : data.object;

                            target.setVisible(eyeBox.isSelected());
                            callbacks.renderCurrentPageFromTree();

                            if (data.masterLocked) {
                                callbacks.refreshTreeFromTree();
                            }
                        } catch (IOException ex) {
                            callbacks.showErrorFromTree("Visibility toggle error", ex.getMessage());
                        }
                    });

                    HBox box = new HBox(eyeBox);
                    setGraphic(box);
                    setText(null);
                    setStyle(data.masterLocked ? "-fx-opacity: 0.55; -fx-font-style: italic;" : "");
                }
            }
        }
    }
}