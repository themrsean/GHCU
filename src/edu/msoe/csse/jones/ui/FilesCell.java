/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package edu.msoe.csse.jones.ui;

import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;

import java.util.function.Predicate;

public class FilesCell extends ListCell<String> {

    private final TextField editor = new TextField();
    private final Predicate<String> duplicateCheck;

    /**
     * Creates a Cell for the ListView of Files
     * @param duplicateCheck returns true if the given filename already exists
     */
    public FilesCell(Predicate<String> duplicateCheck) {
        this.duplicateCheck = duplicateCheck;
        configureInlineEdit();
    }

    /* ---------- INLINE EDIT ---------- */

    private void configureInlineEdit() {
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !isEmpty()) {
                startEdit();
            }
        });

        editor.setOnAction(_ -> commitEdit(editor.getText()));

        editor.focusedProperty().addListener((_, _, is) -> {
            if (!is) {
                cancelEdit();
            }
        });
    }

    /* ---------- CELL LIFECYCLE ---------- */

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setStyle("");
        } else {
            setText(item);
            setGraphic(null);
            updateDuplicateStyle(item);
        }
    }

    @Override
    public void startEdit() {
        super.startEdit();
        if (getItem() != null) {
            editor.setText(getItem());
            setGraphic(editor);
            setText(null);
            editor.selectAll();
            editor.requestFocus();
        }
    }

    @Override
    public void commitEdit(String newValue) {
        if (newValue == null || newValue.isBlank()) {
            cancelEdit();
        } else if (!newValue.equals(getItem()) && duplicateCheck.test(newValue)) {
            cancelEdit();
        } else {
            super.commitEdit(newValue);
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem());
        setGraphic(null);
    }

    /* ---------- DUPLICATE VISUAL ---------- */

    private void updateDuplicateStyle(String value) {
        if (duplicateCheck.test(value)) {
            setStyle("""
                    -fx-background-color: rgba(255,0,0,0.25);
                    -fx-border-color: red;
                    """);
        } else {
            setStyle("");
        }
    }
}
