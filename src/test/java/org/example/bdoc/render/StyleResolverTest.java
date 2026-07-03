package org.example.bdoc.render;

import org.example.bdoc.model.ParagraphStyle;
import org.example.bdoc.model.StylesCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StyleResolverTest {

    @Test
    void childInheritsUnsetPropertiesFromParent() {
        ParagraphStyle parent = new ParagraphStyle(
                "body-text", null, "Georgia", 16.0, 1.4, "left", "#1E293B");
        ParagraphStyle child = new ParagraphStyle(
                "heading-1", "body-text", null, 26.0, null, "center", "#0F172A");

        StyleResolver resolver = new StyleResolver(
                new StylesCatalog(List.of(parent, child), List.of()));

        EffectiveParagraphStyle effective = resolver.resolve("heading-1");

        assertEquals("Georgia", effective.getFontFamily());
        assertEquals(26.0, effective.getFontSize());
        assertEquals(1.4, effective.getLineHeight());
        assertEquals("center", effective.getAlignment());
        assertEquals("#0F172A", effective.getColor());
    }

    @Test
    void missingStyleRefFallsBackToDefaults() {
        StyleResolver resolver = new StyleResolver(StylesCatalog.empty());

        EffectiveParagraphStyle effective = resolver.resolve("does-not-exist");

        assertEquals("Serif", effective.getFontFamily());
        assertEquals(20.0, effective.getFontSize());
    }

    @Test
    void cyclicBasedOnThrows() {
        ParagraphStyle a = new ParagraphStyle("a", "b", null, null, null, null, null);
        ParagraphStyle b = new ParagraphStyle("b", "a", null, null, null, null, null);

        StyleResolver resolver = new StyleResolver(
                new StylesCatalog(List.of(a, b), List.of()));

        assertThrows(IllegalStateException.class, () -> resolver.resolve("a"));
    }
}