package org.example.bdoc.ui;

import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import org.example.bdoc.i18n.LocalizationManager;
import org.example.bdoc.plugin.BdocSettings;
import org.example.bdoc.plugin.PluginContext;
import org.example.bdoc.spi.DtpToolStrategy;
import java.text.MessageFormat;
import java.util.function.Consumer;

public class ToolManager {
    private final BdocEditorApp app;
    private String currentToolId;
    private ToggleGroup group;

    public ToolManager(BdocEditorApp app) {
        this.app = app;
        currentToolId = BdocSettings.getInstance().loadActiveTool();
    }

    public String getCurrentToolId() { return currentToolId; }
    public void setCurrentToolId(String id) { currentToolId = id; BdocSettings.getInstance().saveActiveTool(id); }

    public ToolBar buildToolPalette() {
        ToolBar palette = new ToolBar();
        palette.setOrientation(javafx.geometry.Orientation.VERTICAL);
        palette.getStyleClass().add("bdoc-tool-palette");
        group = new ToggleGroup();
        LocalizationManager i18n = LocalizationManager.getInstance();
        for (DtpToolStrategy tool : PluginContext.getInstance().getRegisteredTools().values()) {
            ToggleButton btn = new ToggleButton(glyphFor(tool.getToolId()));
            btn.getStyleClass().add("bdoc-tool-palette-button");
            btn.setToggleGroup(group);
            btn.setTooltip(new Tooltip(tool.getLabel()));
            if (tool.getToolId().equals(currentToolId)) btn.setSelected(true);
            btn.setOnAction(e -> {
                DtpToolStrategy old = PluginContext.getInstance().getTool(currentToolId);
                if (old != null) old.deactivate(app);
                currentToolId = tool.getToolId();
                tool.activate(app);
                app.setStatusText(MessageFormat.format(i18n.get("status.tool.active"), tool.getLabel()));
                app.renderCurrentPage();
            });
            palette.getItems().add(btn);
        }
        return palette;
    }

    private String glyphFor(String id) {
        return switch (id) {
            case "SELECTION" -> "⬖";
            case "TEXT" -> "T";
            default -> "?";
        };
    }

    public void dispatchToActiveTool(Consumer<DtpToolStrategy> action) {
        if (app.getDocument() == null) return;
        DtpToolStrategy strategy = PluginContext.getInstance().getTool(currentToolId);
        if (strategy != null) {
            try { action.accept(strategy); }
            catch (Exception e) { app.showError("Tool Error", e.getMessage()); }
        }
    }
}