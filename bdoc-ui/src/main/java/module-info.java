module org.example.bdoc.ui {
    requires org.example.bdoc.core;
    requires org.example.bdoc.spi;
    requires org.example.bdoc.extension;

    requires javafx.controls;
    requires java.desktop;
    requires javafx.graphics;
    requires javafx.swing;
    requires jakarta.xml.bind;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.cbor;

    opens org.example.bdoc.ui to javafx.graphics;
    opens org.example.bdoc.ui.properties to javafx.graphics;
    opens org.example.bdoc.ui.tool to javafx.graphics;

    exports org.example.bdoc.ui;
}