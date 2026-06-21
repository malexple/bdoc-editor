package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum LayerRole {
    @XmlEnumValue("background")
    BACKGROUND,

    @XmlEnumValue("decoration")
    DECORATION,

    @XmlEnumValue("text")
    TEXT,

    @XmlEnumValue("image")
    IMAGE,

    @XmlEnumValue("header-footer")
    HEADER_FOOTER,

    @XmlEnumValue("annotation")
    ANNOTATION
}