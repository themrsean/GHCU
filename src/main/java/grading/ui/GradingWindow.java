/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package grading.ui;

import grading.controller.GradingController;
import grading.model.UIState;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * JavaFX entry point for launching the grading window.
 * <p>
 * This application loads the grading UI from FXML, prompts the user to select a
 * folder containing generated HTML feedback reports, installs editor styling and
 * keyboard accelerators, and restores/persists window geometry using {@link UIState}.
 */
public class GradingWindow extends Application {

    /* Defaults */
    private static final int DEFAULT_WINDOW_WIDTH = 900;
    private static final int DEFAULT_WINDOW_HEIGHT = 600;

    private final UIState uiState = new UIState(GradingWindow.class);

    /**
     * Launches the grading window.
     * <p>
     * This method loads the grading FXML, wires controller behavior, optionally
     * loads a report folder selected by the user, installs CSS styling, restores
     * window state, and registers shutdown persistence hooks.
     *
     * @param stage primary stage provided by the JavaFX runtime
     * @throws IOException if the grading FXML cannot be loaded
     * @throws NullPointerException if {@code stage} is {@code null}
     */
    @Override
    public void start(Stage stage) throws IOException {
        Objects.requireNonNull(stage, "stage");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/grading/grading.fxml"));
        Parent root = loader.load();
        GradingController controller = loader.getController();
        Scene scene = new Scene(root);
        installStylesheet(scene);
        controller.installAccelerators(scene);
        stage.setOnCloseRequest(_ -> controller.onClose());
        stage.setScene(scene);
        stage.setTitle("Report Grading");
        uiState.restoreWindow(stage, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
        stage.setOnShown(_ -> promptForReportFolder(stage, controller));
        stage.setOnHidden(_ -> uiState.saveWindow(stage));
        stage.show();
    }

    /**
     * Installs a stylesheet into the given {@link Scene}.
     *
     * @param scene scene to apply stylesheet to
     * @throws NullPointerException  if {@code scene} or {@code resourcePath} is {@code null}
     * @throws IllegalStateException if the stylesheet resource cannot be found
     */
    private void installStylesheet(Scene scene) {
        Objects.requireNonNull(scene, "scene");
        URL cssUrl = getClass().getResource("/grading/editor.css");
        if (cssUrl == null) {
            throw new IllegalStateException("Missing stylesheet: " + "/grading/editor.css");
        }
        scene.getStylesheets().add(cssUrl.toExternalForm());
    }

    /**
     * Prompts the user to choose a folder containing feedback reports and loads it.
     * <p>
     * If the user cancels the dialog, no folder is loaded.
     *
     * @param stage owner stage for the dialog
     * @param controller grading controller to receive the folder
     */
    private void promptForReportFolder(Stage stage, GradingController controller) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(controller, "controller");
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Grade Report Folder");
        File folder = chooser.showDialog(stage);
        if (folder != null) {
            try {
                controller.loadReportFolder(folder.toPath());
            } catch (IOException e) {
                // This is a startup convenience action; fail softly.
                System.err.println("Failed to load report folder: " + folder);
                System.err.println("Reason: " + e.getMessage());
            }
        }
    }
}