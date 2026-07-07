package org.example.bdoc.ui.tool;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.example.bdoc.model.*;
import org.example.bdoc.render.PageRenderer;
import org.example.bdoc.spi.DtpToolStrategy;
import org.example.bdoc.spi.EditorContext;

import java.util.ArrayList;
import java.util.List;

public final class SelectionToolStrategy implements DtpToolStrategy {

    private static final double HANDLE_SIZE = 6.0;

    private final PageRenderer pageRenderer = new PageRenderer();

    private boolean isResizing = false;
    private int resizeHandleIndex = -1;
    private double dragStartX;
    private double dragStartY;
    private double objectInitialX;
    private double objectInitialY;
    private double initialWidth;
    private double initialHeight;
    private PathModel dragStartPathData;
    private Geometry dragStartPathGeometry;

    @Override
    public String getToolId() { return "SELECTION"; }

    @Override
    public String getLabel() { return "🏹 Select"; }

    @Override
    public void onMousePressed(MouseEvent e, EditorContext context) {
        PageModel page = context.getCurrentPage();
        MasterPage masterPage = context.getCurrentMasterPage();
        BdocObject selectedObject = context.getSelectedObject();

        if (selectedObject != null) {
            Geometry g = selectedObject.getGeometry();
            double[] local = context.toLocalPoint(e.getX(), e.getY(), selectedObject);
            double x = local[0];
            double y = local[1];

            double[][] handles = {
                    {g.getX(), g.getY()},
                    {g.getX() + g.getWidth(), g.getY()},
                    {g.getX(), g.getY() + g.getHeight()},
                    {g.getX() + g.getWidth(), g.getY() + g.getHeight()}
            };

            for (int i = 0; i < handles.length; i++) {
                if (x >= handles[i][0] - HANDLE_SIZE && x <= handles[i][0] + HANDLE_SIZE &&
                        y >= handles[i][1] - HANDLE_SIZE && y <= handles[i][1] + HANDLE_SIZE) {

                    BdocObject materialized = context.materializeOverrideIfNeeded(selectedObject);
                    context.setSelectedObject(materialized);

                    isResizing = true;
                    resizeHandleIndex = i;
                    Geometry gg = materialized.getGeometry();
                    dragStartX = x;
                    dragStartY = y;
                    objectInitialX = gg.getX();
                    objectInitialY = gg.getY();
                    initialWidth = gg.getWidth();
                    initialHeight = gg.getHeight();
                    dragStartPathData = materialized.getPathData();
                    dragStartPathGeometry = gg.copy();

                    context.setStatusText("Resizing object from corner: " + i);
                    return;
                }
            }
        }

        isResizing = false;
        resizeHandleIndex = -1;

        List<BdocObject> effectiveObjects = pageRenderer.resolveEffectiveObjects(page, masterPage);
        BdocObject found = null;
        for (int i = effectiveObjects.size() - 1; i >= 0; i--) {
            BdocObject obj = effectiveObjects.get(i);
            if (!obj.isVisible() || obj.isArtifact()) continue;
            Geometry g = obj.getGeometry();
            double[] local = context.toLocalPoint(e.getX(), e.getY(), obj);
            if (local[0] >= g.getX() && local[0] <= g.getX() + g.getWidth() &&
                    local[1] >= g.getY() && local[1] <= g.getY() + g.getHeight()) {
                found = obj;
                break;
            }
        }

        context.setSelectedObject(found);

        if (found != null) {
            dragStartX = e.getX();
            dragStartY = e.getY();
            objectInitialX = found.getGeometry().getX();
            objectInitialY = found.getGeometry().getY();
            dragStartPathData = found.getPathData();
            dragStartPathGeometry = found.getGeometry().copy();
            boolean fromMaster = pageRenderer.isRawMasterObject(found, masterPage);
            context.setStatusText((fromMaster ? "Selected (master-locked): " : "Selected: ") + found.getId());
        }

        context.renderCurrentPage();
    }

    @Override
    public void onMouseDragged(MouseEvent e, EditorContext context) {
        BdocObject selectedObject = context.getSelectedObject();
        if (selectedObject == null) return;

        MasterPage masterPage = context.getCurrentMasterPage();

        if (!isResizing && pageRenderer.isRawMasterObject(selectedObject, masterPage)) {
            selectedObject = context.materializeOverrideIfNeeded(selectedObject);
            context.setSelectedObject(selectedObject);
            objectInitialX = selectedObject.getGeometry().getX();
            objectInitialY = selectedObject.getGeometry().getY();
        }

        double deltaX = e.getX() - dragStartX;
        double deltaY = e.getY() - dragStartY;
        Geometry g = selectedObject.getGeometry();
        final BdocObject current = selectedObject;

        context.runWriteAction(() -> {
            if (isResizing) {
                applyResize(g, deltaX, deltaY);
            } else {
                g.setX(objectInitialX + deltaX);
                g.setY(objectInitialY + deltaY);
            }
        });

        if (dragStartPathData != null && !dragStartPathData.getPoints().isEmpty()
                && !"primitive".equals(dragStartPathData.getContourType())) {
            PathModel rescaled = rescalePathData(dragStartPathData, dragStartPathGeometry, g);
            BdocObject updated = context.replacePathData(current, rescaled);
            context.setSelectedObject(updated);
        }

        context.renderCurrentPage();
    }

