/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/30/2026
 */
package assignments.ui;

import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Custom {@link ListCell} implementation for displaying and editing file names in an
 * assignment's file list.
 * <p>
 * This cell supports inline editing via double-click. When the user commits an edit,
 * the new value is validated to ensure it is non-blank and does not create a duplicate
 * file name (as determined by the provided duplicate-check predicate).
 * </p>
 *
 * <p>
 * Duplicate entries are visually highlighted using a red-tinted background.
 * </p>
 *
 * @author Sean Jones
 */
public class FilesCell extends ListCell<String> {

    private final TextField editor = new TextField();
    private final Predicate<String> duplicateCheck;

    /**
     * Constructs a file-name list cell.
     *
     * @param duplicateCheck predicate that returns {@code true} if the given filename
     *                       would create a duplicate entry
     * @throws NullPointerException if {@code duplicateCheck} is {@code null}
     */
    public FilesCell(Predicate<String> duplicateCheck) {
        this.duplicateCheck = duplicateCheck;
        configureInlineEdit();
    }

    /* INLINE EDIT */
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

    /* CELL LIFECYCLE */
    /**
     * Updates the contents and style of this cell.
     * <p>
     * When not editing, the cell displays the file name. If the file name is a duplicate,
     * the cell is highlighted.
     * </p>
     *
     * @param item the file name associated with this cell
     * @param empty whether this cell is empty
     */
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setStyle("");
        } else if (isEditing()) {
            setGraphic(editor);
            setText(null);
        } else {
            setText(item);
            setGraphic(null);
            updateDuplicateStyle(item);
        }
    }

    /**
     * Starts inline editing for this cell.
     * <p>
     * The current file name is loaded into the editor and the text is selected so the
     * user can quickly rename the file.
     * </p>
     */
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

    /**
     * Commits the edit if the new value is valid.
     * <p>
     * The edit is cancelled if:
     * </p>
     * <ul>
     *     <li>The new value is {@code null} or blank</li>
     *     <li>The new value differs from the current value and would create a duplicate</li>
     * </ul>
     *
     * @param newValue the new file name value entered by the user
     */
    @Override
    public void commitEdit(String newValue) {
        String current = getItem();
        if (newValue == null || newValue.isBlank()) {
            cancelEdit();
        } else if (current != null
                && !newValue.equals(current)
                && duplicateCheck.test(newValue)) {
            cancelEdit();
        } else {
            super.commitEdit(newValue);
        }
    }

    /**
     * Cancels editing and restores the cell to display mode.
     * <p>
     * The cell styling is also refreshed to ensure duplicate highlighting is correct.
     * </p>
     */
    @Override
    public void cancelEdit() {
        super.cancelEdit();
        String item = getItem();
        setText(item);
        setGraphic(null);
        if (item != null) {
            updateDuplicateStyle(item);
        } else {
            setStyle("");
        }
    }

    /* DUPLICATE VISUAL */
    private void updateDuplicateStyle(String value) {
        Objects.requireNonNull(duplicateCheck, "duplicateCheck");
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