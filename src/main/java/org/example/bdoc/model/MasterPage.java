package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Шаблон страницы. Объекты мастера НЕ копируются на страницы —
 * PageRenderer подмешивает их динамически через PageModel.templateRef.
 * Все геометрические величины — в pt.
 *
 * templateRef в PageModel сделан строкой (одиночная ссылка) в v0.1,
 * но данные MasterPage спроектированы так, что переход к массиву
 * ссылок (multi-slot: grid + pagination отдельно) не потребует
 * изменения структуры самого MasterPage.
 */
public final class MasterPage {

    private final String id;
    private final String name;
    private final double width;
    private final double height;
    private final MarginModel margin;
    private final GridModel grid;
    private final BaselineGrid baselineGrid;
    private final List<Guide> guides;
    private final List<BdocObject> objects;

    @JsonCreator
    public MasterPage(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("width") double width,
            @JsonProperty("height") double height,
            @JsonProperty("margin") MarginModel margin,
            @JsonProperty("grid") GridModel grid,
            @JsonProperty("baselineGrid") BaselineGrid baselineGrid,
            @JsonProperty("guides") List<Guide> guides,
            @JsonProperty("objects") List<BdocObject> objects) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.height = height;
        this.margin = margin != null ? margin : MarginModel.uniform(0.0);
        this.grid = grid != null ? grid : GridModel.disabled();
        this.baselineGrid = baselineGrid != null ? baselineGrid : BaselineGrid.disabled();
        this.guides = guides != null ? guides : List.of();
        this.objects = objects != null ? objects : List.of();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public MarginModel getMargin() { return margin; }
    public GridModel getGrid() { return grid; }
    public BaselineGrid getBaselineGrid() { return baselineGrid; }
    public List<Guide> getGuides() { return guides; }
    public List<BdocObject> getObjects() { return objects; }

    public BdocObject findObject(String objectId) {
        return objects.stream()
                .filter(o -> o.getId().equals(objectId))
                .findFirst()
                .orElse(null);
    }
}