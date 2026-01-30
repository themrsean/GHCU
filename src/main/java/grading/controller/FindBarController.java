/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package grading.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import java.util.Objects;

/**
 * Controller for the grading find/replace bar.
 * <p>
 * This controller is designed to be loaded via FXML and then bound to a
 * {@link GradingController} instance using {@link #bind(GradingController)}.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Wire find/replace UI controls to grading operations</li>
 *     <li>Support find-next / find-previous via Enter / Shift+Enter</li>
 *     <li>Support replace and replace-all actions</li>
 *     <li>Show/hide bar and update match count</li>
 * </ul>
 *
 * @author Sean Jones
 */
public class FindBarController {
    @FXML private HBox root;
    @FXML private TextField findField;
    @FXML private TextField replaceField;
    @FXML private Label matchLabel;

    private GradingController gradingController;
    private boolean bound;

    /* Initialization */
    /**
     * Binds this find bar to a {@link GradingController}.
     * <p>
     * This method is intended to be called exactly once after FXML loading.
     *
     * @param controller grading controller to bind actions to
     * @throws NullPointerException if {@code controller} is null
     * @throws IllegalStateException if called more than once
     */
    void bind(GradingController controller) {
        Objects.requireNonNull(controller, "controller");
        if (bound) {
            throw new IllegalStateException("FindBarController.bind() called more than once");
        }
        Objects.requireNonNull(root, "root (FXML injection failed)");
        Objects.requireNonNull(findField, "findField (FXML injection failed)");
        Objects.requireNonNull(replaceField, "replaceField (FXML injection failed)");
        Objects.requireNonNull(matchLabel, "matchLabel (FXML injection failed)");
        gradingController = controller;
        bound = true;
        configureFindField();
        configureKeyboardHandling();
    }

    private void configureFindField() {
        findField.textProperty().addListener((_, _, text) -> {
            if (text == null || text.isEmpty()) {
                GradingController controller = gradingController;
                if (controller != null) {
                    controller.clearFindHighlighting();
                }
            }
        });

        // Enter => find next
        findField.setOnAction(_ -> {
            GradingController controller = gradingController;
            if (controller != null) {
                controller.findNext(findField.getText());
            }
        });

        // Shift+Enter => find previous
        findField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && e.isShiftDown()) {
                GradingController controller = gradingController;
                if (controller != null) {
                    controller.findPrevious(findField.getText());
                }
                e.consume();
            }
        });
    }

    /**
     * Adds key handling local to the bar.
     * <p>
     * This is mainly to ensure ESC works even when a TextField consumes key events.
     */
    private void configureKeyboardHandling() {
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hide();
                e.consume();
            }
        });
    }

    /* Visibility and setting mode */
    /**
     * Enables/disables replace mode.
     * <p>
     * When disabled, the replace field is hidden and cleared.
     *
     * @param enabled true to show replace field; false to hide it
     */
    void setReplaceMode(boolean enabled) {
        replaceField.setVisible(enabled);
        replaceField.setManaged(enabled);
        if (!enabled) {
            replaceField.clear();
        }
    }

    /**
     * Shows the find bar and focuses the find field.
     */
    void show() {
        root.setVisible(true);
        root.setManaged(true);
        findField.requestFocus();
        findField.selectAll();
    }

    /**
     * Hides the find bar and clears find highlighting in the editor.
     */
    void hide() {
        root.setVisible(false);
        root.setManaged(false);
        if (gradingController != null) {
            gradingController.clearFindHighlighting();
        }
    }

    /**
     * Updates the match label (ex: "2 / 10").
     *
     * @param current current match number (1-based)
     * @param total total number of matches
     */
    void updateMatchCount(int current, int total) {
        matchLabel.setText(
                total == 0 ? "0 / 0" : current + " / " + total
        );
    }

    /* Event handlers */
    @FXML
    private void onNext() {
        if (gradingController != null) {
            gradingController.findNext(findField.getText());
        }
    }

    @FXML
    private void onPrevious() {
        if (gradingController != null) {
            gradingController.findPrevious(findField.getText());
        }
    }

    @FXML
    private void onReplace() {
        if (gradingController != null) {
            gradingController.replaceCurrent(findField.getText(), replaceField.getText());
        }
    }

    @FXML
    private void onReplaceAll() {
        if (gradingController != null) {
            gradingController.replaceAll(findField.getText(), replaceField.getText());
        }
    }

    @FXML
    private void onClose() {
        hide();
    }
}
