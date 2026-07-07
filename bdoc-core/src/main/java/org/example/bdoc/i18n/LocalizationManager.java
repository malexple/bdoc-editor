package org.example.bdoc.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Единая точка локализации ядра и плагинов (аналог
 * com.intellij.openapi.util.NlsContexts + ResourceBundle-based message бины).
 * Плагины должны класть свои .properties в тот же формат bundle-ключей
 * с префиксом namespace, например "org.example.aicleaner.button.run".
 */
public final class LocalizationManager {
    private static final LocalizationManager INSTANCE = new LocalizationManager();

    private ResourceBundle bundle;

    private LocalizationManager() {
        setLocale(Locale.getDefault());
    }

    public static LocalizationManager getInstance() { return INSTANCE; }

    public void setLocale(Locale locale) {
        this.bundle = ResourceBundle.getBundle("i18n.messages", locale);
    }

    public String get(String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }
}