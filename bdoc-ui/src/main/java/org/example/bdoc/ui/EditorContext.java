package org.example.bdoc.ui;

import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.*;
import org.example.bdoc.ui.task.TaskQueue;

public interface EditorContext extends org.example.bdoc.spi.EditorContext {

    @Override
    DocumentHandle getDocument();

    PageModel getCurrentPage();

    MasterPage getCurrentMasterPage();

    int getCurrentPageIndex();

    @Override
    BdocObject getSelectedObject();

    @Override
    void setSelectedObject(BdocObject object);

    BdocObject materializeOverrideIfNeeded(BdocObject object);

    void renderCurrentPage();

    void refreshTree();

    void setStatusText(String text);

    void showError(String title, String message);

    double[] toLocalPoint(double screenX, double screenY, BdocObject object);

    @Override
    void runWriteAction(Runnable mutation);

    TaskQueue getTaskQueue();

    BdocObject replacePathData(BdocObject object, PathModel newPathData);

    BdocObject restoreToMaster(BdocObject overrideObject);
}