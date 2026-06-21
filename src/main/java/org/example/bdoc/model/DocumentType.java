package org.example.bdoc.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum DocumentType {
    @XmlEnumValue("book")
    BOOK,

    @XmlEnumValue("journal")
    JOURNAL,

    @XmlEnumValue("article")
    ARTICLE,

    @XmlEnumValue("manuscript")
    MANUSCRIPT
}