package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public final class TextFrame extends BdocObject {

    private final String storyRef;

    // Story threading (Этап 1.5): цепочка перетекания текста между фреймами
    // одной истории. storyRef у всех фреймов цепочки одинаковый, а порядок
    // задаётся именно этими ссылками. Реальный расчёт "остатка" текста
    // (Layout Manager) — отдельная задача Этапа 2, здесь только модель.
    private final String nextFrameRef;
    private final String previousFrameRef;

    // isOverset НЕ персистится в JSON/CBOR — это вычисляемое состояние
    // (результат работы TextWrapper при текущей геометрии и шрифтах),
    // а не стабильный факт документа. Пересчитывается при каждом рендере,
    // поэтому помечено @JsonIgnore и хранится как обычное mutable-поле,
    // по аналогии с visible/setVisible у BdocObject.
    private boolean isOverset = false;

    public TextFrame(String id, String layerRef, Geometry geometry, String storyRef) {
        super(id, layerRef, geometry);
        this.storyRef = storyRef;
        this.nextFrameRef = null;
        this.previousFrameRef = null;
    }

    public TextFrame(String id, String layerRef, Geometry geometry, String storyRef,
                     String masterSourceId, Set<String> overriddenProperties) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties);
        this.storyRef = storyRef;
        this.nextFrameRef = null;
        this.previousFrameRef = null;
    }

    @JsonCreator
    public TextFrame(
            @JsonProperty("id") String id,
            @JsonProperty("layerRef") String layerRef,
            @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("storyRef") String storyRef,
            @JsonProperty("masterSourceId") String masterSourceId,
            @JsonProperty("overriddenProperties") Set<String> overriddenProperties,
            @JsonProperty("visible") Boolean visible,
            @JsonProperty("clipGeometry") Geometry clipGeometry,
            @JsonProperty("maskRef") String maskRef,
            @JsonProperty("mask") Boolean mask,
            @JsonProperty("artifact") Boolean artifact,
            @JsonProperty("artifactType") String artifactType,
            @JsonProperty("textWrap") TextWrapModel textWrap,
            @JsonProperty("pathData") PathModel pathData,
            @JsonProperty("transform") TransformModel transform,
            @JsonProperty("nextFrameRef") String nextFrameRef,
            @JsonProperty("previousFrameRef") String previousFrameRef) {
        super(id, layerRef, geometry, masterSourceId, overriddenProperties, visible,
                clipGeometry, maskRef, mask, artifact, artifactType, textWrap, pathData, transform);
        this.storyRef = storyRef;
        this.nextFrameRef = nextFrameRef;
        this.previousFrameRef = previousFrameRef;
    }

    public String getStoryRef() { return storyRef; }
    public String getNextFrameRef() { return nextFrameRef; }
    public String getPreviousFrameRef() { return previousFrameRef; }

    @JsonIgnore
    public boolean isOverset() { return isOverset; }
    public void setOverset(boolean overset) { this.isOverset = overset; }

    @Override
    public String getType() { return "TextFrame"; }
}