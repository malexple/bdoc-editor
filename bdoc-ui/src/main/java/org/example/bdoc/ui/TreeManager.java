package org.example.bdoc.ui;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.example.bdoc.i18n.LocalizationManager;
import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.*;
import org.example.bdoc.render.PageRenderer;
import java.io.IOException;
import java.text.MessageFormat;

public class TreeManager {
    private final BdocEditorApp app;
    private final DocumentManager docMgr;
    private final TreeView<TreeNodeData> treeView;

    public TreeManager(BdocEditorApp app, DocumentManager docMgr) {
        this.app = app;
        this.docMgr = docMgr;
        treeView = new TreeView<>();
        treeView.setShowRoot(true);
        treeView.setCellFactory(tv -> new TreeNodeCell(app));
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            if (nw != null) onTreeSelectionChanged(nw.getValue());
        });
    }

    public TreeView<TreeNodeData> getTreeView() { return treeView; }

    public void refreshTree() {
        DocumentHandle doc = docMgr.getDocument();
        if (doc == null) { treeView.setRoot(null); return; }
        TreeItem<TreeNodeData> root = new TreeItem<>(TreeNodeData.document());
        root.setExpanded(true);
        int currentPage = docMgr.getCurrentPageIndex();
        PageRenderer renderer = new PageRenderer();
        for (int pi = 1; pi <= doc.getPageCount(); pi++) {
            TreeItem<TreeNodeData> pageItem = new TreeItem<>(TreeNodeData.page(pi));
            pageItem.setExpanded(pi == currentPage);
            try {
                PageModel page = doc.loadPage(pi);
                MasterPage master = doc.getMasterPage(page.getTemplateRef());
                var effective = renderer.resolveEffectiveObjects(page, master);
                for (LayerModel layer : page.getLayers()) {
                    TreeItem<TreeNodeData> layerItem = new TreeItem<>(TreeNodeData.layer(pi, layer));
                    layerItem.setExpanded(true);
                    for (BdocObject obj : effective) {
                        if (!obj.getLayerRef().equals(layer.getId())) continue;
                        boolean locked = renderer.isRawMasterObject(obj, master);
                        layerItem.getChildren().add(new TreeItem<>(TreeNodeData.object(pi, layer, obj, locked)));
                    }
                    pageItem.getChildren().add(layerItem);
                }
            } catch (IOException e) {
                app.showError(LocalizationManager.getInstance().get("error.treeBuild.title"),
                        MessageFormat.format(LocalizationManager.getInstance().get("error.treeBuild.message"), pi, e.getMessage()));
            }
            root.getChildren().add(pageItem);
        }
        treeView.setRoot(root);
    }

    private void onTreeSelectionChanged(TreeNodeData data) {
        if (data == null || docMgr.getDocument() == null) return;
        if (data.kind == TreeNodeData.NodeKind.PAGE) {
            docMgr.setCurrentPageIndex(data.pageIndex);
            docMgr.setSelectedObject(null);
            app.renderCurrentPage();
            try {
                PageModel page = docMgr.getDocument().loadPage(data.pageIndex);
                Unit u = Unit.fromString(page.getUnit());
                double w = u.fromPoints(page.getWidth()), h = u.fromPoints(page.getHeight());
                app.setStatusText(String.format("Active Page %d — Format %.1f × %.1f %s (%.0f × %.0f pt)",
                        data.pageIndex, w, h, page.getUnit(), page.getWidth(), page.getHeight()));
            } catch (IOException e) { app.setStatusText("Page selected"); }
            return;
        }
        if (data.kind == TreeNodeData.NodeKind.OBJECT) {
            docMgr.setCurrentPageIndex(data.pageIndex);
            docMgr.setSelectedObject(data.object);
            app.renderCurrentPage();
        }
    }

    public void selectTreeNodeFor(BdocObject obj) {
        if (obj == null || treeView.getRoot() == null) return;
        findAndSelect(treeView.getRoot(), obj);
    }

    private boolean findAndSelect(TreeItem<TreeNodeData> item, BdocObject target) {
        if (item.getValue() != null && item.getValue().kind == TreeNodeData.NodeKind.OBJECT && item.getValue().object == target) {
            treeView.getSelectionModel().select(item);
            return true;
        }
        for (TreeItem<TreeNodeData> child : item.getChildren())
            if (findAndSelect(child, target)) return true;
        return false;
    }
}