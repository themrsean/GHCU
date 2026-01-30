/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package grading.ui;

import grading.model.ReportState;
import javafx.css.PseudoClass;
import javafx.scene.control.ListCell;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * List cell for displaying report files in the grading UI.
 * <p>
 * The cell displays the report filename and indicates whether the report has
 * unsaved edits (dirty state) by applying a CSS pseudoclass.
 * <p>
 * Expected CSS:
 * <pre>
 * .list-cell:dirty-report {
 *     -fx-font-weight: bold;
 * }
 * </pre>
 *
 * @author Sean Jones
 */
public final class ReportCell extends ListCell<Path> {
    private static final PseudoClass DIRTY_REPORT =
            PseudoClass.getPseudoClass("dirty-report");

    private final Map<Path, ReportState> stateMap;

    /**
     * Creates a report cell backed by the given report state map.
     *
     * @param stateMap map from report path to its current editor state
     * @throws NullPointerException if {@code stateMap} is {@code null}
     */
    public ReportCell(Map<Path, ReportState> stateMap) {
        this.stateMap = Objects.requireNonNull(stateMap, "stateMap");
    }

    /**
     * Updates the cell to display the given report path.
     * <p>
     * If the report has a corresponding {@link ReportState} and that state is dirty,
     * the cell is marked using the {@code dirty-report} pseudoclass.
     *
     * @param item report file path for this cell
     * @param empty whether the cell is empty
     */
    @Override
    protected void updateItem(Path item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            pseudoClassStateChanged(DIRTY_REPORT, false);
        } else {
            ReportState state = stateMap.get(item);
            boolean dirty = state != null && state.isDirty();
            setText(item.getFileName().toString());
            setGraphic(null);
            pseudoClassStateChanged(DIRTY_REPORT, dirty);
        }
    }
}