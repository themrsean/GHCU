/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 */
package edu.msoe.csse.jones;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * GUI frontend for GHCU
 */
public class GitHubClassroomUtils extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage)throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("fxml.fxml"));
        Parent root = loader.load();
        stage.setTitle("GitHub Classroom Utilities");
        stage.setScene(new Scene(root));
        stage.show();
    }
}
