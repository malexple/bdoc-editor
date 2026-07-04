package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Справочник допустимых значений VectorShape.shapeType. Само поле в
 * VectorShape остаётся типом String (а не ShapeType) — это осознанное
 * решение: неизвестные/будущие значения shapeType не должны приводить
 * к ошибке десериализации всего документа, они просто попадут в switch
 * в PageRenderer.renderShape() и будут обработаны в default-ветке.
 * Этот enum используется только в BdocIntegrityValidator для проверки
 * "known shape types" и в UI (например, выпадающий список при создании
 * фигуры), а не как тип самого поля в модели.
 */
public enum ShapeType {
    RECTANGLE("rectangle"),
    ROUNDED_RECTANGLE("rounded-rectangle"),
    LINE("line"),
    ELLIPSE("ellipse"),
    POLYGON("polygon");

    private final String jsonValue;

    ShapeType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String toJsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static ShapeType fromJsonValue(String value) {
        for (ShapeType type : values()) {
            if (type.jsonValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown shapeType: " + value);
    }
}