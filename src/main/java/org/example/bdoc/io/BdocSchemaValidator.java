package org.example.bdoc.io;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class BdocSchemaValidator {

    public static final String DEFAULT_SCHEMA_CLASSPATH = "xsd/bdoc-v0.1.xsd";

    private final Schema schema;

    public BdocSchemaValidator() {
        this(DEFAULT_SCHEMA_CLASSPATH);
    }

    public BdocSchemaValidator(String classpathLocation) {
        this.schema = loadSchemaFromClasspath(classpathLocation);
    }

    public BdocSchemaValidator(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        return schema;
    }

    public void validate(File file) {
        validate(new StreamSource(file));
    }

    public void validate(String xml) {
        validate(new StreamSource(new StringReader(xml)));
    }

    public void validate(Reader reader) {
        validate(new StreamSource(reader));
    }

    public void validate(Source source) {
        try {
            Validator validator = schema.newValidator();
            validator.validate(source);
        } catch (SAXParseException e) {
            throw new BdocValidationException(
                    "BDoc XML failed XSD validation",
                    List.of(formatSaxParseException(e))
            );
        } catch (SAXException e) {
            throw new BdocValidationException(
                    "BDoc XML failed XSD validation",
                    List.of(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate XML against schema", e);
        }
    }

    private static Schema loadSchemaFromClasspath(String classpathLocation) {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(classpathLocation)) {

            if (is == null) {
                throw new IllegalArgumentException("Schema not found in classpath: " + classpathLocation);
            }

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            return schemaFactory.newSchema(new StreamSource(is));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load XSD schema from classpath: " + classpathLocation, e);
        }
    }

    public static String formatSaxParseException(SAXParseException e) {
        return "line=" + e.getLineNumber()
                + ", column=" + e.getColumnNumber()
                + ": " + e.getMessage();
    }
}