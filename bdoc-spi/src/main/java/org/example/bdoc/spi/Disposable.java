package org.example.bdoc.spi;

/**
 * Контракт управления жизненным циклом (аналог com.intellij.openapi.Disposable).
 * Любая стратегия, панель или сервис, удерживающий ресурсы (кэши изображений,
 * сетевые сокеты, JavaFX-слушатели), обязан реализовать dispose() и
 * освободить их. Ядро вызывает dispose() при деактивации инструмента
 * (DtpToolStrategy.deactivate) и при закрытии документа (DocumentHandle.close).
 */
public interface Disposable {
    void dispose();
}