package org.example.bdoc.plugin;

import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Централизованное хранилище пользовательских настроек (аналог
 * PersistentStateComponent + PropertiesComponent в IntelliJ Platform).
 * Инфраструктурный сервис ядра — лежит рядом с BdocBundle и PluginContext,
 * так как и UI, и будущие плагины должны иметь доступ к сохранению своих
 * настроек через node(pluginId) без риска коллизии ключей (Пункт 9.3).
 */
public final class BdocSettings {
    private static final BdocSettings INSTANCE = new BdocSettings();
    private static final int MAX_RECENT_FILES = 5;

    private final Preferences root = Preferences.userNodeForPackage(BdocSettings.class);

    private BdocSettings() { }

    public static BdocSettings getInstance() { return INSTANCE; }

    /** Изолированный узел для плагина — namespace должен совпадать с PluginDescriptor.getNamespace(). */
    public Preferences node(String namespace) {
        return root.node(namespace.replace('.', '_'));
    }

    // ---- Window bounds ----
    public void saveWindowBounds(double x, double y, double w, double h, boolean maximized) {
        Preferences p = root.node("window");
        p.putDouble("x", x);
        p.putDouble("y", y);
        p.putDouble("width", w);
        p.putDouble("height", h);
        p.putBoolean("maximized", maximized);
    }

    public double[] loadWindowBounds() {
        Preferences p = root.node("window");
        return new double[]{
                p.getDouble("x", 100),
                p.getDouble("y", 100),
                p.getDouble("width", 1360),
                p.getDouble("height", 900)
        };
    }

    public boolean isWindowMaximized() {
        return root.node("window").getBoolean("maximized", false);
    }

    // ---- SplitPane dividers ----
    public void saveDividerPositions(double... positions) {
        Preferences p = root.node("layout");
        for (int i = 0; i < positions.length; i++) {
            p.putDouble("divider" + i, positions[i]);
        }
    }

    public double[] loadDividerPositions(double[] defaults) {
        Preferences p = root.node("layout");
        double[] result = new double[defaults.length];
        for (int i = 0; i < defaults.length; i++) {
            result[i] = p.getDouble("divider" + i, defaults[i]);
        }
        return result;
    }

    // ---- Recent files (список последних 5) ----
    public void pushRecentFile(String absolutePath) {
        List<String> recent = loadRecentFiles();
        recent.remove(absolutePath);
        recent.add(0, absolutePath);
        while (recent.size() > MAX_RECENT_FILES) {
            recent.remove(recent.size() - 1);
        }
        Preferences p = root.node("recent");
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            if (i < recent.size()) {
                p.put("file" + i, recent.get(i));
            } else {
                p.remove("file" + i);
            }
        }
    }

    public List<String> loadRecentFiles() {
        Preferences p = root.node("recent");
        List<String> result = new LinkedList<>();
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            String path = p.get("file" + i, null);
            if (path != null) result.add(path);
        }
        return result;
    }

    public void clearRecentFiles() {
        Preferences p = root.node("recent");
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            p.remove("file" + i);
        }
    }

    // ---- Theme ----
    public void saveTheme(String themeId) {
        root.node("appearance").put("theme", themeId);
    }

    public String loadTheme() {
        return root.node("appearance").get("theme", "PRIMER_DARK");
    }

    // ---- Active tool ----
    public void saveActiveTool(String toolId) {
        root.node("tools").put("lastActiveTool", toolId);
    }

    public String loadActiveTool() {
        return root.node("tools").get("lastActiveTool", "SELECTION");
    }

    // ---- Panel visibility ----
    public void savePanelVisibility(boolean showTree, boolean showProperties) {
        Preferences p = root.node("panels");
        p.putBoolean("showTree", showTree);
        p.putBoolean("showProperties", showProperties);
    }

    public boolean[] loadPanelVisibility() {
        Preferences p = root.node("panels");
        return new boolean[]{
                p.getBoolean("showTree", true),
                p.getBoolean("showProperties", true)
        };
    }
}