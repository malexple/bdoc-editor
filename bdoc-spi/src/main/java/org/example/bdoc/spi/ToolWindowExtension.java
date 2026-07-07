package org.example.bdoc.spi;

import javafx.scene.Node;

public interface ToolWindowExtension {
    String getWindowId();
    String getTitleKey();
    String getIconPath();
    Anchor getDefaultAnchor();
    ViewMode getDefaultViewMode();
    Node createContent(Object context);
}