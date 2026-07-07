package org.example.bdoc.spi;

public interface ToolbarActionExtension {
    String getActionId();
    String getLabelKey();
    String getIconPath();
    String getGroup();
    String getShortcut();

    void execute(Object context);

    default boolean isEnabled(Object context) { return true; }
    default boolean isSelected(Object context) { return false; }
}