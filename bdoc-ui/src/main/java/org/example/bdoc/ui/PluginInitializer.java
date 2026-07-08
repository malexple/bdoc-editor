package org.example.bdoc.ui;

import org.example.bdoc.plugin.PluginContext;
import org.example.bdoc.plugin.PluginDescriptor;
import org.example.bdoc.ui.properties.DefaultGeometryPropertiesPanelFactory;
import org.example.bdoc.ui.properties.TextEditorPropertiesPanelFactory;
import org.example.bdoc.ui.tool.SelectionToolStrategy;
import org.example.bdoc.ui.tool.TextToolStrategy;

public class PluginInitializer {
    public static void init() {
        PluginContext ctx = PluginContext.getInstance();
        ctx.registerPlugin(new PluginDescriptor("bdoc-core", "org.example.bdoc.core", "BDoc Core", "0.1", "BDoc Team"));
        ctx.registerTool(new SelectionToolStrategy());
        ctx.registerTool(new TextToolStrategy());
        ctx.registerPropertiesFactory(new DefaultGeometryPropertiesPanelFactory());
        ctx.registerPropertiesFactory(new TextEditorPropertiesPanelFactory());
    }
}