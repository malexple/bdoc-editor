package org.example.bdoc.ui.tool;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import org.example.bdoc.plugin.Disposable;
import org.example.bdoc.ui.EditorContext;

/**
 * Стратегия инструмента (аналог AnAction в IntelliJ IDEA). Реализация не
 * знает о существовании BdocEditorApp — только об EditorContext.
 */
public interface DtpToolStrategy extends Disposable {
    String getToolId();
    String getLabel();

    void onMousePressed(MouseEvent e, EditorContext context);
    void onMouseDragged(MouseEvent e, EditorContext context);
    void onMouseReleased(MouseEvent e, EditorContext context);

    void renderOverlay(GraphicsContext gc, EditorContext context);

    default void activate(EditorContext context) {}
    default void deactivate(EditorContext context) {}

    @Override
    default void dispose() {}
}