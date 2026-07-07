module org.example.bdoc.core {
    requires javafx.graphics;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.cbor;
    requires jakarta.xml.bind;

    exports org.example.bdoc.io;
    exports org.example.bdoc.model;
    exports org.example.bdoc.render;
    exports org.example.bdoc.i18n;

    opens org.example.bdoc.model to com.fasterxml.jackson.databind;
}