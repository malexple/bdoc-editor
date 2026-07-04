package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Один шаг в линейном порядке чтения страницы. Не хранит геометрию —
 * только ссылку на существующий BdocObject.id (включая override-объекты)
 * и порядковый номер. Используется экспортёрами в EPUB/accessible PDF
 * и скриптами очистки/перевода для восстановления смыслового потока
 * контента независимо от визуальных координат на холсте.
 */
public final class ReadingSegment {

    private final int sequence;
    private final String targetObjectId;
    private final String role;

    @JsonCreator
    public ReadingSegment(
            @JsonProperty("sequence") int sequence,
            @JsonProperty("targetObjectId") String targetObjectId,
            @JsonProperty("role") String role) {
        this.sequence = sequence;
        this.targetObjectId = targetObjectId;
        this.role = role;
    }

    public int getSequence() { return sequence; }
    public String getTargetObjectId() { return targetObjectId; }
    public String getRole() { return role; }
}