package org.example.bdoc.i18n;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class LocalizationManager {
    private static final String DEFAULT_BUNDLE = "i18n.messages";
    private static final String CORE_OWNER_ID = "core";
    private static final LocalizationManager INSTANCE = new LocalizationManager();

    private final ObjectProperty<Locale> locale = new SimpleObjectProperty<>(Locale.getDefault());
    private final ReadOnlyObjectWrapper<ResourceBundle> bundle = new ReadOnlyObjectWrapper<>();
    private final IntegerProperty bundleVersion = new SimpleIntegerProperty(0);
    private final Map<String, BundleRegistration> bundles = new LinkedHashMap<>();

    private LocalizationManager() {
        registerBundle(CORE_OWNER_ID, DEFAULT_BUNDLE, LocalizationManager.class.getClassLoader());
        locale.addListener((obs, oldLocale, newLocale) -> reloadBundles(newLocale));
        reloadBundles(locale.get());
    }

    public static LocalizationManager getInstance() {
        return INSTANCE;
    }

    public synchronized void registerBundle(String ownerId, String baseName, ClassLoader loader) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be null or blank");
        }
        if (baseName == null || baseName.isBlank()) {
            throw new IllegalArgumentException("baseName must not be null or blank");
        }
        ClassLoader effectiveLoader = loader != null ? loader : LocalizationManager.class.getClassLoader();
        bundles.put(ownerId, new BundleRegistration(baseName, effectiveLoader));
        reloadBundles(getCurrentLocale());
    }

    public Locale getCurrentLocale() {
        Locale current = locale.get();
        return current != null ? current : Locale.getDefault();
    }

    public void setLocale(Locale locale) {
        this.locale.set(locale != null ? locale : Locale.getDefault());
    }

    public ObjectProperty<Locale> localeProperty() {
        return locale;
    }

    public ResourceBundle getBundle() {
        return bundle.get();
    }

    public ReadOnlyObjectProperty<ResourceBundle> bundleProperty() {
        return bundle.getReadOnlyProperty();
    }

    public String get(String key) {
        return get(CORE_OWNER_ID, key);
    }

    public String get(String ownerId, String key) {
        if (key == null || key.isBlank()) {
            return "";
        }

        ResourceBundle rb = resolveBundle(ownerId);
        if (rb == null) {
            return key;
        }

        try {
            return rb.containsKey(key) ? rb.getString(key) : key;
        } catch (MissingResourceException ex) {
            return key;
        }
    }

    public StringBinding createStringBinding(String key) {
        return Bindings.createStringBinding(
                () -> get(key),
                localeProperty(),
                bundleProperty(),
                bundleVersion
        );
    }

    public StringBinding createStringBinding(String ownerId, String key) {
        return Bindings.createStringBinding(
                () -> get(ownerId, key),
                localeProperty(),
                bundleProperty(),
                bundleVersion
        );
    }

    private synchronized void reloadBundles(Locale targetLocale) {
        Locale effective = targetLocale != null ? targetLocale : Locale.getDefault();

        for (BundleRegistration reg : bundles.values()) {
            reg.bundle = loadBundleSafely(reg.baseName, effective, reg.loader);
        }

        BundleRegistration core = bundles.get(CORE_OWNER_ID);
        bundle.set(core != null ? core.bundle : null);
        bundleVersion.set(bundleVersion.get() + 1);
    }

    private ResourceBundle resolveBundle(String ownerId) {
        String effectiveOwnerId = (ownerId == null || ownerId.isBlank()) ? CORE_OWNER_ID : ownerId;
        BundleRegistration reg = bundles.get(effectiveOwnerId);
        if (reg != null && reg.bundle != null) {
            return reg.bundle;
        }

        BundleRegistration core = bundles.get(CORE_OWNER_ID);
        return core != null ? core.bundle : null;
    }

    private ResourceBundle loadBundleSafely(String baseName, Locale locale, ClassLoader loader) {
        try {
            return ResourceBundle.getBundle(baseName, locale, loader);
        } catch (MissingResourceException primaryEx) {
            try {
                return ResourceBundle.getBundle(baseName, Locale.ENGLISH, loader);
            } catch (MissingResourceException fallbackEx) {
                return null;
            }
        }
    }

    private static final class BundleRegistration {
        private final String baseName;
        private final ClassLoader loader;
        private ResourceBundle bundle;

        private BundleRegistration(String baseName, ClassLoader loader) {
            this.baseName = baseName;
            this.loader = loader;
        }
    }
}