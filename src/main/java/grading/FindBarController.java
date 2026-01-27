/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package main.java.grading;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class FindBarController {
    @FXML private HBox root;
    @FXML private TextField findField;
    @FXML private TextField replaceField;
    @FXML private Label matchLabel;

    private GradingController gradingController;

    void bind(GradingController controller) {
        this.gradingController = controller;
        findField.textProperty().addListener((_, _, text) -> {
            if (text == null || text.isEmpty()) {
                gradingController.clearFindHighlighting();
            }
        });
        findField.setOnAction(_ ->
                gradingController.findNext(findField.getText())
        );
        findField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && e.isShiftDown()) {
                gradingController.findPrevious(findField.getText());
                e.consume();
            }
        });

    }

    void setReplaceMode(boolean enabled) {
        replaceField.setVisible(enabled);
        replaceField.setManaged(enabled);
        if (!enabled) {
            replaceField.clear();
        }
    }

    void show() {
        root.setVisible(true);
        root.setManaged(true);
        findField.requestFocus();
        findField.selectAll();
    }

    void hide() {
        root.setVisible(false);
        root.setManaged(false);
        gradingController.clearFindHighlighting();
    }

    @FXML
    private void onNext() {
        gradingController.findNext(findField.getText());
    }

    @FXML
    private void onPrevious() {
        gradingController.findPrevious(findField.getText());
    }

    @FXML
    private void onReplace() {
        gradingController.replaceCurrent(
                findField.getText(),
                replaceField.getText()
        );
    }

    @FXML
    private void onReplaceAll() {
        gradingController.replaceAll(
                findField.getText(),
                replaceField.getText()
        );
    }

    void updateMatchCount(int current, int total) {
        matchLabel.setText(
                total == 0 ? "0 / 0" : current + " / " + total
        );
    }

    @FXML
    private void onClose() {
        hide();
    }
}
