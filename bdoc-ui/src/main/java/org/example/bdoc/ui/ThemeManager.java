package org.example.bdoc.ui;

import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.net.URL;

public final class ThemeManager {

    public static final String OBSIDIAN_INK = "obsidian-ink";
    public static final String PAPER_MATTE = "paper-matte";

    private boolean fontsLoaded = false;

    public void apply(Scene scene, String themeId) {
        loadFontsIfNeeded();

        scene.getStylesheets().clear();

        addIfExists(scene, "/theme/base.css");

        String themePath = resolveThemePath(themeId);
        addIfExists(scene, themePath);
    }

    private void loadFontsIfNeeded() {
        if (fontsLoaded) {
            return;
        }

        loadFontFamily("/fonts/inter/Inter-Regular.ttf");
        loadFontFamily("/fonts/inter/Inter-Medium.ttf");
        loadFontFamily("/fonts/inter/Inter-SemiBold.ttf");
        loadFontFamily("/fonts/inter/Inter-Bold.ttf");

        loadFontFamily("/fonts/jetbrains-mono/JetBrainsMono-Regular.ttf");
        loadFontFamily("/fonts/jetbrains-mono/JetBrainsMono-Medium.ttf");

        fontsLoaded = true;
    }

    private void loadFontFamily(String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url != null) {
            Font.loadFont(url.toExternalForm(), 12);
        }
    }

    private String resolveThemePath(String themeId) {
        if (PAPER_MATTE.equalsIgnoreCase(themeId)) {
            return "/theme/paper-matte.css";
        }
        return "/theme/obsidian-ink.css";
    }

    private void addIfExists(Scene scene, String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }
}