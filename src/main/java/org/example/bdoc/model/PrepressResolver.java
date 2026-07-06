package org.example.bdoc.model;

/**
 * Разрешает bleedMargin / safetyMargin / printMarksSettings по трёхуровневому
 * каскаду: PageModel -> MasterPage -> Manifest -> системный дефолт.
 * printMarksSettings разрешается целиком объектом (не по отдельным полям) —
 * это осознанное упрощение декларативной модели (Вопрос 4).
 */
public final class PrepressResolver {

    private static final double DEFAULT_MARGIN = 0.0;

    private PrepressResolver() {
    }

    public static double resolveBleedMargin(PageModel page, MasterPage masterPage, Manifest manifest) {
        if (page.getBleedMargin() != null) return page.getBleedMargin();
        if (masterPage != null && masterPage.getBleedMargin() != null) return masterPage.getBleedMargin();
        if (manifest != null && manifest.getDefaultBleedMargin() != null) return manifest.getDefaultBleedMargin();
        return DEFAULT_MARGIN;
    }

    public static double resolveSafetyMargin(PageModel page, MasterPage masterPage, Manifest manifest) {
        if (page.getSafetyMargin() != null) return page.getSafetyMargin();
        if (masterPage != null && masterPage.getSafetyMargin() != null) return masterPage.getSafetyMargin();
        if (manifest != null && manifest.getDefaultSafetyMargin() != null) return manifest.getDefaultSafetyMargin();
        return DEFAULT_MARGIN;
    }

    public static PrintMarksSettings resolvePrintMarksSettings(PageModel page, MasterPage masterPage, Manifest manifest) {
        if (page.getPrintMarksSettings() != null) return page.getPrintMarksSettings();
        if (masterPage != null && masterPage.getPrintMarksSettings() != null) return masterPage.getPrintMarksSettings();
        if (manifest != null && manifest.getDefaultPrintMarksSettings() != null) return manifest.getDefaultPrintMarksSettings();
        return PrintMarksSettings.disabled();
    }
}