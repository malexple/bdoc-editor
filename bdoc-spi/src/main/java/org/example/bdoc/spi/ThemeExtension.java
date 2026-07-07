package org.example.bdoc.spi;

public interface ThemeExtension {
    String getThemeId();
    String getThemeName();
    boolean isDark();
    String getCssResourcePath();
}