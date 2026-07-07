package org.example.bdoc.ui.properties;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.bdoc.model.*;
import org.example.bdoc.spi.EditorContext;
import org.example.bdoc.spi.PropertiesPanelFactory;

/**
 * Встроенная фабрика по умолчанию — геометрия + слой + видимость.
 * Зарегистрирована первой, поэтому плагины, зарегистрированные позже,
 * могут переопределить её для своих типов (PluginContext ищет с конца списка).
 */
public final class DefaultGeometryPropertiesPanelFactory implements PropertiesPanelFactory {

    @Override
    public boolean supports(BdocObject object) {
        return true; // fallback для любого типа, если более специфичная фабрика не нашлась
    }

    @Override
    public void buildPanel(VBox container, BdocObject object, EditorContext context) {
        PageModel page = context.getCurrentPage();
        MasterPage masterPage = context.getCurrentMasterPage();

        LayerModel objectLayer = page.getLayers().stream()
                .filter(l -> l.getId().equals(object.getLayerRef()))
                .findFirst()
                .orElse(null);
        if (objectLayer == null) return;

        Label objectInfo = new Label("Object ID: " + object.getId() + "\nType: " + object.getType());
        objectInfo.getStyleClass().add("bdoc-muted-label");

        Unit currentUnit = Unit.fromString(page.getUnit());
        Geometry go = object.getGeometry();
        Label geometryInfo = new Label(String.format(
                "Geometry (%s):\nX: %.1f, Y: %.1f\nW: %.1f, H: %.1f",
                page.getUnit(),
                currentUnit.fromPoints(go.getX()),
                currentUnit.fromPoints(go.getY()),
                currentUnit.fromPoints(go.getWidth()),
                currentUnit.fromPoints(go.getHeight())));
        geometryInfo.getStyleClass().add("bdoc-mono-label");

        Label layerLabel = new Label("Layer: " + objectLayer.getName() + " (" + objectLayer.getRole() + ")");
        layerLabel.getStyleClass().add("bdoc-section-title");

        CheckBox visibleCheckBox = new CheckBox("Layer Visible");
        visibleCheckBox.setSelected(objectLayer.isVisible());
        visibleCheckBox.setOnAction(e -> {
            context.runWriteAction(() -> objectLayer.setVisible(visibleCheckBox.isSelected()));
            context.renderCurrentPage();
            context.refreshTree();
        });

        Label opacityLabel = new Label("Layer Opacity: " + Math.round(objectLayer.getOpacity() * 100) + "%");
        Slider opacitySlider = new Slider(0.0, 1.0, objectLayer.getOpacity());
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            context.runWriteAction(() -> objectLayer.setOpacity(newVal.doubleValue()));
            opacityLabel.setText("Layer Opacity: " + Math.round(newVal.doubleValue() * 100) + "%");
            context.renderCurrentPage();
        });

        CheckBox objectVisibleCheckBox = new CheckBox("Object Visible");
        objectVisibleCheckBox.setSelected(object.isVisible());
        objectVisibleCheckBox.setOnAction(e -> {
            BdocObject target = context.materializeOverrideIfNeeded(object);
            context.runWriteAction(() -> target.setVisible(objectVisibleCheckBox.isSelected()));
            context.setSelectedObject(target);
            context.renderCurrentPage();
            context.refreshTree();
        });

        container.getChildren().addAll(
                objectInfo, geometryInfo, new Separator(),
                layerLabel, visibleCheckBox, opacityLabel, opacitySlider,
                new Separator(), objectVisibleCheckBox
        );

        if (object.isMasterOverride()) {
            Label masterInfo = new Label("Linked to master: " + object.getMasterSourceId());
            masterInfo.getStyleClass().add("bdoc-warning-label");

            Button restoreBtn = new Button("Restore to Master");
            restoreBtn.setOnAction(e -> {
                BdocObject restored = context.restoreToMaster(object);
                context.setSelectedObject(restored);
                context.setStatusText("Restored to master: " + (restored != null ? restored.getId() : "?"));
                context.renderCurrentPage();
                context.refreshTree();
            });

            container.getChildren().addAll(masterInfo, restoreBtn);
        }
    }
}