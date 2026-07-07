package org.example.bdoc.spi;

import org.example.bdoc.io.DocumentHandle;
import org.example.bdoc.model.BdocObject;
import org.example.bdoc.model.MasterPage;
import org.example.bdoc.model.PageModel;
import org.example.bdoc.model.PathModel;

public interface EditorContext {

    DocumentHandle getDocument();

    PageModel getCurrentPage();

    MasterPage getCurrentMasterPage();

    int getCurrentPageIndex();

    BdocObject getSelectedObject();

    void setSelectedObject(BdocObject object);

    BdocObject materializeOverrideIfNeeded(BdocObject object);

    void renderCurrentPage();

    void refreshTree();

    void setStatusText(String text);

    void showError(String title, String message);

    double[] toLocalPoint(double screenX, double screenY, BdocObject object);

    void runWriteAction(Runnable mutation);

    BdocObject replacePathData(BdocObject object, PathModel newPathData);

    BdocObject restoreToMaster(BdocObject overrideObject);
}