    private void applyResize(Geometry g, double deltaX, double deltaY) {
        switch (resizeHandleIndex) {
            case 0 -> {
                double newWidth = initialWidth - deltaX;
                double newHeight = initialHeight - deltaY;
                if (newWidth > 10 && newHeight > 10) {
                    g.setX(objectInitialX + deltaX);
                    g.setY(objectInitialY + deltaY);
                    g.setWidth(newWidth);
                    g.setHeight(newHeight);
                }
            }
            case 1 -> {
                double newWidth = initialWidth + deltaX;
                double newHeight = initialHeight - deltaY;
                if (newWidth > 10 && newHeight > 10) {
                    g.setY(objectInitialY + deltaY);
                    g.setWidth(newWidth);
                    g.setHeight(newHeight);
                }
            }
            case 2 -> {
                double newWidth = initialWidth - deltaX;
                double newHeight = initialHeight + deltaY;
                if (newWidth > 10 && newHeight > 10) {
                    g.setX(objectInitialX + deltaX);
                    g.setWidth(newWidth);
                    g.setHeight(newHeight);
                }
            }
            case 3 -> {
                double newWidth = initialWidth + deltaX;
                double newHeight = initialHeight + deltaY;
                if (newWidth > 10 && newHeight > 10) {
                    g.setWidth(newWidth);
                    g.setHeight(newHeight);
                }
            }
        }
    }

    private PathModel rescalePathData(PathModel original, Geometry oldBox, Geometry newBox) {
        double oldW = oldBox.getWidth();
        double oldH = oldBox.getHeight();
        double scaleX = oldW != 0 ? newBox.getWidth() / oldW : 1.0;
        double scaleY = oldH != 0 ? newBox.getHeight() / oldH : 1.0;

        List<PathPoint> rescaled = new ArrayList<>();
        for (PathPoint p : original.getPoints()) {
            double nx = newBox.getX() + (p.getX() - oldBox.getX()) * scaleX;
            double ny = newBox.getY() + (p.getY() - oldBox.getY()) * scaleY;
            if ("C".equals(p.getCommand())) {
                double nx1 = newBox.getX() + (p.getX1() - oldBox.getX()) * scaleX;
                double ny1 = newBox.getY() + (p.getY1() - oldBox.getY()) * scaleY;
                double nx2 = newBox.getX() + (p.getX2() - oldBox.getX()) * scaleX;
                double ny2 = newBox.getY() + (p.getY2() - oldBox.getY()) * scaleY;
                rescaled.add(PathPoint.cubicTo(nx1, ny1, nx2, ny2, nx, ny));
            } else if ("L".equals(p.getCommand())) {
                rescaled.add(PathPoint.lineTo(nx, ny));
            } else {
                rescaled.add(PathPoint.moveTo(nx, ny));
            }
        }
        return new PathModel(original.getContourType(), rescaled, original.getFillRule());
    }

    @Override
    public void onMouseReleased(MouseEvent e, EditorContext context) {
        isResizing = false;
        resizeHandleIndex = -1;
        dragStartPathData = null;
        dragStartPathGeometry = null;
    }

    @Override
    public void renderOverlay(GraphicsContext gc, EditorContext context) {
        BdocObject selected = context.getSelectedObject();
        if (selected == null) return;

        Geometry g = selected.getGeometry();
        gc.save();
        gc.setStroke(Color.web("#2563EB"));
        gc.setLineWidth(1.5);
        gc.setLineDashes(6.0, 4.0);
        gc.strokeRect(g.getX(), g.getY(), g.getWidth(), g.getHeight());
        gc.setLineDashes(null);

        gc.setFill(Color.WHITE);
        gc.setStroke(Color.web("#2563EB"));
        gc.setLineWidth(1.0);
        double[][] handles = {
                {g.getX(), g.getY()},
                {g.getX() + g.getWidth(), g.getY()},
                {g.getX(), g.getY() + g.getHeight()},
                {g.getX() + g.getWidth(), g.getY() + g.getHeight()}
        };
        for (double[] h : handles) {
            gc.fillRect(h[0] - HANDLE_SIZE / 2, h[1] - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
            gc.strokeRect(h[0] - HANDLE_SIZE / 2, h[1] - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
        }
        gc.restore();
    }

    @Override
    public void deactivate(EditorContext context) {
        isResizing = false;
        resizeHandleIndex = -1;
        dragStartPathData = null;
        dragStartPathGeometry = null;
    }
}