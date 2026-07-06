package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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

    // Этап 1.8: шаблонный уровень каскада prepress-геометрии.
    private final Double bleedMargin;
    private final Double safetyMargin;
    private final PrintMarksSettings printMarksSettings;

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
            @JsonProperty("objects") List<BdocObject> objects,
            @JsonProperty("bleedMargin") Double bleedMargin,
            @JsonProperty("safetyMargin") Double safetyMargin,
            @JsonProperty("printMarksSettings") PrintMarksSettings printMarksSettings) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.height = height;
        this.margin = margin != null ? margin : MarginModel.uniform(0.0);
        this.grid = grid != null ? grid : GridModel.disabled();
        this.baselineGrid = baselineGrid != null ? baselineGrid : BaselineGrid.disabled();
        this.guides = guides != null ? guides : List.of();
        this.objects = objects != null ? objects : List.of();
        this.bleedMargin = bleedMargin;
        this.safetyMargin = safetyMargin;
        this.printMarksSettings = printMarksSettings;
    }

    /** Совместимость с Этапом 1.6: конструктор без prepress-полей. */
    public MasterPage(String id, String name, double width, double height, MarginModel margin,
                      GridModel grid, BaselineGrid baselineGrid, List<Guide> guides, List<BdocObject> objects) {
        this(id, name, width, height, margin, grid, baselineGrid, guides, objects, null, null, null);
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
    public Double getBleedMargin() { return bleedMargin; }
    public Double getSafetyMargin() { return safetyMargin; }
    public PrintMarksSettings getPrintMarksSettings() { return printMarksSettings; }

    public BdocObject findObject(String objectId) {
        return objects.stream()
                .filter(o -> o.getId().equals(objectId))
                .findFirst()
                .orElse(null);
    }
}