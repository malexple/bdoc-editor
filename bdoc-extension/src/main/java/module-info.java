module org.example.bdoc.extension {
    requires org.example.bdoc.core;
    requires org.example.bdoc.spi;

    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires java.prefs;

    exports org.example.bdoc.plugin;
}