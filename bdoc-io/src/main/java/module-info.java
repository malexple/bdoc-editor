module org.example.bdoc.io {
    requires org.example.bdoc.model;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.cbor;

    exports org.example.bdoc.io;
}