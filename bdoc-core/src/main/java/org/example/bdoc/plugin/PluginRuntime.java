package org.example.bdoc.plugin;

/**
 * Глобальный держатель runtime-слоя модулей плагинов (аналог решения
 * из статьи "Плагинное приложение на Java без боли", habr.com/ru/articles/479478).
 * Заполняется один раз в Main.main() до запуска JavaFX Application,
 * так как JavaFX сам создаёт экземпляр Application через reflection
 * и передать слой напрямую в конструктор невозможно.
 */
public final class PluginRuntime {

    private static volatile ModuleLayer pluginLayer;

    private PluginRuntime() {
    }

    public static void setPluginLayer(ModuleLayer layer) {
        pluginLayer = layer;
    }

    /**
     * Возвращает слой плагинов, если он был создан, иначе boot layer
     * (это безопасный fallback для тестов/запуска без Main).
     */
    public static ModuleLayer getLayer() {
        ModuleLayer layer = pluginLayer;
        return layer != null ? layer : ModuleLayer.boot();
    }
}