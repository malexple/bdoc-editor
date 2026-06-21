package org.example.bdoc.model;

public sealed interface BdocObject permits TextFrame, VectorShape {
    String id();
    String layerId();
}