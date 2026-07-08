package org.example.bdoc.ui;

import org.example.bdoc.io.BdocValidationException;
import org.example.bdoc.model.BdocObject;

public interface DocumentEventSink {
    void onDocumentOpened();
    void onPageChanged(int pageIndex);
    void onObjectSelected(BdocObject object);
    void onDataChanged();
    void onRecentFilesChanged();
    void showError(String title, String message);
    void showValidationErrors(String title, BdocValidationException validationEx);
}