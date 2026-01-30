/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package grading.model;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Persists and restores UI-related state for the grading application using
 * {@link java.util.prefs.Preferences}.
 * <p>
 * This class centralizes storage of window geometry, sidebar sizing, font size,
 * and per-report scroll position so that the grading UI can restore the user's
 * previous layout across application runs.
 * <p>
 * Values are stored under the user preference node associated with the owning
 * class' package (see {@link Preferences#userNodeForPackage(Class)}). Window
 * dimensions are clamped to remain within a usable range based on the primary
 * display's visual bounds.
 * <p>
 * This class is intended to be lightweight and used as a simple helper from
 * JavaFX controllers; it does not manage any UI controls directly.
 *
 * @author Sean Jones
 */
public final class UIState {
    private static final double MIN_WINDOW_WIDTH = 600.0;
    private static final double MIN_WINDOW_HEIGHT = 400.0;

    private final Preferences prefs;

    /**
     * Creates a new UIState manager backed by {@link Preferences}.
     * <p>
     * Preferences are stored in the user node associated with the given owner class'
     * package, allowing UI state to persist across application runs.
     *
     * @param owner class used to determine the {@link Preferences} node
     * @throws NullPointerException if {@code owner} is {@code null}
     */
    public UIState(Class<?> owner) {
        Objects.requireNonNull(owner, "owner");
        this.prefs = Preferences.userNodeForPackage(owner);
    }

    /* ---------------- Window ---------------- */
    /**
     * Restores window size and position from persisted preferences.
     * <p>
     * Width and height are clamped to ensure they remain within a usable range:
     * at least {@link #MIN_WINDOW_WIDTH}/{@link #MIN_WINDOW_HEIGHT} and at most the
     * primary screen's visual bounds.
     * <p>
     * X/Y are only restored if the stored values are not {@code NaN}.
     *
     * @param stage the window to restore
     * @param defW default width to use if no preference exists
     * @param defH default height to use if no preference exists
     * @throws NullPointerException if {@code stage} is {@code null}
     */
    public void restoreWindow(Stage stage, double defW, double defH) {
        Objects.requireNonNull(stage, "stage");

        double maxW = maxWindowWidth();
        double maxH = maxWindowHeight();

        double width = prefs.getDouble(Keys.WINDOW_WIDTH.toString(), defW);
        double height = prefs.getDouble(Keys.WINDOW_HEIGHT.toString(), defH);

        width = clamp(width, MIN_WINDOW_WIDTH, maxW);
        height = clamp(height, MIN_WINDOW_HEIGHT, maxH);

        stage.setWidth(width);
        stage.setHeight(height);

        double x = prefs.getDouble(Keys.WINDOW_X.toString(), Double.NaN);
        if (!Double.isNaN(x)) {
            stage.setX(x);
        }

        double y = prefs.getDouble(Keys.WINDOW_Y.toString(), Double.NaN);
        if (!Double.isNaN(y)) {
            stage.setY(y);
        }
    }

