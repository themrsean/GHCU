/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package main.java.grading;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class GradingWindow extends Application {
    private final UIState uiState =
            new UIState(GradingWindow.class);

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/grading/grading.fxml"));
        Parent root;
        root = loader.load();

        GradingController controller = loader.getController();

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Grade Report Folder");
        File folder = chooser.showDialog(stage);

        if (folder != null) {
            controller.loadReportFolder(folder.toPath());
        }
        Scene scene = new Scene(root);
        var css = getClass().getResource("/grading/editor.css");
        System.out.println("CSS URL = " + css);
        scene.getStylesheets().add(Objects.requireNonNull(css).toExternalForm());
        controller.installAccelerators(scene);
        stage.setOnCloseRequest(_ -> controller.onClose());

        stage.setScene(scene);
        stage.setTitle("Report Grading");
        stage.show();
        final int width = 900;
        final int height = 600;
        uiState.restoreWindow(stage, width, height);

        stage.setOnHidden(_ ->
                uiState.saveWindow(stage)
        );

    }
}
