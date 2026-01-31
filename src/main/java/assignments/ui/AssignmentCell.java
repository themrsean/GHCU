/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/30/2026
 */
package assignments.ui;

import assignments.model.Assignment;
import javafx.application.Platform;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.io.File;

/**
 * Custom {@link ListCell} implementation for displaying and editing {@link Assignment}
 * objects in the assignment list view.
 * <p>
 * This cell supports:
 * </p>
 * <ul>
 *     <li>Inline editing of assignment metadata (short name and full name) via
 *     double-click</li>
 *     <li>Drag-and-drop of files and folders onto an assignment to add file names
 *     to the assignment's required file list</li>
 * </ul>
 *
 * <p>
 * Drag-and-drop supports recursively expanding folders and adding all allowed files
 * found within them. Only supported file types (currently {@code .java} and
 * {@code .fxml}) are added.
 * </p>
 *
 * @author Sean Jones
 */
public class AssignmentCell extends ListCell<Assignment> {
    private static final String DROP_STYLE =
            "-fx-background-color: derive(-fx-accent, 70%);" +
                    "-fx-border-color: -fx-accent; -fx-border-width: 2;";
    private final TextField editor = new TextField();

    /**
     * Constructs an assignment cell and configures editing and drag-and-drop behavior.
     */
    public AssignmentCell() {
        configureInlineEdit();
        configureDragAndDrop();
    }

    private void configureInlineEdit() {
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !isEmpty()) {
                startEdit();
            }
        });
        editor.setOnAction(_ -> {
            Assignment a = getItem();
            if (a != null) {
                commitEdit(a);
            } else {
                cancelEdit();
            }
        });
        editor.focusedProperty().addListener((_, _, is) -> {
            if (!is) {
                cancelEdit();
            }
        });
    }

    private void configureDragAndDrop() {
        setOnDragOver(e -> {
            if (!isEmpty() && e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                setStyle(DROP_STYLE);
            }
            e.consume();
        });
        setOnDragExited(e -> {
            setStyle("");
            e.consume();
        });
        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (!isEmpty() && db.hasFiles()) {
                Assignment assignment = getItem();
                if (assignment != null) {
                    for (File f : db.getFiles()) {
                        if (f.isDirectory()) {
                            addFolderFiles(assignment, f);
                        } else {
                            addSingleFile(assignment, f);
                        }
                    }
                    success = true;
                }
            }
            setStyle("");
            e.setDropCompleted(success);
            e.consume();
        });
    }

    private void addFolderFiles(Assignment assignment, File folder) {
        File[] children = folder.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    addFolderFiles(assignment, child);
                } else {
                    addSingleFile(assignment, child);
                }
            }
        }
    }

    private void addSingleFile(Assignment assignment, File file) {
        if (isAllowedFile(file)) {
            String name = file.getName();
            if (!assignment.getFiles().contains(name)) {
                assignment.getFiles().add(name);
            }
        }
    }

    @Override
    protected void updateItem(Assignment item, boolean empty) {
        super.updateItem(item, empty);
        setStyle("");
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else if (isEditing()) {
            setGraphic(editor);
            setText(null);
        } else {
            setText(format(item));
            setGraphic(null);
        }
    }

    @Override
    public void startEdit() {
        super.startEdit();
        if (getItem() != null) {
            editor.setText(format(getItem()));
            setGraphic(editor);
            setText(null);
            editor.selectAll();
            Platform.runLater(editor::requestFocus);
        }
    }

    @Override
    public void commitEdit(Assignment assignment) {
        String[] parts = editor.getText().split("\\s*[–-]\\s*", 2);
        if (parts.length == 2) {
            try {
                assignment.setShortName(parts[0].trim());
                assignment.setFullName(parts[1].trim());
                super.commitEdit(assignment);
            } catch (IllegalArgumentException | NullPointerException e) {
                cancelEdit();
            }
        } else {
            cancelEdit();
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        if (getItem() != null) {
            setText(format(getItem()));
        }
        setGraphic(null);
    }

    private String format(Assignment a) {
        return a.getShortName() + " – " + a.getFullName();
    }

    private boolean isAllowedFile(File file) {
        String name = file.getName();
        return file.isFile()
                && (name.endsWith(".java") || name.endsWith(".fxml"));
    }

}
