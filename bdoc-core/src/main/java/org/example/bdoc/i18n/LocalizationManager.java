package org.example.bdoc.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Единая точка локализации ядра и плагинов.
 */
public final class LocalizationManager {
    private static final LocalizationManager INSTANCE = new LocalizationManager();

    private ResourceBundle bundle;
    private Locale currentLocale;

    private LocalizationManager() {
        setLocale(Locale.getDefault());
    }

    public static LocalizationManager getInstance() {
        return INSTANCE;
    }

    public void setLocale(Locale locale) {
        this.currentLocale = locale;
        this.bundle = ResourceBundle.getBundle("i18n.messages", locale);
    }

    public Locale getCurrentLocale() {
        if (currentLocale != null) {
            return currentLocale;
        }
        if (bundle != null) {
            return bundle.getLocale();
        }
        return Locale.getDefault();
    }

    public String get(String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }
}