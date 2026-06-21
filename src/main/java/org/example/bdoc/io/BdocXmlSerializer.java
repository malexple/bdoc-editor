package org.example.bdoc.io;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.example.bdoc.model.DocumentModel;
import org.example.bdoc.model.TextFrame;
import org.example.bdoc.model.VectorShape;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

public class BdocXmlSerializer {

    private final JAXBContext context;

    public BdocXmlSerializer() {
        try {
            context = JAXBContext.newInstance(
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
            marshaller.marshal(document, file);
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to save document: " + file, e);
        }
    }

    public String toXml(DocumentModel document) {
        try {
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            StringWriter writer = new StringWriter();
            marshaller.marshal(document, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to serialize document", e);
        }
    }

    public DocumentModel load(File file) {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (DocumentModel) unmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to load document: " + file, e);
        }
    }

    public DocumentModel fromXml(String xml) {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (DocumentModel) unmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to deserialize document", e);
        }
    }
}