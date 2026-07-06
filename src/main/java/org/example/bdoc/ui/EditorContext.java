package org.example.bdoc.ui;

import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.*;
import org.example.bdoc.ui.task.TaskQueue;

/**
 * Строго очерченный фасад ядра для плагинов (аналог DataContext/AnActionEvent
 * в IntelliJ Platform). Стратегии, панели и валидаторы общаются с
 * приложением ИСКЛЮЧИТЕЛЬНО через этот интерфейс — прямой доступ к
 * BdocEditorApp запрещён архитектурным правилом.
 */
public interface EditorContext {

    DocumentHandle getDocument();
    PageModel getCurrentPage();
    MasterPage getCurrentMasterPage();
    int getCurrentPageIndex();

    BdocObject getSelectedObject();
    void setSelectedObject(BdocObject object);

    /** Материализует override поверх мастер-объекта, если объект пока "сырой" master-locked. */
    BdocObject materializeOverrideIfNeeded(BdocObject object);

    void renderCurrentPage();
    void refreshTree();
    void setStatusText(String text);
    void showError(String title, String message);

    /** Преобразует экранные координаты канваса в локальные координаты объекта с учётом Transform. */
    double[] toLocalPoint(double screenX, double screenY, BdocObject object);

    /**
     * Единственный разрешённый способ изменить модель документа (Пункт 9.2).
     * Оборачивает мутацию в транзакцию: в будущем здесь появится блокировка
     * холста, атомарная запись CBOR и запись шага в Undo/Redo-историю.
     * Прямые вызовы object.getGeometry().setX(...) вне runWriteAction()
     * считаются архитектурным нарушением.
     */
    void runWriteAction(Runnable mutation);

    /** Точка входа для тяжёлых фоновых операций (Пункт 9.1) — см. TaskQueue. */
    TaskQueue getTaskQueue();

    /**
     * Заменяет PathModel у VectorShape на странице (используется при
     * Drag/Resize фигур с непримитивным контуром — Bezier-путь должен
     * масштабироваться вместе с bounding box). Возвращает обновлённый
     * объект, который следует использовать как новый selectedObject.
     */
    BdocObject replacePathData(BdocObject object, PathModel newPathData);

    BdocObject restoreToMaster(BdocObject overrideObject);
}