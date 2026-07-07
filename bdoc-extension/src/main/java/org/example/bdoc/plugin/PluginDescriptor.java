package org.example.bdoc.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Декларативный манифест плагина (аналог plugin.xml в IntelliJ Platform).
 * В текущем патче (Вопрос 7) загружается только программно — реальный
 * PluginLoader со сканированием plugins/*.zip и URLClassLoader откладывается
 * на будущий этап. namespace используется как обязательный префикс для
 * customData-ключей объекта (Пункт 9.3).
 */
public final class PluginDescriptor {
    private final String id;
    private final String namespace; // reverse-domain, например "org.example.aicleaner"
    private final String name;
    private final String version;
    private final String vendor;
    private final List<ExtensionPoint> extensions = new ArrayList<>();

    public PluginDescriptor(String id, String namespace, String name, String version, String vendor) {
        this.id = id;
        this.namespace = namespace;
        this.name = name;
        this.version = version;
        this.vendor = vendor;
    }

    public String getId() { return id; }
    public String getNamespace() { return namespace; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getVendor() { return vendor; }
    public List<ExtensionPoint> getExtensions() { return extensions; }

    public static class ExtensionPoint {
        public String point; // "tools" | "properties" | "validators" | "lifecycle"
        public String id;
        public String implementationClass;
        public String iconPath;
        public String label;
    }
}