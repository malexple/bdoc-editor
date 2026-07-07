package org.example.bdoc;

import javafx.application.Application;
import org.example.bdoc.ui.BdocEditorApp;

public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(BdocEditorApp.class, args);
    }
}