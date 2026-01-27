/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/23/2026
 */
package main.java;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import main.java.grading.GradingController;
import main.java.model.Assignment;
import main.java.ui.AssignmentCell;
import main.java.ui.FilesCell;
import main.java.model.Rubric;
import main.java.model.RubricItem;
import main.java.persistence.AssignmentStore;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.stream.Collectors;


/**
 * Controller for GHCU
 */
@SuppressWarnings("unused")
public class Controller implements Initializable {
    private static final Path APP_DATA_DIR =
            Paths.get(System.getProperty("user.home"), ".ghcu");
    private static final Path DATA_DIR = APP_DATA_DIR.resolve("data");
    private static final String DROP_STYLE =
            "-fx-background-color: derive(-fx-accent, 70%);" +
                    "-fx-border-color: -fx-accent; -fx-border-width: 2;";
    private static final Path ASSIGNMENTS_JSON =
            DATA_DIR.resolve("assignments.json");
    private final Path config =
            DATA_DIR.resolve("config.txt");
    private Path ignored =
            DATA_DIR.resolve("ignored.txt");
    private final List<String> ignoredFiles = new ArrayList<>();
    private final TextInputDialog input = new TextInputDialog();
    private final ObservableList<Assignment> assignments = FXCollections.observableArrayList();
    private final FileChooser chooser = new FileChooser();
    private Task<?> currentTask;

