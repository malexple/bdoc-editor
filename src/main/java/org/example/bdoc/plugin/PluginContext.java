package org.example.bdoc.plugin;

import org.example.bdoc.ui.properties.PropertiesPanelFactory;
import org.example.bdoc.ui.tool.DtpToolStrategy;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Центральный микро-контейнер зависимостей (аналог IntelliJ ExtensionPointName
 * + Application-level сервисов). Синглтон осознанно: в v0.1 одно окно —
 * один документ, полноценный DI-фреймворк избыточен на этом этапе.
 */
public final class PluginContext {
    private static final PluginContext INSTANCE = new PluginContext();

    // Reverse-domain namespace плагина должен состоять хотя бы из двух сегментов,
    // разделённых точкой, например "org.example.aicleaner" (Пункт 9.3).
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)+$");

    private final Map<String, DtpToolStrategy> toolRegistry = new LinkedHashMap<>();
    private final List<PropertiesPanelFactory> propertiesFactories = new ArrayList<>();
    private final Map<String, ValidatorExtension> validatorRegistry = new LinkedHashMap<>();
    private final List<DocumentLifecycleListener> lifecycleListeners = new ArrayList<>();
    private final Map<String, PluginDescriptor> registeredPlugins = new LinkedHashMap<>();

    private PluginContext() {}

    public static PluginContext getInstance() { return INSTANCE; }

    public void registerPlugin(PluginDescriptor descriptor) {
        if (!NAMESPACE_PATTERN.matcher(descriptor.getNamespace()).matches()) {
            throw new IllegalArgumentException("Plugin '" + descriptor.getId()
                    + "' has invalid namespace '" + descriptor.getNamespace()
                    + "': must be reverse-domain, e.g. org.example.myplugin");
        }
        registeredPlugins.put(descriptor.getId(), descriptor);
    }

    public Collection<PluginDescriptor> getRegisteredPlugins() {
        return registeredPlugins.values();
    }

    /** Проверяет, что customData-ключ начинается с namespace одного из зарегистрированных плагинов (Пункт 9.3). */
    public boolean isKnownNamespacedKey(String key) {
        for (PluginDescriptor descriptor : registeredPlugins.values()) {
            if (key.startsWith(descriptor.getNamespace() + ".")) {
                return true;
            }
        }
        return false;
    }

    public void registerTool(DtpToolStrategy tool) {
        toolRegistry.put(tool.getToolId(), tool);
    }

    public DtpToolStrategy getTool(String toolId) { return toolRegistry.get(toolId); }
    public Map<String, DtpToolStrategy> getRegisteredTools() { return toolRegistry; }

    public void registerPropertiesFactory(PropertiesPanelFactory factory) {
        propertiesFactories.add(factory);
    }

    /** Первая подходящая фабрика выигрывает — плагины регистрируются после встроенных, поэтому могут переопределить их. */
    public PropertiesPanelFactory findPropertiesFactory(org.example.bdoc.model.BdocObject object) {
        for (int i = propertiesFactories.size() - 1; i >= 0; i--) {
            PropertiesPanelFactory factory = propertiesFactories.get(i);
            if (factory.supports(object)) return factory;
        }
        return null;
    }

    public void registerValidator(ValidatorExtension validator) {
        validatorRegistry.put(validator.getId(), validator);
    }

    public Collection<ValidatorExtension> getRegisteredValidators() { return validatorRegistry.values(); }

    public void registerLifecycleListener(DocumentLifecycleListener listener) {
        lifecycleListeners.add(listener);
    }

    public List<DocumentLifecycleListener> getLifecycleListeners() { return lifecycleListeners; }
}