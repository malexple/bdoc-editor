package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * rowIndex/colIndex — архитектурный задел под будущие rowSpan/colSpan
 * (Этап 3): движок рендеринга просто будет пропускать перекрытые индексы,
 * структура файла не изменится.
 */
public final class TableCell {
    private final int rowIndex;
    private final int colIndex;
    private final String storyRef;

    @JsonCreator
    public TableCell(
            @JsonProperty("rowIndex") int rowIndex,
            @JsonProperty("colIndex") int colIndex,
            @JsonProperty("storyRef") String storyRef) {
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
        this.storyRef = storyRef;
    }

    public int getRowIndex() { return rowIndex; }
    public int getColIndex() { return colIndex; }
    public String getStoryRef() { return storyRef; }
}