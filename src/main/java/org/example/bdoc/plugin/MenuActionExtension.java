package org.example.bdoc.plugin;

import javafx.scene.control.MenuItem;
import org.example.bdoc.ui.EditorContext;

/**
 * Точка расширения для добавления команд в верхнее меню (аналог
 * <action> в plugin.xml IntelliJ). Плагины регистрируют свои пункты через
 * PluginContext.registerMenuExtension(...), ядро само их не создаёт.
 */
public interface MenuActionExtension {
    /** "FILE", "EDIT", "VIEW", "TOOLS" — расширяемо без изменения формата. */
    String getTargetMenuId();
    MenuItem createMenuItem(EditorContext context);
}