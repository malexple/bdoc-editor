package org.example.bdoc.ui.tool;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.example.bdoc.model.BdocObject;
import org.example.bdoc.model.Geometry;
import org.example.bdoc.model.TextFrame;
import org.example.bdoc.render.PageRenderer;
import org.example.bdoc.spi.DtpToolStrategy;
import org.example.bdoc.spi.EditorContext;

import java.util.List;

/**
 * Инструмент Т — работает с семантическим слоем StoryModel, а не с
 * геометрией. Клик внутрь TextFrame переключает правую панель в режим
 * текстового редактора. Изменять геометрию фрейма этому инструменту
 * запрещено архитектурным правилом (см. ответ пользователя, пункт 3).
 */
public final class TextToolStrategy implements DtpToolStrategy {

    private final PageRenderer pageRenderer = new PageRenderer();

    @Override
    public String getToolId() { return "TEXT"; }

    @Override
    public String getLabel() { return "Text"; }

    @Override
    public void onMousePressed(MouseEvent e, EditorContext context) {
        List<BdocObject> effectiveObjects = pageRenderer.resolveEffectiveObjects(
                context.getCurrentPage(), context.getCurrentMasterPage());

        BdocObject found = null;
        for (int i = effectiveObjects.size() - 1; i >= 0; i--) {
            BdocObject obj = effectiveObjects.get(i);
            if (!(obj instanceof TextFrame) || !obj.isVisible()) continue;
            double[] local = context.toLocalPoint(e.getX(), e.getY(), obj);
            var g = obj.getGeometry();
            if (local[0] >= g.getX() && local[0] <= g.getX() + g.getWidth() &&
                    local[1] >= g.getY() && local[1] <= g.getY() + g.getHeight()) {
                found = obj;
                break;
            }
        }

        context.setSelectedObject(found);
        if (found instanceof TextFrame textFrame) {
            context.setStatusText("Editing Story: " + textFrame.getStoryRef());
        }
        context.renderCurrentPage();
    }

    @Override
    public void onMouseDragged(MouseEvent e, EditorContext context) {
        // Text tool не двигает и не ресайзит геометрию — намеренно пусто.
    }

    @Override
    public void onMouseReleased(MouseEvent e, EditorContext context) {
        // Намеренно пусто.
    }

    @Override
    public void renderOverlay(GraphicsContext gc, EditorContext context) {
        BdocObject selected = context.getSelectedObject();
        if (!(selected instanceof TextFrame)) return;

        Geometry g = selected.getGeometry();
        gc.save();
        gc.setStroke(Color.web("#10B981"));
        gc.setLineWidth(2.0);
        gc.strokeRect(g.getX() - 1, g.getY() - 1, g.getWidth() + 2, g.getHeight() + 2);
        gc.restore();
    }
}