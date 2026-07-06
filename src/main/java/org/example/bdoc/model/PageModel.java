package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class PageModel {

    private final String id;
    private final int index;
    private final double width;
    private final double height;
    private final String unit;
    private final String templateRef;
    private final List<LayerModel> layers;
    private final List<BdocObject> objects;
    private final List<ReadingSegment> readingOrder;

    // Этап 1.8: prepress-геометрия (Вопросы 1-4), nullable для каскада
    // Page -> MasterPage -> Manifest.
    private final Double bleedMargin;
    private final Double safetyMargin;
    private final PrintMarksSettings printMarksSettings;

    @JsonCreator
    public PageModel(
            @JsonProperty("id") String id,
            @JsonProperty("index") int index,
            @JsonProperty("width") double width,
            @JsonProperty("height") double height,
            @JsonProperty("unit") String unit,
            @JsonProperty("templateRef") String templateRef,
            @JsonProperty("layers") List<LayerModel> layers,
            @JsonProperty("objects") List<BdocObject> objects,
            @JsonProperty("readingOrder") List<ReadingSegment> readingOrder,
            @JsonProperty("bleedMargin") Double bleedMargin,
            @JsonProperty("safetyMargin") Double safetyMargin,
            @JsonProperty("printMarksSettings") PrintMarksSettings printMarksSettings) {
        this.id = id;
        this.index = index;
        this.width = width;
        this.height = height;
        this.unit = unit != null ? unit : "pt";
        this.templateRef = templateRef;
        this.layers = layers != null ? layers : List.of();
        this.objects = objects != null ? objects : List.of();
        this.readingOrder = readingOrder != null ? readingOrder : List.of();
        this.bleedMargin = bleedMargin;
        this.safetyMargin = safetyMargin;
        this.printMarksSettings = printMarksSettings;
    }

    /** Совместимость с Этапом 1.6: полный конструктор без prepress-полей. */
    public PageModel(String id, int index, double width, double height, String unit, String templateRef,
                     List<LayerModel> layers, List<BdocObject> objects, List<ReadingSegment> readingOrder) {
        this(id, index, width, height, unit, templateRef, layers, objects, readingOrder, null, null, null);
    }

    /** Удобный конструктор без readingOrder — для случаев, когда порядок чтения ещё не размечен. */
    public PageModel(String id, int index, double width, double height, String unit, String templateRef,
                     List<LayerModel> layers, List<BdocObject> objects) {
        this(id, index, width, height, unit, templateRef, layers, objects, List.of(), null, null, null);
    }

    public String getId() { return id; }
    public int getIndex() { return index; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public String getUnit() { return unit; }
    public String getTemplateRef() { return templateRef; }
    public List<LayerModel> getLayers() { return layers; }
    public List<BdocObject> getObjects() { return objects; }
    public List<ReadingSegment> getReadingOrder() { return readingOrder; }
    public Double getBleedMargin() { return bleedMargin; }
    public Double getSafetyMargin() { return safetyMargin; }
    public PrintMarksSettings getPrintMarksSettings() { return printMarksSettings; }
}