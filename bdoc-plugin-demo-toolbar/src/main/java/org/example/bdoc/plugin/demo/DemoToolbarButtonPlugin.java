package org.example.bdoc.plugin.demo;

import javafx.scene.control.Alert;
import org.example.bdoc.spi.ToolbarActionExtension;

public class DemoToolbarButtonPlugin implements ToolbarActionExtension {

    @Override
    public String getActionId() {
        return "demo.toolbar.button";
    }

    @Override
    public String getLabelKey() {
        return "toolbar.demoButton";
    }

    @Override
    public String getIconPath() {
        return null;
    }

    @Override
    public String getGroup() {
        return "demo";
    }

    @Override
    public String getShortcut() {
        return null;
    }

    @Override
    public void execute(Object context) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Plugin Layer Works");
        alert.setHeaderText("Загружено через ModuleLayer из папки plugins/");
        alert.setContentText("Этот плагин не был в основной сборке приложения.");
        alert.showAndWait();
    }
}