module org.example.bdoc.ui {
    requires org.example.bdoc.model;
    requires org.example.bdoc.io;
    requires org.example.bdoc.spi;
    requires org.example.bdoc.core;
    requires java.prefs;

    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.swing;
    requires java.desktop;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.cbor;

    exports org.example.bdoc.ui;

    uses org.example.bdoc.spi.ToolbarActionExtension;
}