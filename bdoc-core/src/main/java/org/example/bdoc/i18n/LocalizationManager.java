package org.example.bdoc.i18n;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public final class LocalizationManager {
    private static final String DEFAULT_BUNDLE = "i18n.messages";
    private static final LocalizationManager INSTANCE = new LocalizationManager();

    private final ObjectProperty<Locale> locale = new SimpleObjectProperty<>();
    private final ReadOnlyObjectWrapper<ResourceBundle> bundle = new ReadOnlyObjectWrapper<>();
    private final Map<String, BundleRegistration> bundles = new HashMap<>();

    private LocalizationManager() {
        registerBundle("core", DEFAULT_BUNDLE, LocalizationManager.class.getClassLoader());
        locale.addListener((obs, oldLocale, newLocale) -> reloadBundles(newLocale));
        setLocale(Locale.getDefault());
    }

    public static LocalizationManager getInstance() {
        return INSTANCE;
    }

    public void registerBundle(String ownerId, String baseName, ClassLoader loader) {
        bundles.put(ownerId, new BundleRegistration(baseName, loader));
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
        return get("core", key);
    }

    public String get(String ownerId, String key) {
        ResourceBundle rb = resolveBundle(ownerId);
        if (rb == null) {
            return key;
        }
        return rb.containsKey(key) ? rb.getString(key) : key;
    }

    public StringBinding createStringBinding(String key) {
        return Bindings.createStringBinding(() -> get(key), bundleProperty());
    }

    public StringBinding createStringBinding(String ownerId, String key) {
        return Bindings.createStringBinding(() -> get(ownerId, key), bundleProperty());
    }

    private void reloadBundles(Locale targetLocale) {
        Locale effective = targetLocale != null ? targetLocale : Locale.getDefault();

        for (Map.Entry<String, BundleRegistration> entry : bundles.entrySet()) {
            BundleRegistration reg = entry.getValue();
            reg.bundle = ResourceBundle.getBundle(reg.baseName, effective, reg.loader);
        }

        BundleRegistration core = bundles.get("core");
        bundle.set(core != null ? core.bundle : null);
    }

    private ResourceBundle resolveBundle(String ownerId) {
        BundleRegistration reg = bundles.get(ownerId);
        return reg != null ? reg.bundle : bundle.get();
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