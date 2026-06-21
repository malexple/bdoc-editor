package org.example.bdoc.io;

import jakarta.xml.bind.*;
import jakarta.xml.bind.util.ValidationEventCollector;
import org.example.bdoc.model.DocumentModel;
import org.example.bdoc.model.TextFrame;
import org.example.bdoc.model.VectorShape;

import javax.xml.validation.Schema;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class BdocXmlSerializer {

    private final JAXBContext context;
    private final BdocSchemaValidator schemaValidator;
    private final Schema schema;

    public BdocXmlSerializer() {
        this(new BdocSchemaValidator());
    }

    public BdocXmlSerializer(BdocSchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator;
        this.schema = schemaValidator.getSchema();
        try {
            this.context = JAXBContext.newInstance(
                    DocumentModel.class,
                    TextFrame.class,
                    VectorShape.class
            );
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to initialize JAXB context", e);
        }
    }

    public void save(DocumentModel document, File file) {
        try {
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setSchema(schema);

            ValidationEventCollector collector = new ValidationEventCollector();
            marshaller.setEventHandler(collector);

            marshaller.marshal(document, file);

            throwIfValidationErrors("BDoc object failed XSD validation during save", collector);
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to save document: " + file, e);
        }
    }

    public String toXml(DocumentModel document) {
        try {
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setSchema(schema);

            ValidationEventCollector collector = new ValidationEventCollector();
            marshaller.setEventHandler(collector);

            StringWriter writer = new StringWriter();
            marshaller.marshal(document, writer);

            throwIfValidationErrors("BDoc object failed XSD validation during XML serialization", collector);
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to serialize document", e);
        }
    }

    public DocumentModel load(File file) {
        schemaValidator.validate(file);

        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            unmarshaller.setSchema(schema);

            ValidationEventCollector collector = new ValidationEventCollector();
            unmarshaller.setEventHandler(collector);

            DocumentModel document = (DocumentModel) unmarshaller.unmarshal(file);
            throwIfValidationErrors("BDoc XML failed XSD validation during load", collector);
            return document;
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to load document: " + file, e);
        }
    }

    public DocumentModel fromXml(String xml) {
        schemaValidator.validate(xml);

        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            unmarshaller.setSchema(schema);

            ValidationEventCollector collector = new ValidationEventCollector();
            unmarshaller.setEventHandler(collector);

            DocumentModel document = (DocumentModel) unmarshaller.unmarshal(new StringReader(xml));
            throwIfValidationErrors("BDoc XML failed XSD validation during load", collector);
            return document;
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to deserialize document", e);
        }
    }

    public void validate(File file) {
        schemaValidator.validate(file);
    }

    public void validate(String xml) {
        schemaValidator.validate(xml);
    }

    private static void throwIfValidationErrors(String message, ValidationEventCollector collector) {
        if (collector == null || !collector.hasEvents()) {
            return;
        }

        List<String> errors = new ArrayList<>();
        for (ValidationEvent event : collector.getEvents()) {
            StringBuilder sb = new StringBuilder();

            sb.append(severity(event.getSeverity())).append(": ");
            if (event.getMessage() != null) {
                sb.append(event.getMessage());
            } else {
                sb.append("validation error");
            }

            if (event.getLocator() != null) {
                if (event.getLocator().getLineNumber() > 0) {
                    sb.append(" [line=").append(event.getLocator().getLineNumber()).append("]");
                }
                if (event.getLocator().getColumnNumber() > 0) {
                    sb.append(" [column=").append(event.getLocator().getColumnNumber()).append("]");
                }
                if (event.getLocator().getNode() != null) {
                    sb.append(" [node=").append(event.getLocator().getNode().getNodeName()).append("]");
                }
            }

            errors.add(sb.toString());
        }

        throw new BdocValidationException(message, errors);
    }

    private static String severity(int severity) {
        return switch (severity) {
            case ValidationEvent.WARNING -> "WARNING";
            case ValidationEvent.ERROR -> "ERROR";
            case ValidationEvent.FATAL_ERROR -> "FATAL";
            default -> "UNKNOWN";
        };
    }
}