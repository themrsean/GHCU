/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package comments.ui;

import comments.domain.CommentTemplate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * JavaFX controller for the comment browser UI.
 * <p>
 * This class binds filter/search UI controls to a {@link CommentBrowserController}
 * and displays matching {@link CommentTemplate} entries in a list.
 */
public final class CommentBrowserFxController {

    /* UI Controls */
    @FXML private TextField searchField;
    @FXML private TextField assignmentField;
    @FXML private TextField rubricField;
    @FXML private TextField tagField;

    @FXML private ListView<CommentTemplate> commentList;

    @FXML private Button insertButton;
    @FXML private Button deleteButton;
    @FXML private Button importButton;
    @FXML private Button exportButton;

    /* Backing model for list */
    private final ObservableList<CommentTemplate> visibleComments =
            FXCollections.observableArrayList();

    /* Logic Controller */
    private CommentBrowserController controller;

    /* Initialization */
    @FXML
    private void initialize() {
        commentList.setItems(visibleComments);
        commentList.setCellFactory(_ ->
                new ListCell<>() {
                    @Override
                    protected void updateItem(CommentTemplate item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.title());
                    }
                }
        );

        // Disable UI until controller is wired
        setUiEnabled(false);

        // Wire listeners (safe even before controller is set)
        searchField.textProperty().addListener((_, _, text) -> {
            if (controller != null) {
                controller.setSearchQuery(text);
                refresh();
            }
        });

        assignmentField.textProperty().addListener((_, _, text) -> {
            if (controller != null) {
                controller.setAssignmentFilter(normalizeFilter(text));
                refresh();
            }
        });

        rubricField.textProperty().addListener((_, _, text) -> {
            if (controller != null) {
                controller.setRubricFilter(normalizeFilter(text));
                refresh();
            }
        });

        tagField.textProperty().addListener((_, _, text) -> {
            if (controller != null) {
                controller.setTagFilter(normalizeFilter(text));
                refresh();
            }
        });

        // Button actions
        insertButton.setOnAction(_ -> onInsert());
        deleteButton.setOnAction(_ -> onDelete());
        importButton.setOnAction(_ -> onImport());
        exportButton.setOnAction(_ -> onExport());
    }

    /* Wiring from GradingController */
    /**
     * Wires the backing logic controller used for filtering and actions.
     *
     * @param controller backing controller
     * @throws NullPointerException if controller is null
     */
    public void setController(CommentBrowserController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
        setUiEnabled(true);
        refresh();
    }

    /* UI Refresh */

    private void refresh() {
        if (controller == null) {
            visibleComments.clear();
        } else {
            List<CommentTemplate> results = controller.getVisibleComments();
            visibleComments.setAll(results);
        }
    }

    private void setUiEnabled(boolean enabled) {
        searchField.setDisable(!enabled);
        assignmentField.setDisable(!enabled);
        rubricField.setDisable(!enabled);
        tagField.setDisable(!enabled);

        commentList.setDisable(!enabled);

        insertButton.setDisable(!enabled);
        deleteButton.setDisable(!enabled);
        importButton.setDisable(!enabled);
        exportButton.setDisable(!enabled);
    }

    private static String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /* ------------------------------------------------------------
       Actions
       ------------------------------------------------------------ */

    private void onInsert() {
        boolean ok = controller != null;
        if (ok) {
            CommentTemplate selected =
                    commentList.getSelectionModel().getSelectedItem();

            ok = selected != null;
            if (ok) {
                controller.insertComment(selected);
            }
        }
    }


    private void onDelete() {
        boolean ok = controller != null;
        if (ok) {
            CommentTemplate selected =
                    commentList.getSelectionModel().getSelectedItem();

            ok = selected != null;
            if (ok) {
                controller.deleteComment(selected.id());
                refresh();
            }
        }
    }


    private void onImport() {
        boolean ok = controller != null;
        if (ok) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Import Comments");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            File file = chooser.showOpenDialog(commentList.getScene().getWindow());
            ok = file != null;
            if (ok) {
                controller.importComments(file.toPath());
                refresh();
            }
        }
    }

    private void onExport() {
        boolean ok = controller != null;
        if (ok) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Comments");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            File file = chooser.showSaveDialog(commentList.getScene().getWindow());
            ok = file != null;
            if (ok) {
                controller.exportComments(file.toPath());
            }
        }
    }

}
