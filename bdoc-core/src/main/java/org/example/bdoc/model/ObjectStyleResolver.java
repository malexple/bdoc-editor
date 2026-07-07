package org.example.bdoc.model;

/**
 * Каскадный резолвер параметров ObjectStyle (Этап 1.6), зеркалит паттерн
 * StyleResolver/CharacterStyleResolver для текста. Порядок разрешения:
 * локальное значение объекта → objectStyleRef → basedOn стиля → системный дефолт.
 * Модель (BdocObject) ничего не знает о StylesCatalog — весь каскад собирается
 * здесь, снаружи, вызывающим кодом (PageRenderer).
 */
public final class ObjectStyleResolver {

    private ObjectStyleResolver() {
    }

    public static double resolveArcWidth(BdocObject object, StylesCatalog catalog) {
        if (object.getGeometry() != null && object.getGeometry().getArcWidth() != null) {
            return object.getGeometry().getArcWidth();
        }
        ObjectStyle style = firstStyleInChain(object, catalog);
        while (style != null) {
            if (style.getArcWidth() != null) {
                return style.getArcWidth();
            }
            style = nextInChain(style, catalog);
        }
        return 0.0;
    }

    public static double resolveArcHeight(BdocObject object, StylesCatalog catalog) {
        if (object.getGeometry() != null && object.getGeometry().getArcHeight() != null) {
            return object.getGeometry().getArcHeight();
        }
        ObjectStyle style = firstStyleInChain(object, catalog);
        while (style != null) {
            if (style.getArcHeight() != null) {
                return style.getArcHeight();
            }
            style = nextInChain(style, catalog);
        }
        return 0.0;
    }

    public static double resolveOpacity(BdocObject object, StylesCatalog catalog) {
        if (object.getOpacity() != null) {
            return object.getOpacity();
        }
        ObjectStyle style = firstStyleInChain(object, catalog);
        while (style != null) {
            if (style.getOpacity() != null) {
                return style.getOpacity();
            }
            style = nextInChain(style, catalog);
        }
        return 1.0;
    }

    public static TextWrapModel resolveTextWrap(BdocObject object, StylesCatalog catalog) {
        if (isExplicitWrap(object.getTextWrap())) {
            return object.getTextWrap();
        }
        ObjectStyle style = firstStyleInChain(object, catalog);
        while (style != null) {
            if (isExplicitWrap(style.getTextWrap())) {
                return style.getTextWrap();
            }
            style = nextInChain(style, catalog);
        }
        return TextWrapModel.disabled();
    }

    private static boolean isExplicitWrap(TextWrapModel wrap) {
        return wrap != null && wrap.getMode() != null && !wrap.getMode().equalsIgnoreCase("none");
    }

    private static ObjectStyle firstStyleInChain(BdocObject object, StylesCatalog catalog) {
        if (object.getObjectStyleRef() == null || catalog == null) {
            return null;
        }
        return catalog.findObjectStyle(object.getObjectStyleRef());
    }

    private static ObjectStyle nextInChain(ObjectStyle style, StylesCatalog catalog) {
        return style.getBasedOn() != null ? catalog.findObjectStyle(style.getBasedOn()) : null;
    }
}