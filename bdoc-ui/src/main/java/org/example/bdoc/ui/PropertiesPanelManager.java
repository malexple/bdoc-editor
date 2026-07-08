package org.example.bdoc.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.example.bdoc.model.BdocObject;
import org.example.bdoc.plugin.PluginContext;
import org.example.bdoc.spi.PropertiesPanelFactory;

public class PropertiesPanelManager {
    private final BdocEditorApp app;
    private VBox container;
    private Label title;

    public PropertiesPanelManager(BdocEditorApp app) { this.app = app; }

    public void setPropertiesContainer(VBox container, Label title) {
        this.container = container;
        this.title = title;
    }

    public void rebuildPropertiesPanel(BdocObject obj) {
        if (container == null) return;
        container.getChildren().clear();
        if (obj == null) return;
        PropertiesPanelFactory factory = PluginContext.getInstance().findPropertiesFactory(obj);
        if (factory != null) factory.buildPanel(container, obj, app);
    }
}