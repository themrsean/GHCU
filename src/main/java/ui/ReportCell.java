/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package main.java.ui;

import javafx.scene.control.ListCell;
import main.java.grading.ReportState;

import java.nio.file.Path;
import java.util.Map;

public final class ReportCell extends ListCell<Path> {

    private final Map<Path, ReportState> stateMap;

    public ReportCell(Map<Path, ReportState> stateMap) {
        this.stateMap = stateMap;
    }

    @Override
    protected void updateItem(Path item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            ReportState state = stateMap.get(item);
            boolean dirty = state != null && state.isDirty();
            String name = item.getFileName().toString();
            setText(dirty ? "● " + name : name);
            setStyle(dirty
                    ? "-fx-font-weight: bold;"
                    : "");
        }
    }
}
