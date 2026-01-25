/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package main.java.ui;

import main.java.model.Assignment;
import javafx.application.Platform;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.io.File;

public class AssignmentCell extends ListCell<Assignment> {
    private static final String DROP_STYLE =
            "-fx-background-color: derive(-fx-accent, 70%);" +
                    "-fx-border-color: -fx-accent; -fx-border-width: 2;";
    private final TextField editor = new TextField();

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

        editor.setOnAction(_ -> commitEdit(getItem()));
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
                for (File file : db.getFiles()) {
                    String name = file.getName();
                    if (!assignment.getFiles().contains(name)) {
                        assignment.getFiles().add(name);
                    }
                }
                success = true;
            }
            setStyle("");
            e.setDropCompleted(success);
            e.consume();
        });
    }

    @Override
    protected void updateItem(Assignment item, boolean empty) {
        super.updateItem(item, empty);
        setStyle("");
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else if (isEditing()) {
            editor.setText(format(item));
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
            assignment.setShortName(parts[0].trim());
            assignment.setFullName(parts[1].trim());
            super.commitEdit(assignment);
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
}
