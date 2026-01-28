/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package grading.model;

import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.prefs.Preferences;

public final class UIState {

    private final Preferences prefs;

    public UIState(Class<?> owner) {
        this.prefs = Preferences.userNodeForPackage(owner);
    }

    /* ---------------- Window ---------------- */

    public void restoreWindow(Stage stage, double defW, double defH) {
        stage.setWidth(prefs.getDouble("window.width", defW));
        stage.setHeight(prefs.getDouble("window.height", defH));
        stage.setX(prefs.getDouble("window.x", Double.NaN));
        stage.setY(prefs.getDouble("window.y", Double.NaN));
    }

    public void saveWindow(Stage stage) {
        prefs.putDouble("window.width", stage.getWidth());
        prefs.putDouble("window.height", stage.getHeight());
        prefs.putDouble("window.x", stage.getX());
        prefs.putDouble("window.y", stage.getY());
    }

    /* ---------------- Sidebar ---------------- */

    public double loadSidebarWidth(double def) {
        return prefs.getDouble("sidebar.width", def);
    }

    public void saveSidebarWidth(double width) {
        prefs.putDouble("sidebar.width", width);
    }

    /* ---------------- Font ---------------- */

    public double loadFontSize(double def) {
        return prefs.getDouble("font.size", def);
    }

    public void saveFontSize(double size) {
        prefs.putDouble("font.size", size);
    }

    /* ---------------- Reports ---------------- */

    public Path loadLastReport() {
        String v = prefs.get("last.report", null);
        return v == null ? null : Path.of(v);
    }

    public void saveLastReport(Path report) {
        prefs.put("last.report", report.toString());
    }

    public double loadScroll(Path report) {
        return prefs.getDouble(scrollKey(report), 0.0);
    }

    public void saveScroll(Path report, double value) {
        prefs.putDouble(scrollKey(report), value);
    }

    private String scrollKey(Path report) {
        return "scroll." + keyFor(report);
    }

    private String keyFor(Path path) {
        return Integer.toHexString(path.toAbsolutePath().toString().hashCode());
    }

}
