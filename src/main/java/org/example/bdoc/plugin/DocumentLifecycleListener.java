package org.example.bdoc.plugin;

import org.example.bdoc.io.DocumentHandle;

/**
 * Слушатель жизненного цикла документа. Плагины подписываются через
 * PluginContext.registerLifecycleListener(...), чтобы реагировать на
 * открытие/сохранение/смену страницы без модификации ядра BdocEditorApp
 * (например: авто-сохранение, ИИ-индексация текста, прогрев кэша ресурсов).
 */
public interface DocumentLifecycleListener {
    default void onDocumentOpened(DocumentHandle document) {}
    default void onDocumentClosed(DocumentHandle document) {}
    default void onDocumentSaved(DocumentHandle document) {}
    default void onActivePageChanged(DocumentHandle document, int pageIndex) {}
}