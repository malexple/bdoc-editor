package org.example.bdoc.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.example.bdoc.model.BdocObject;
import org.example.bdoc.model.Geometry;
import org.example.bdoc.model.PageModel;
import org.example.bdoc.model.TransformModel;
import org.example.bdoc.plugin.PluginContext;
import org.example.bdoc.render.PageRenderer;
import org.example.bdoc.spi.DtpToolStrategy;
import java.io.IOException;

public class CanvasController {
    private final BdocEditorApp app;
    private final DocumentManager docMgr;
    private final ToolManager toolMgr;
    private final Canvas canvas;
    private final PageRenderer renderer = new PageRenderer();

    public CanvasController(BdocEditorApp app, DocumentManager docMgr, ToolManager toolMgr) {
        this.app = app;
        this.docMgr = docMgr;
        this.toolMgr = toolMgr;
        canvas = new Canvas(595, 842);
        canvas.setOnMousePressed(e -> toolMgr.dispatchToActiveTool(s -> s.onMousePressed(e, app)));
        canvas.setOnMouseDragged(e -> toolMgr.dispatchToActiveTool(s -> s.onMouseDragged(e, app)));
        canvas.setOnMouseReleased(e -> toolMgr.dispatchToActiveTool(s -> s.onMouseReleased(e, app)));
    }

    public Canvas getCanvas() { return canvas; }

    public void renderCurrentPage() {
        if (docMgr.getDocument() == null) return;
        try {
            PageModel page = docMgr.getCurrentPage();
            if (page == null) return;
            canvas.setWidth(page.getWidth());
            canvas.setHeight(page.getHeight());
            GraphicsContext gc = canvas.getGraphicsContext2D();
            renderer.render(gc, docMgr.getDocument(), page);
            DtpToolStrategy strategy = PluginContext.getInstance().getTool(toolMgr.getCurrentToolId());
            if (strategy != null) strategy.renderOverlay(gc, app);
        } catch (IOException e) {
            app.showError("Render error", e.getMessage());
        }
    }

    public double[] toLocalPoint(double screenX, double screenY, BdocObject obj) {
        TransformModel t = obj.getTransform();
        if (t == null || t.isIdentity()) return new double[]{screenX, screenY};
        Geometry g = obj.getGeometry();
        double cx = g.getX() + g.getWidth()/2.0, cy = g.getY() + g.getHeight()/2.0;
        double px = screenX - t.getTranslateX(), py = screenY - t.getTranslateY();
        px -= cx; py -= cy;
        double rad = Math.toRadians(-t.getRotationDegrees());
        double cos = Math.cos(rad), sin = Math.sin(rad);
        double rx = px*cos - py*sin, ry = px*sin + py*cos;
        rx /= t.getScaleX(); ry /= t.getScaleY();
        return new double[]{rx + cx, ry + cy};
    }
}