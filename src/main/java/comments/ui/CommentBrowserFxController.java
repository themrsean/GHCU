/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package comments.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import comments.domain.CommentTemplate;

import java.io.File;
import java.util.List;

public final class CommentBrowserFxController {

    /* ---------- UI Controls ---------- */

    @FXML private TextField searchField;
    @FXML private TextField assignmentField;
    @FXML private TextField rubricField;
    @FXML private TextField tagField;

    @FXML private ListView<CommentTemplate> commentList;

    @FXML private Button insertButton;
    @FXML private Button deleteButton;
    @FXML private Button importButton;
    @FXML private Button exportButton;

    /* ---------- Logic Controller ---------- */

    private CommentBrowserController controller;

    /* ------------------------------------------------------------
       Initialization
       ------------------------------------------------------------ */

    @FXML
    private void initialize() {
        commentList.setCellFactory(list ->
                new ListCell<>() {
                    @Override
                    protected void updateItem(CommentTemplate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getTitle());
                        }
                    }
                }
        );

        searchField.textProperty().addListener((obs, o, n) -> {
            controller.setSearchQuery(n);
            refresh();
        });

        assignmentField.textProperty().addListener((obs, o, n) -> {
            controller.setAssignmentFilter(n.isBlank() ? null : n);
            refresh();
        });

        rubricField.textProperty().addListener((obs, o, n) -> {
            controller.setRubricFilter(n.isBlank() ? null : n);
            refresh();
        });

        tagField.textProperty().addListener((obs, o, n) -> {
            controller.setTagFilter(n.isBlank() ? null : n);
            refresh();
        });

        insertButton.setOnAction(e -> onInsert());
        deleteButton.setOnAction(e -> onDelete());
        importButton.setOnAction(e -> onImport());
        exportButton.setOnAction(e -> onExport());
    }

    /* ------------------------------------------------------------
       Wiring from GradingController
       ------------------------------------------------------------ */

    public void setController(CommentBrowserController controller) {
        this.controller = controller;
        refresh();
    }

    /* ------------------------------------------------------------
       Actions
       ------------------------------------------------------------ */

    private void refresh() {
        List<CommentTemplate> results = controller.getVisibleComments();
        commentList.setItems(FXCollections.observableArrayList(results));
    }

    private void onInsert() {
        CommentTemplate selected = commentList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            controller.insertComment(selected);
        }
    }

    private void onDelete() {
        CommentTemplate selected = commentList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            controller.deleteComment(selected.getId());
            refresh();
        }
    }

    private void onImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Comments");
        File file = chooser.showOpenDialog(commentList.getScene().getWindow());
        if (file != null) {
            controller.importComments(file.toPath());
            refresh();
        }
    }

    private void onExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Comments");
        File file = chooser.showSaveDialog(commentList.getScene().getWindow());
        if (file != null) {
            controller.exportComments(file.toPath());
        }
    }
}
