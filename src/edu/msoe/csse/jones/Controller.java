/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 */
package edu.msoe.csse.jones;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;


/**
 * Controller for GHCU
 */
public class Controller implements Initializable {
    private final Thread pullRepositoriesThread = new Thread(this::pullRepositories);
    private final Thread extractPackagesThread = new Thread(this::extractPackages);
    private final Thread extractImportsThread = new Thread(this::extractImports);
    private final Thread generateReportsThread = new Thread(this::generateReports);
    private final List<String> ignoredFiles = new ArrayList<>();
    private final TextInputDialog input = new TextInputDialog();
    private final Path config = Paths.get("data", "config.txt");
    private Path ignored = Paths.get("data", "ignored.txt");
    private Path header = Paths.get("data", "defaultHeader.txt");
    private final FileChooser chooser = new FileChooser();
    @FXML
    private TextField repositoryField;
    @FXML
    private TextField pathField;
    @FXML
    private CheckBox checkStyleBox;
    @FXML
    private TextField shortNameField;
    @FXML
    private TextField fullNameField;
    @FXML
    private ListView<String> listView;
    @FXML
    private TextArea feedback;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try (Scanner in = new Scanner(config)) {
            ignored = Paths.get(in.nextLine());
            header = Paths.get(in.nextLine());
        } catch (IOException e) {
            String[] messages = {"File Not Found", "Missing config",
                    "Cannot load the configuration file"};
            makeAlert(messages);
        }
        if (ignored.toFile().exists()) {
            try (Scanner in = new Scanner(ignored)) {
                while (in.hasNextLine()) {
                    ignoredFiles.add(in.nextLine());
                }
            } catch (IOException e) {
                String[] messages = {"File Not Found", "Missing ignored files",
                        "Cannot load the ignored file list"};
                makeAlert(messages);
            }
        }
        feedback.textProperty().addListener(
                (observableValue, s, t1) -> feedback.setScrollTop(Double.MAX_VALUE));
    }

    @FXML
    private void open() {
        DirectoryChooser chooser = new DirectoryChooser();
        File file = chooser.showDialog(pathField.getScene().getWindow());
        if (file != null) {
            pathField.setText(file.getAbsolutePath());
            feedback.appendText("Working directory set.\n");
        }
    }

    @FXML
    private void quit() {
        Platform.exit();
    }

    @FXML
    private void addToIgnored() {
        input.setTitle("Add File to Ignored List");
        input.setHeaderText("Enter the file to add to the Ignored List");
        StringBuilder sb = new StringBuilder();
        for (String s : ignoredFiles) {
            sb.append(s).append("\n");
        }
        input.setContentText(sb.toString());
        Optional<String> filename = input.showAndWait();
        if (filename.isPresent()) {
            try (PrintWriter pw = new PrintWriter(new FileOutputStream(ignored.toFile(), true))) {
                pw.append(filename.get()).append("\n");
                ignoredFiles.add(filename.get());
                feedback.appendText(filename.get() + " added to ignored list.\n");
            } catch (IOException e) {
                String[] messages = {"Could not write",
                        "Ignored list not updated", "Cannot save changes to the ignored list"};
                makeAlert(messages);
            }
        }
    }

    @FXML
    private void pullRepositories() {
        feedback.appendText("Pulling down student repositories...\n");
        try {
            if (!repositoryField.getText().isEmpty() && !pathField.getText().isEmpty()) {
                Utilities.pullRepositories(repositoryField.getText(),
                        Paths.get(pathField.getText()));
            }
            feedback.appendText("Pulled down all student repositories.\n");
        } catch (IOException | InterruptedException e) {
            String[] messages = {"Could not pull", "Repositories not pulled",
                    "Cannot pull student repositories"};
            System.out.println(e.getMessage());
            makeAlert(messages);
        }
    }

    @FXML
    private void extractPackages() {
        if (!pathField.getText().isEmpty()) {
            feedback.appendText("Extracting packages from repositories...\n");
            Utilities.extractPackages(Paths.get(pathField.getText()), ignoredFiles);
            feedback.appendText("Packages extracted.\n");
        }
    }

    @FXML
    private void extractImports() {
        feedback.appendText("Extracting imports...\n");
        Utilities.generateImports(Paths.get(pathField.getText(), "submissions"));
        feedback.appendText("Imports extracted.\n");
    }

    @FXML
    private void generateReports() {
        feedback.appendText("Generating student feedback reports...\n");
        Utilities.generateReports(Paths.get(pathField.getText(), "submissions"),
                listView.getItems(),
                shortNameField.getText(),
                fullNameField.getText(),
                header,
                checkStyleBox.isSelected());
        feedback.appendText("Feedback reports generated.\n");
    }

    @FXML
    private void runAll() {
        try {
            pullRepositoriesThread.start();
            pullRepositoriesThread.join();
            extractPackagesThread.start();
            extractPackagesThread.join();
            extractImportsThread.start();
            extractImportsThread.join();
            generateReportsThread.start();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    @FXML
    private void help() {
        try {
            StringBuilder sb = new StringBuilder();
            List<String> list = Files.readAllLines(Paths.get("data", "README.md"));
            for (String s : list) {
                sb.append(s).append("\n");
            }
            String html = convertMarkdownToHtml(sb.toString());
            Stage helpWindow = new Stage();
            Pane pane = new Pane();
            WebView view = new WebView();
            view.getEngine().loadContent(html);
            pane.getChildren().add(view);
            helpWindow.setScene(new Scene(pane));
            helpWindow.show();
        } catch (IOException e) {
            String[] messages = {"File Not Found", "Missing manual", "Cannot load the manual file"};
            makeAlert(messages);
        }
    }

    @FXML
    private void addFiles() {
        input.setTitle("Add files to report");
        input.setHeaderText("Enter files for the repo as a comma-separated list");
        Optional<String> files = input.showAndWait();
        if (files.isPresent()) {
            String[] split = files.get().trim().split(",");
            listView.setItems(FXCollections.observableArrayList(Arrays.stream(split)
                    .map(String::trim)
                    .toList()));
        }
    }

    @FXML
    private void removeFiles() {
        listView.getItems().remove(listView.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void loadConfig() {
        File file = chooser.showOpenDialog(pathField.getScene().getWindow());
        if (file != null) {
            try (Scanner in = new Scanner(file)) {
                ignored = Paths.get(in.nextLine());
                header = Paths.get(in.nextLine());
            } catch (FileNotFoundException e) {
                String[] messages = {"File Not Found", "Missing config",
                        "Cannot load the configuration file"};
                makeAlert(messages);
            }
        }
    }

    private void makeAlert(String... messages) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        int length = messages.length;
        switch (length) {
            case 1 -> alert.setContentText(messages[0]);
            case 2 -> {
                alert.setHeaderText(messages[0]);
                alert.setContentText(messages[1]);
            }
            case 3 -> {
                alert.setTitle(messages[0]);
                alert.setHeaderText(messages[1]);
                alert.setContentText(messages[2]);
            }
        }
        alert.show();
    }

    private String convertMarkdownToHtml(String markdownText) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdownText);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }
}
