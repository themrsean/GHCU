/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package main.java.grading;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.util.Duration;
import main.java.ui.ReportCell;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class GradingController implements Initializable {
    private static final double FONT_STEP = 1.0;
    private static final double MIN_FONT_SIZE = 8.0;
    private static final double MAX_FONT_SIZE = 32.0;
    private static final double DEFAULT_FONT_SIZE = 12.0;
    private static final double DEFAULT_SIDEBAR_WIDTH = 200.0;
    private static final Duration AUTOSAVE_DELAY = Duration.millis(500);

    private final ObservableList<Path> reports =
            FXCollections.observableArrayList();
    private final Map<Path, ReportState> stateMap = new HashMap<>();
    private final PauseTransition autosaveTimer =
            new PauseTransition(AUTOSAVE_DELAY);
    private final UIState uiState =
            new UIState(GradingController.class);

    @FXML
    private ListView<Path> reportListView;
    @FXML
    private TextArea editor;
    @FXML
    private SplitPane splitPane;

    private Stage stage;
    private Path currentReport;
    private double fontSize = DEFAULT_FONT_SIZE;
    private boolean programmaticEdit = false;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configureReportListView();
        configureSplitPanePersistence();
        configureReportSelection();
        configureEditorChangeTracking();
        configureFontHandling();
        configureAutosave();
    }

    private void configureReportListView() {
        reportListView.setCellFactory(lv -> new ReportCell(stateMap));
        reportListView.setItems(reports);
    }

    private void configureSplitPanePersistence() {
        Platform.runLater(() -> {
            double width = uiState.loadSidebarWidth(DEFAULT_SIDEBAR_WIDTH);
            splitPane.setDividerPositions(width / splitPane.getWidth());
        });

        splitPane.getDividers().getFirst().positionProperty()
                .addListener((obs, old, pos) ->
                        uiState.saveSidebarWidth(
                                pos.doubleValue() * splitPane.getWidth()
                        ));
    }

    private void configureReportSelection() {
        reportListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldReport, newReport) -> {
                    saveCurrentState();
                    if (newReport != null) {
                        try {
                            loadState(newReport);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                });
    }

    private void configureEditorChangeTracking() {

        editor.textProperty().addListener((obs, old, text) -> {
            if (programmaticEdit || currentReport == null) return;

            ReportState state =
                    stateMap.computeIfAbsent(currentReport, p -> new ReportState());

            state.recordEdit(text);
            state.markDirty();

            reportListView.refresh();
            autosaveTimer.playFromStart();
        });

        editor.caretPositionProperty().addListener((obs, o, n) -> {
            if (programmaticEdit || currentReport == null) return;

            ReportState state = stateMap.get(currentReport);
            if (state != null) {
                state.updateCaret(n.intValue());
            }
        });

        editor.scrollTopProperty().addListener((obs, o, n) -> {
            if (programmaticEdit || currentReport == null) return;

            ReportState state = stateMap.get(currentReport);
            if (state != null) {
                state.updateScroll(n.doubleValue());
            }
        });
    }

    private void configureFontHandling() {
        fontSize = uiState.loadFontSize(DEFAULT_FONT_SIZE);
        applyFontSize();
    }

    private void configureAutosave() {
        autosaveTimer.setOnFinished(e -> autosaveCurrentReport());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void loadReportFolder(Path folder) throws IOException {
        reports.clear();

        try (Stream<Path> stream = Files.list(folder)) {
            stream
                    .filter(p -> p.toString().endsWith(".html"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(reports::add);
        }

        // Restore last opened report if possible
        Path last = uiState.loadLastReport();
        if (last != null && reports.contains(last)) {
            reportListView.getSelectionModel().select(last);
        } else if (!reports.isEmpty()) {
            reportListView.getSelectionModel().select(0);
        }
    }

    public void installAccelerators(Scene scene) {
        var accels = scene.getAccelerators();

        accels.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.Z,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                this::undo
        );

        accels.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.Y,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                this::redo
        );

        accels.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.UP,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                () -> selectRelative(-1)
        );

        accels.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.DOWN,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                () -> selectRelative(1)
        );

        accels.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.EQUALS,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                () -> setFontSize(fontSize + FONT_STEP)
        );

        accels.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.MINUS,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                () -> setFontSize(fontSize - FONT_STEP)
        );

        accels.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.DIGIT0,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                () -> setFontSize(DEFAULT_FONT_SIZE)
        );
    }

    private void applyFontSize() {
        editor.setStyle("-fx-font-size: " + fontSize + "pt;");
    }

    private void saveCurrentState() {
        if (currentReport == null) return;
        ReportState state =
                stateMap.computeIfAbsent(currentReport, p -> new ReportState());
        uiState.saveScroll(currentReport, editor.getScrollTop());
        autosaveCurrentReport(); // one forced save on switch
    }

    private void loadState(Path report) throws IOException {
        ReportState state =
                stateMap.computeIfAbsent(report, p -> new ReportState());

        String text = Files.readString(report);

        programmaticEdit = true;

        state.reset(text);
        editor.setText(text);

        editor.positionCaret(
                Math.min(state.getCaretPosition(), editor.getLength())
        );
        editor.setScrollTop(uiState.loadScroll(report));

        programmaticEdit = false;

        currentReport = report;
        uiState.saveLastReport(report);
    }

    private void selectRelative(int delta) {
        int size = reports.size();
        if (size == 0) return;

        int index = reportListView.getSelectionModel().getSelectedIndex();
        if (index < 0) return;

        int target = (index + delta + size) % size;
        if (target >= 0 && target < reports.size()) {
            reportListView.getSelectionModel().select(target);
            reportListView.scrollTo(target);
        }
    }

    private void autosaveCurrentReport() {
        if (currentReport == null) return;

        ReportState state = stateMap.get(currentReport);
        if (state == null || !state.isDirty()) return;

        try {
            Files.writeString(currentReport, state.getText());
            state.markClean();
            reportListView.refresh();
        } catch (IOException e) {
            System.err.println("Autosave failed: " + currentReport);
        }
    }

    private void setFontSize(double newSize) {
        fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, newSize));
        applyFontSize();
        uiState.saveFontSize(fontSize);
    }

    private void undo() {
        if (currentReport == null) return;

        ReportState state = stateMap.get(currentReport);
        if (state == null || !state.canUndo()) return;

        programmaticEdit = true;
        editor.setText(state.undo());
        programmaticEdit = false;

        editor.positionCaret(
                Math.min(state.getCaretPosition(), editor.getLength())
        );
        editor.setScrollTop(state.getScrollLocation());
    }

    private void redo() {
        if (currentReport == null) return;

        ReportState state = stateMap.get(currentReport);
        if (state == null || !state.canRedo()) return;

        programmaticEdit = true;
        editor.setText(state.redo());
        programmaticEdit = false;

        editor.positionCaret(
                Math.min(state.getCaretPosition(), editor.getLength())
        );
        editor.setScrollTop(state.getScrollLocation());
    }
}
