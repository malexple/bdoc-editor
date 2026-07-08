package org.example.bdoc.ui;

import org.example.bdoc.model.BdocObject;
import org.example.bdoc.model.LayerModel;

public class TreeNodeData {
    public enum NodeKind { DOCUMENT, PAGE, LAYER, OBJECT }

    public final NodeKind kind;
    public final int pageIndex;
    public final LayerModel layer;
    public final BdocObject object;
    public final boolean isMasterLocked;

    private TreeNodeData(NodeKind kind, int pageIndex, LayerModel layer, BdocObject object, boolean isMasterLocked) {
        this.kind = kind;
        this.pageIndex = pageIndex;
        this.layer = layer;
        this.object = object;
        this.isMasterLocked = isMasterLocked;
    }

    public static TreeNodeData document() {
        return new TreeNodeData(NodeKind.DOCUMENT, -1, null, null, false);
    }
    public static TreeNodeData page(int pageIndex) {
        return new TreeNodeData(NodeKind.PAGE, pageIndex, null, null, false);
    }
    public static TreeNodeData layer(int pageIndex, LayerModel layer) {
        return new TreeNodeData(NodeKind.LAYER, pageIndex, layer, null, false);
    }
    public static TreeNodeData object(int pageIndex, LayerModel layer, BdocObject object, boolean masterLocked) {
        return new TreeNodeData(NodeKind.OBJECT, pageIndex, layer, object, masterLocked);
    }
}