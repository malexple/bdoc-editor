package org.example.bdoc.ui;

import org.example.bdoc.plugin.PluginRuntime;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Точка входа приложения. Отвечает только за создание runtime-слоя
 * модулей плагинов из папки plugins/ (см. статью на Хабре:
 * https://habr.com/ru/articles/479478/). Сам UI ничего не знает
 * о том, как устроены плагины физически — он только опрашивает
 * ServiceLoader через слой, который здесь создан.
 */
public final class Main {

    public static void main(String[] args) {
        Path pluginsDir = Paths.get("plugins");

        try {
            java.nio.file.Files.createDirectories(pluginsDir);
        } catch (java.io.IOException e) {
            System.err.println("Failed to create plugins directory: " + e.getMessage());
        }

        ModuleFinder pluginsFinder = ModuleFinder.of(pluginsDir);

        List<String> pluginModuleNames = pluginsFinder
                .findAll()
                .stream()
                .map(ModuleReference::descriptor)
                .map(ModuleDescriptor::name)
                .collect(Collectors.toList());

        System.out.println("Discovered plugin modules: " + pluginModuleNames);

        Configuration pluginsConfiguration = ModuleLayer.boot()
                .configuration()
                .resolve(pluginsFinder, ModuleFinder.of(), pluginModuleNames);

        ModuleLayer pluginLayer = ModuleLayer.boot()
                .defineModulesWithOneLoader(pluginsConfiguration, ClassLoader.getSystemClassLoader());

        PluginRuntime.setPluginLayer(pluginLayer);

        BdocEditorApp.launch(BdocEditorApp.class, args);
    }
}