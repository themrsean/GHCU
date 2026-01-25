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

public class GradingWindow extends Application {
    private final UIState uiState =
            new UIState(GradingWindow.class);

    static void main() {
        Application.launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("grading.fxml"));
        Parent root = loader.load();

        GradingController controller = loader.getController();
        controller.setStage(stage);

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Grade Report Folder");
        File folder = chooser.showDialog(stage);

        if (folder != null) {
            controller.loadReportFolder(folder.toPath());
        }
        Scene scene = new Scene(root);
        controller.installAccelerators(scene);
        stage.setScene(scene);
        stage.setTitle("Report Grading");
        stage.show();
        uiState.restoreWindow(stage, 900, 600);

        stage.setOnHidden(e ->
                uiState.saveWindow(stage)
        );

    }
}
