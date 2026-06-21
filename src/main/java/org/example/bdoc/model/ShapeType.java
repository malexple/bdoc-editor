package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum ShapeType {
    @XmlEnumValue("rectangle")
    RECTANGLE,

    @XmlEnumValue("rounded-rectangle")
    ROUNDED_RECTANGLE,

    @XmlEnumValue("line")
    LINE
}