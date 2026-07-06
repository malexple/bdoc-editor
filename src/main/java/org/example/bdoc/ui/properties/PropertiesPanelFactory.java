package org.example.bdoc.ui.properties;

import javafx.scene.layout.VBox;
import org.example.bdoc.model.BdocObject;
import org.example.bdoc.ui.EditorContext;

/**
 * Фабрика UI-компонентов правой панели (Extension Point, аналог
 * PropertiesComponent провайдеров в IntelliJ). Плагин, добавивший новый тип
 * BdocObject, регистрирует свою фабрику через
 * PluginContext.registerPropertiesFactory(...) — ядро ничего не знает о
 * конкретных типах объектов расширений.
 */
public interface PropertiesPanelFactory {
    boolean supports(BdocObject object);
    void buildPanel(VBox container, BdocObject object, EditorContext context);
}