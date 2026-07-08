package org.example.bdoc.ui;

import org.example.bdoc.model.BdocObject;
import org.example.bdoc.model.LayerModel;

final class EditorTreeNodeData {

    enum NodeKind {
        DOCUMENT, PAGE, LAYER, OBJECT
    }

    final NodeKind kind;
    final int pageIndex;
    final LayerModel layer;
    final BdocObject object;
    final boolean masterLocked;

    static EditorTreeNodeData document() {
        return new EditorTreeNodeData(NodeKind.DOCUMENT, -1, null, null, false);
    }

    static EditorTreeNodeData page(int pageIndex) {
        return new EditorTreeNodeData(NodeKind.PAGE, pageIndex, null, null, false);
    }

    static EditorTreeNodeData layer(int pageIndex, LayerModel layer) {
        return new EditorTreeNodeData(NodeKind.LAYER, pageIndex, layer, null, false);
    }

    static EditorTreeNodeData object(int pageIndex, LayerModel layer, BdocObject object, boolean masterLocked) {
        return new EditorTreeNodeData(NodeKind.OBJECT, pageIndex, layer, object, masterLocked);
    }

    private EditorTreeNodeData(NodeKind kind, int pageIndex, LayerModel layer, BdocObject object, boolean masterLocked) {
        this.kind = kind;
        this.pageIndex = pageIndex;
        this.layer = layer;
        this.object = object;
        this.masterLocked = masterLocked;
    }
}