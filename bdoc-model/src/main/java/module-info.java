module org.example.bdoc.model {
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.cbor;
    requires jakarta.xml.bind;

    exports org.example.bdoc.model;
    opens org.example.bdoc.model to com.fasterxml.jackson.databind;
}