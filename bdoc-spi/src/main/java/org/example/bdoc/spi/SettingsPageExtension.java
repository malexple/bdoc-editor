package org.example.bdoc.spi;

import javafx.scene.Node;

public interface SettingsPageExtension {
    String getPageId();
    String getTitleKey();
    String getParentId();
    Node createPage(Object settingsStore);
}