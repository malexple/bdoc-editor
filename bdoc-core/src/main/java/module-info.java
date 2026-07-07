module org.example.bdoc.core {
    requires org.example.bdoc.model;
    requires org.example.bdoc.io;
    requires org.example.bdoc.spi;

    requires javafx.controls;
    requires javafx.graphics;
    requires java.prefs;
    requires jakarta.xml.bind;

    exports org.example.bdoc.plugin;
    exports org.example.bdoc.render;
    exports org.example.bdoc.i18n;
}