    private static double maxWindowWidth() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        return bounds.getWidth();
    }

    private static double maxWindowHeight() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        return bounds.getHeight();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Saves the current window size and position into persisted preferences.
     * <p>
     * Width and height are clamped to ensure persisted values remain within a usable
     * range: at least {@link #MIN_WINDOW_WIDTH}/{@link #MIN_WINDOW_HEIGHT} and at most
     * the primary screen's visual bounds.
     * <p>
     * X/Y are only saved if they are not {@code NaN}.
     *
     * @param stage the window to save
     * @throws NullPointerException if {@code stage} is {@code null}
     */
    public void saveWindow(Stage stage) {
        Objects.requireNonNull(stage, "stage");
        double maxW = maxWindowWidth();
        double maxH = maxWindowHeight();
        double width = clamp(stage.getWidth(), MIN_WINDOW_WIDTH, maxW);
        double height = clamp(stage.getHeight(), MIN_WINDOW_HEIGHT, maxH);
        prefs.putDouble(Keys.WINDOW_WIDTH.toString(), width);
        prefs.putDouble(Keys.WINDOW_HEIGHT.toString(), height);
        double x = stage.getX();
        double y = stage.getY();
        if (!Double.isNaN(x)) {
            prefs.putDouble(Keys.WINDOW_X.toString(), x);
        }
        if (!Double.isNaN(y)) {
            prefs.putDouble(Keys.WINDOW_Y.toString(), y);
        }
    }

    /* ---------------- Sidebar ---------------- */
    /**
     * Loads the persisted sidebar width.
     *
     * @param def default width to use if no preference exists
     * @return the stored sidebar width, or {@code def} if not present
     */
    public double loadSidebarWidth(double def) {
        return prefs.getDouble(Keys.SIDEBAR_WIDTH.toString(), def);
    }

    /**
     * Saves the sidebar width.
     *
     * @param width the sidebar width to persist
     */
    public void saveSidebarWidth(double width) {
        prefs.putDouble(Keys.SIDEBAR_WIDTH.toString(), width);
    }

    /* ---------------- Font ---------------- */
    /**
     * Loads the persisted editor font size.
     *
     * @param def default font size to use if no preference exists
     * @return the stored font size, or {@code def} if not present
     */
    public double loadFontSize(double def) {
        return prefs.getDouble(Keys.FONT_SIZE.toString(), def);
    }

    /**
     * Saves the editor font size.
     *
     * @param size the font size to persist
     */
    public void saveFontSize(double size) {
        prefs.putDouble(Keys.FONT_SIZE.toString(), size);
    }

    /* ---------------- Reports ---------------- */
    /**
     * Loads the last opened report path, if available.
     *
     * @return the last opened report path, or {@code null} if none is stored
     */
    public Path loadLastReport() {
        String v = prefs.get(Keys.LAST_REPORT.toString(), null);
        return v == null ? null : Path.of(v);
    }

    /**
     * Saves the last opened report path.
     * <p>
     * If {@code report} is {@code null}, the stored preference is removed.
     *
     * @param report the report path to store, or {@code null} to clear the preference
     */
    public void saveLastReport(Path report) {
        if (report == null) {
            prefs.remove(Keys.LAST_REPORT.toString());
        } else {
            prefs.put(Keys.LAST_REPORT.toString(), report.toString());
        }
    }

    /**
     * Loads the persisted vertical scroll position for the given report.
     *
     * @param report report file whose scroll position should be loaded
     * @return stored scroll position for the report, or {@code 0.0} if none exists
     * @throws NullPointerException if {@code report} is {@code null}
     */
    public double loadScroll(Path report) {
        Objects.requireNonNull(report, "report");
        return prefs.getDouble(scrollKey(report), 0.0);
    }

    /**
     * Saves the vertical scroll position for the given report.
     *
     * @param report report file whose scroll position should be stored
     * @param value scroll position value to persist
     * @throws NullPointerException if {@code report} is {@code null}
     */
    public void saveScroll(Path report, double value) {
        Objects.requireNonNull(report, "report");
        prefs.putDouble(scrollKey(report), value);
    }

    private String scrollKey(Path report) {
        Objects.requireNonNull(report, "report");
        return Keys.SCROLL + keyFor(report);
    }

    private String keyFor(Path path) {
        Objects.requireNonNull(path, "path");
        return Integer.toHexString(path.toAbsolutePath().toString().hashCode());
    }

    /* Valid keys */
    private enum Keys {
        WINDOW_WIDTH("window.width"),
        WINDOW_HEIGHT("window.height"),
        WINDOW_X("window.x"),
        WINDOW_Y("window.y"),
        SIDEBAR_WIDTH("sidebar.width"),
        FONT_SIZE("font.size"),
        SCROLL("scroll."),
        LAST_REPORT("last.report");

        private final String key;
        Keys(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }
}
