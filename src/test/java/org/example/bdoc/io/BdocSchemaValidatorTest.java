package org.example.bdoc.io;

import org.example.bdoc.model.DocumentModel;
import org.example.bdoc.model.SampleDocuments;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BdocSchemaValidatorTest {

    private final BdocXmlSerializer serializer = new BdocXmlSerializer();

    @Test
    void validXmlPassesSchema() {
        DocumentModel document = SampleDocuments.sample();
        String xml = serializer.toXml(document);

        assertDoesNotThrow(() -> serializer.validate(xml));
    }

    @Test
    void brokenXmlFailsSchemaWhenStoryRefDoesNotExist() {
        String invalidXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <document xmlns="urn:bdoc:v0.1" id="doc-1" title="Bad Doc" documentType="book">
                    <pages>
                        <page id="page-1" index="1" width="595.0" height="842.0">
                            <layers>
                                <layer id="layer-text" name="Text" role="text" visible="true" zIndex="1"/>
                            </layers>
                            <objects>
                                <textFrame id="text-1" layerRef="layer-text" storyRef="story-missing">
                                    <geometry x="10.0" y="10.0" width="100.0" height="50.0"/>
                                </textFrame>
                            </objects>
                        </page>
                    </pages>
                    <stories>
                        <story id="story-1">
                            <paragraph role="body">hello</paragraph>
                        </story>
                    </stories>
                </document>
                """;

        BdocValidationException ex = assertThrows(
                BdocValidationException.class,
                () -> serializer.validate(invalidXml)
        );

        assertTrue(ex.getMessage().contains("story-missing") || ex.getMessage().contains("IDREF"));
    }

    @Test
    void brokenXmlFailsSchemaWhenParagraphRoleIsInvalid() {
        String invalidXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <document xmlns="urn:bdoc:v0.1" id="doc-1" title="Bad Doc" documentType="book">
                    <pages>
                        <page id="page-1" index="1" width="595.0" height="842.0">
                            <layers>
                                <layer id="layer-text" name="Text" role="text" visible="true" zIndex="1"/>
                            </layers>
                            <objects/>
                        </page>
                    </pages>
                    <stories>
                        <story id="story-1">
                            <paragraph role="invalid-role">hello</paragraph>
                        </story>
                    </stories>
                </document>
                """;

        BdocValidationException ex = assertThrows(
                BdocValidationException.class,
                () -> serializer.validate(invalidXml)
        );

        assertTrue(ex.getMessage().contains("invalid-role"));
    }
}