    @FXML
    private TextField repositoryField;
    @FXML
    private TextField pathField;
    @FXML
    private CheckBox checkStyleBox;
    @FXML
    private ListView<Assignment> assignmentListView;
    @FXML
    private ListView<String> filesListView;
    @FXML
    private TextArea feedback;
    @FXML
    private ProgressBar progressBar;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadResources();
        configureAssignmentListView();
        configureAssignmentSelection();
        configureAssignmentsModel();
        loadAssignmentsFromDisk();
        configureFilesListView();
        configureIgnoredFiles();
        configureFeedbackAutoScroll();
    }

    private void loadResources() {
        try {
            Files.createDirectories(DATA_DIR);
            copyIfMissing("data/assignments.json", ASSIGNMENTS_JSON);
            copyIfMissing("data/config.txt", config);
            copyIfMissing("data/ignored.txt", ignored);
        } catch(IOException e) {
            makeAlert("Initialization Error", "Cannot load config files",
                    "Cannot load needed files. Exiting");
            quit();
        }
    }

    private void copyIfMissing(String resource, Path target)
            throws IOException {
        if (!Files.exists(target)) {
            try (InputStream is = Controller.class
                    .getClassLoader()
                    .getResourceAsStream(resource)) {
                if (is == null) {
                    throw new IOException("Missing resource: " + resource);
                }
                Files.copy(is, target);
            }
        }
    }

    private void configureAssignmentListView() {
        assignmentListView.setCellFactory(_ -> new AssignmentCell());
        assignmentListView.setEditable(true);

        assignmentListView.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        SortedList<Assignment> sorted =
                new SortedList<>(assignments,
                        Comparator.comparing(Assignment::getShortName,
                                String.CASE_INSENSITIVE_ORDER));

        assignmentListView.setItems(sorted);
        assignmentListView.setTooltip(
                new Tooltip("Double-click to rename assignment")
        );
    }

    private void configureAssignmentSelection() {
        assignmentListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((_, _, assignment) -> {
                    if (assignment == null) {
                        filesListView.setItems(FXCollections.observableArrayList());
                    } else {
                        filesListView.setItems(
                                new SortedList<>(
                                        assignment.getFiles(),
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        );
                    }
                });
    }

    private void configureAssignmentsModel() {
        assignments.addListener(
                (javafx.collections.ListChangeListener<Assignment>) c -> {
                    while (c.next()) {
                        if (c.wasAdded() || c.wasRemoved()) {
                            saveAssignments();
                        }
                    }
                }
        );
    }

    private void loadAssignmentsFromDisk() {
        if (Files.exists(ASSIGNMENTS_JSON)) {
            try {
                List<Assignment> loaded = AssignmentStore.load(ASSIGNMENTS_JSON);
                assignments.setAll(loaded);
                loaded.forEach(a -> {
                    attachFileListener(a);
                    attachMetadataListener(a);
                    attachRubricListener(a);
                });
            } catch (IOException e) {
                makeAlert("Load Failed",
                        "Could not load assignments",
                        e.getMessage());
            }
        }
    }

    private void configureFilesListView() {
        filesListView.setEditable(true);
        filesListView.setPlaceholder(
                new Label("Drag files here to add to assignment")
        );
        filesListView.disableProperty().bind(
                assignmentListView
                        .getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );
        setFilesCells();


        filesListView.setTooltip(
                new Tooltip("Double-click to rename file")
        );

        configureFilesDragAndDrop();
    }

    private void setFilesCells() {
        filesListView.setCellFactory(list ->
                new FilesCell(name -> {
                    Assignment a = assignmentListView.getSelectionModel().getSelectedItem();
                    if (a == null) {
                        return false;
                    }
                    return a.getFiles().stream()
                            .filter(name::equals)
                            .count() > 1;
                })
        );
    }

    private void configureFilesDragAndDrop() {
        filesListView.setOnDragOver(e -> {
            if (e.getGestureSource() != filesListView
                    && e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        filesListView.setOnDragDropped(e -> {
            Assignment a =
                    assignmentListView.getSelectionModel().getSelectedItem();
            Dragboard db = e.getDragboard();
            boolean success = false;

            if (a != null && db.hasFiles()) {
                for (File f : db.getFiles()) {
                    String name = f.getName();
                    if (!a.getFiles().contains(name)) {
                        a.getFiles().add(name);
                    }
                }
                success = true;
            }

            e.setDropCompleted(success);
            e.consume();
        });
    }

    private void configureIgnoredFiles() {
        try (Scanner in = new Scanner(config)) {
            ignored = Paths.get(in.nextLine());
        } catch (IOException e) {
            makeAlert("File Not Found",
                    "Missing config",
                    "Cannot load the configuration file");
        }

        if (ignored.toFile().exists()) {
            try (Scanner in = new Scanner(ignored)) {
                while (in.hasNextLine()) {
                    ignoredFiles.add(in.nextLine());
                }
            } catch (IOException e) {
                makeAlert("File Not Found",
                        "Missing ignored files",
                        "Cannot load the ignored file list");
            }
        }
    }

    private void configureFeedbackAutoScroll() {
        feedback.textProperty().addListener(
                (_, _, _) ->
                        feedback.setScrollTop(Double.MAX_VALUE)
        );
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
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Utilities.pullRepositories(
                        repositoryField.getText(),
                        Paths.get(pathField.getText())
                );
                return null;
            }
        };

        runTask(task,
                "Pulling down student repositories...",
                "Pulled down all student repositories.");
    }

    @FXML
    private void extractPackages() {
        if (!pathField.getText().isEmpty()) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws IOException {
                    Utilities.extractPackages(
                            Paths.get(pathField.getText()),
                            ignoredFiles
                    );
                    return null;
                }
            };
            runTask(
                    task,
                    "Extracting packages from repositories...",
                    "Packages extracted."
            );
        }
    }

    @FXML
    private void extractImports() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                Utilities.generateImports(
                        Paths.get(pathField.getText(), "submissions")
                );
                return null;
            }
        };

        runTask(
                task,
                "Extracting imports...",
                "Imports extracted."
        );
    }

    @FXML
    private void generateReports() {
        Assignment assignment =
                assignmentListView.getSelectionModel().getSelectedItem();
        if (assignment != null) {
            makeAlert("No Assignment Selected",
                    "Select an assignment",
                    "Reports require an assignment");
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    Utilities.generateReports(
                            Paths.get(pathField.getText(), "submissions"),
                            assignment,
                            checkStyleBox.isSelected()
                    );
                    return null;
                }
            };
            runTask(task,
                    "Generating student feedback reports...",
                    "Feedback reports generated.");
        }
    }

    @FXML
    private void runAll() {
        final double repositoryCompletion = 0.3;
        final double packageCompletion = 0.6;
        final double importCompletion = 0.8;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Pulling repositories...");
                updateProgress(0.0, 1.0);
                Utilities.pullRepositories(
                        repositoryField.getText(),
                        Paths.get(pathField.getText())
                );
                if (!isCancelled()) {
                    updateMessage("Extracting packages...");
                    updateProgress(repositoryCompletion, 1);
                    Utilities.extractPackages(
                            Paths.get(pathField.getText()),
                            ignoredFiles
                    );
                    if (!isCancelled()) {
                        updateMessage("Generating imports...");
                        updateProgress(packageCompletion, 1);
                        Utilities.generateImports(
                                Paths.get(pathField.getText(), "submissions")
                        );
                        Assignment assignment =
                                assignmentListView.getSelectionModel().getSelectedItem();
                        if (assignment != null) {
                            updateMessage("Generating reports...");
                            updateProgress(importCompletion, 1);
                            Utilities.generateReports(
                                    Paths.get(pathField.getText(), "submissions"),
                                    assignment,
                                    checkStyleBox.isSelected()
                            );
                        }
                    }
                    updateProgress(1.0, 1.0);
                    updateMessage("Run All complete.");
                }
                return null;
            }
        };

        runTask(task,
                "Starting full pipeline...",
                "Run All complete.");
    }

    @FXML
    private void help() {
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("data/README.md")) {
            if (is == null) {
                throw new IOException("README.md not found in resources.");
            }
            String markdown = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            String html = convertMarkdownToHtml(markdown);
            WebView view = new WebView();
            view.getEngine().loadContent(html);
            Stage helpWindow = new Stage();
            helpWindow.setScene(new Scene(new StackPane(view)));
            helpWindow.show();
        } catch (IOException e) {
            String[] messages = {
                    "File Not Found",
                    "Missing manual",
                    "Cannot load the manual file"
            };
            makeAlert(messages);
        }
    }

    @FXML
    private void addAssignment() {
        Assignment assignment = showAssignmentDialog(null);
        if (assignment != null) {
            attachFileListener(assignment);
            attachMetadataListener(assignment);
            attachRubricListener(assignment);
            assignments.add(assignment);
        }
    }

    @FXML
    private void removeAssignment() {
        assignments.remove(assignmentListView.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void addFilesToAssignment() {
        Assignment assignment =
                assignmentListView.getSelectionModel().getSelectedItem();
        if (assignment != null) {
            makeAlert("No Assignment Selected",
                    "Select an assignment",
                    "Files must belong to an assignment");
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add File");
            dialog.setHeaderText("Enter file name");
            dialog.showAndWait()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .ifPresent(assignment.getFiles()::add);
        }
    }

    @FXML
    private void removeFileFromAssignment() {
        Assignment assignment =
                assignmentListView.getSelectionModel().getSelectedItem();
        String file =
                filesListView.getSelectionModel().getSelectedItem();
        if (assignment != null && file != null) {
            assignment.getFiles().remove(file);
        }
    }

    @FXML
    private void editAssignment() {
        Assignment assignment =
                assignmentListView.getSelectionModel().getSelectedItem();
        if (assignment == null) {
            makeAlert("No Assignment Selected",
                    "Select an assignment",
                    "Nothing to edit");
        } else {
            showAssignmentDialog(assignment);
        }
    }

    @FXML
    private void saveAssignments() {
        try {
            AssignmentStore.save(
                    ASSIGNMENTS_JSON,
                    assignments
            );
        } catch (IOException e) {
            makeAlert("Save Failed",
                    "Could not save assignments",
                    e.getMessage());
        }
    }

    @FXML
    private void editRubric() {
        Assignment assignment =
                assignmentListView.getSelectionModel().getSelectedItem();
        if (assignment == null) {
            makeAlert("No Assignment Selected",
                    "Select an assignment",
                    "Nothing to edit");
        } else if (!assignment.getRubric().isValid()) {
            makeAlert("Invalid Rubric",
                    "Total must equal 100 points",
                    "Current total: " +
                            assignment.getRubric().getTotalPoints());
        } else {
            VBox rubricEditor = buildRubricEditor(assignment.getRubric());
            Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
            dialog.setTitle("Edit Rubric");
            dialog.setHeaderText(
                    "Total Points: " + assignment.getRubric().getTotalPoints());
            dialog.getDialogPane().setContent(rubricEditor);
            dialog.showAndWait();
        }
    }

    @FXML
    private void openGradingWindow() {
        Assignment assignment =
                assignmentListView.getSelectionModel().getSelectedItem();
        if (assignment == null) {
            makeAlert(
                    "No Assignment Selected",
                    "Select an assignment",
                    "Grading requires an assignment"
            );
        } else {
            Path reportsDir = Paths.get(
                    pathField.getText(),
                    "submissions",
                    "feedback"
            );
            if (!Files.exists(reportsDir)) {
                makeAlert(
                        "No Reports Found",
                        "Generate reports first",
                        "No grading files exist for this assignment"
                );
            } else {
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/grading/grading.fxml")
                    );
                    Scene scene = new Scene(loader.load());
                    GradingController gradingController = loader.getController();
                    Stage stage = new Stage();
                    stage.setTitle("Grading – " + assignment.getShortName());
                    stage.setScene(scene);
                    var css = getClass().getResource("/grading/editor.css");
                    System.out.println("CSS URL = " + css);
                    scene.getStylesheets().add(Objects.requireNonNull(css).toExternalForm());
                    stage.initOwner(pathField.getScene().getWindow());
                    gradingController.loadReportFolder(reportsDir);
                    gradingController.installAccelerators(scene);
                    stage.setOnCloseRequest(e -> gradingController.onClose());
                    stage.show();
                } catch (IOException e) {
                    makeAlert(
                            "Failed to Open Grading Window",
                            "FXML load error",
                            e.getMessage()
                    );
                }
            }
        }
    }

    private @NonNull VBox getVBox(Assignment assignment, TableView<RubricItem> table) {
        Button add = new Button("Add");
        add.setOnAction(_ ->
                assignment.getRubric().getItems()
                        .add(new RubricItem("New Item", 0)));
        Button remove = new Button("Remove");
        remove.setOnAction(_ -> {
            RubricItem item = table.getSelectionModel().getSelectedItem();
            if (item != null) {
                assignment.getRubric().getItems().remove(item);
            }
        });
        final int spacing = 10;
        return new VBox(spacing, table, new HBox(spacing, add, remove));
    }

    @FXML
    private void cancelCurrentTask() {
        if (currentTask != null) {
            currentTask.cancel();
        }
    }

    @FXML
    private void loadConfig() {
        File file = chooser.showOpenDialog(pathField.getScene().getWindow());
        if (file != null) {
            try (Scanner in = new Scanner(file)) {
                ignored = Paths.get(in.nextLine());
            } catch (FileNotFoundException e) {
                String[] messages = {"File Not Found", "Missing config",
                        "Cannot load the configuration file"};
                makeAlert(messages);
            }
        }
    }

    private void attachFileListener(Assignment assignment) {
        assignment.getFiles().addListener(
                (javafx.collections.ListChangeListener<String>) change -> {
                    while (change.next()) {
                        if (change.wasAdded() || change.wasRemoved()) {
                            saveAssignments();
                        }
                    }
                }
        );
    }

    private void attachMetadataListener(Assignment assignment) {
        assignment.shortNameProperty().addListener((_, _, _) -> {
            assignmentListView.refresh();
            saveAssignments();
        });
        assignment.fullNameProperty().addListener((_, _, _) -> {
            assignmentListView.refresh();
            saveAssignments();
        });
    }

    private void attachRubricListener(Assignment assignment) {
        assignment.getRubric().getItems().addListener(
                (javafx.collections.ListChangeListener<RubricItem>) change -> {
                    while (change.next()) {
                        saveAssignments();
                    }
                }
        );
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

    private Assignment showAssignmentDialog(Assignment existing) {
        final double width = 400.0;
        final double scaling = 0.9;
        Dialog<Assignment> dialog = new Dialog<>();
        dialog.setWidth(width);
        dialog.getDialogPane().setMaxHeight(Screen.getPrimary()
                .getVisualBounds().getHeight() * scaling);
        dialog.setResizable(true);
        dialog.setTitle(existing == null ? "New Assignment" : "Edit Assignment");
        ButtonType createButton =
                new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);
        Button createBtn = (Button) dialog.getDialogPane().lookupButton(createButton);
        TextField shortNameField = new TextField();
        TextField fullNameField = new TextField();
        ObservableList<String> files = FXCollections.observableArrayList();
        ListView<String> fileList = getListView(files);
        Rubric rubric = existing != null
                ? existing.getRubric()
                : new Rubric();

        VBox rubricBox = buildRubricEditor(rubric);

        Button addFile = new Button("Add File");
        addFile.setOnAction(_ -> {
            TextInputDialog d = new TextInputDialog();
            d.setTitle("Add File");
            d.setHeaderText("Enter file name");
            d.showAndWait()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .ifPresent(files::add);
        });
        Button removeFile = new Button("Remove File");
        removeFile.setOnAction(_ -> {
            String selected = fileList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                files.remove(selected);
            }
        });

        if (existing != null) {
            shortNameField.setText(existing.getShortName());
            fullNameField.setText(existing.getFullName());
            files.setAll(existing.getFiles());
        }
        final int pointsPerAssignment = 100;
        BooleanBinding rubricInvalid = rubric.totalPointsProperty()
                .isNotEqualTo(pointsPerAssignment);
        createBtn.disableProperty().bind(
                Bindings.or(
                        Bindings.or(
                                Bindings.createBooleanBinding(
                                        () -> hasDuplicates(files),
                                        files
                                ),
                                shortNameField.textProperty().isEmpty()
                        ),
                        Bindings.or(
                                fullNameField.textProperty().isEmpty(),
                                rubricInvalid
                        )
                )
        );
        GridPane grid = new GridPane();
        final int spacing = 10;
        grid.setHgap(spacing);
        grid.setVgap(spacing);
        grid.addRow(0, new Label("Short Name:"), shortNameField);
        grid.addRow(1, new Label("Full Name:"), fullNameField);
        grid.addRow(2, new Label("Files:"), fileList);
        grid.addRow(3, addFile, removeFile);
        grid.add(rubricBox, 0, 4, 2, 1);
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        dialog.getDialogPane().setContent(scroll);
        dialog.setResultConverter(button -> {
            if (button == createButton) {
                Assignment target = existing != null ? existing :
                        new Assignment(shortNameField.getText().trim(),
                                fullNameField.getText().trim());
                target.setShortName(shortNameField.getText().trim());
                target.setFullName(fullNameField.getText().trim());
                target.getFiles().setAll(files);
                if (existing == null) {
                    target.getRubric().getItems().setAll(rubric.getItems());
                }
                return target;
            }
            return null;
        });
        return dialog.showAndWait().orElse(null);
    }

    private @NonNull TableColumn<RubricItem, Integer> getRubricItemIntegerTableColumn() {
        TableColumn<RubricItem, Integer> ptsCol =
                new TableColumn<>("Points");

        ptsCol.setCellValueFactory(c ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        c.getValue().getPoints()
                ));
        ptsCol.setCellFactory(
                TextFieldTableCell.forTableColumn(
                        new javafx.util.converter.IntegerStringConverter()
                )
        );
        ptsCol.setOnEditCommit(e ->
                e.getRowValue().setPoints(e.getNewValue())
        );
        return ptsCol;
    }

    @SuppressWarnings("unchecked")
    private VBox buildRubricEditor(Rubric rubric) {
        TableView<RubricItem> table = new TableView<>(rubric.getItems());
        table.setEditable(true);
        TableColumn<RubricItem, String> descCol =
                new TableColumn<>("Item");
        descCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDescription()));
        descCol.setCellFactory(TextFieldTableCell.forTableColumn());
        descCol.setOnEditCommit(e ->
                e.getRowValue().setDescription(e.getNewValue()));
        TableColumn<RubricItem, Integer> ptsCol =
                getRubricItemIntegerTableColumn();
        table.getColumns().addAll(descCol, ptsCol);

        Button addItem = new Button("Add Item");
        addItem.setOnAction(_ ->
                rubric.getItems().add(new RubricItem("New Item", 0)));

        Button removeItem = new Button("Remove Item");
        removeItem.setOnAction(_ -> {
            RubricItem selected =
                    table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                rubric.getItems().remove(selected);
            }
        });

        Label totalLabel = new Label();
        totalLabel.textProperty().bind(
                rubric.totalPointsProperty().asString("Total Points: %d")
        );
        final int boxSpacing = 5;
        return new VBox(
                boxSpacing,
                new Label("Rubric"),
                table,
                new HBox(boxSpacing, addItem, removeItem),
                totalLabel
        );
    }

    private @NonNull ListView<String> getListView(ObservableList<String> files) {
        ListView<String> fileList = new ListView<>(files);
        fileList.setEditable(true);
        setFilesCells();
        configureFileDragAndDrop(fileList, files);
        fileList.setPlaceholder(
                new Label("Drag files here or click Add File")
        );
        return fileList;
    }

    private void configureFileDragAndDrop(
            ListView<String> fileList,
            ObservableList<String> files
    ) {
        fileList.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                fileList.setStyle(DROP_STYLE);
            }
            event.consume();
        });
        fileList.setOnDragExited(event -> {
            fileList.setStyle("");
            event.consume();
        });
        fileList.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    String name = file.getName();
                    if (!files.contains(name)) {
                        files.add(name);
                    }
                }
                success = true;
            }
            fileList.setStyle("");
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean hasDuplicates(ObservableList<String> items) {
        return items.size() != items.stream().distinct().count();
    }

    private void runTask(Task<?> task, String startMsg, String successMsg) {
        // Cancel any running task
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
        currentTask = task;
        // Bind progress
        progressBar.progressProperty().unbind();
        progressBar.visibleProperty().unbind();
        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.visibleProperty().bind(task.runningProperty());
        progressBar.managedProperty().bind(task.runningProperty());
        task.messageProperty().addListener((obs, old, msg) -> {
            if (msg != null) {
                feedback.appendText(msg + "\n");
            }
        });
        task.setOnRunning(e ->
                feedback.appendText(startMsg + "\n")
        );
        task.setOnSucceeded(e ->
                feedback.appendText(successMsg + "\n")
        );
        task.setOnSucceeded(e -> feedback.textProperty().unbind());
        task.setOnCancelled(e -> feedback.textProperty().unbind());
        task.setOnFailed(e -> feedback.textProperty().unbind());
        task.setOnCancelled(e ->
                feedback.appendText("Operation cancelled.\n")
        );
        task.setOnFailed(e ->
                makeAlert(
                        "Task Failed",
                        "Execution Error",
                        task.getException().getMessage()
                )
        );
        Thread t = new Thread(task, "GHCU-Task");
        t.setDaemon(true);
        t.start();
    }
}