/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package main.java.grading;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import main.java.comments.persistence.CommentRepository;
import main.java.comments.persistence.JsonCommentRepository;
import main.java.comments.service.CommentInjectionService;
import main.java.comments.ui.CommentBrowserController;
import main.java.comments.ui.CommentBrowserFxController;
import main.java.ui.ReportCell;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class GradingController implements Initializable {
    private static final double FONT_STEP = 1.0;
    private static final double MIN_FONT_SIZE = 8.0;
    private static final double MAX_FONT_SIZE = 32.0;
    private static final double DEFAULT_FONT_SIZE = 12.0;
    private static final double DEFAULT_SIDEBAR_WIDTH = 125.0;
    private static final Duration AUTOSAVE_DELAY = Duration.millis(500);

    private final ObservableList<Path> reports =
            FXCollections.observableArrayList();
    private final Map<Path, ReportState> stateMap = new HashMap<>();
    private final PauseTransition autosaveTimer =
            new PauseTransition(AUTOSAVE_DELAY);
    private final UIState uiState =
            new UIState(GradingController.class);
    private VirtualizedScrollPane<CodeArea> editorScroll;
    private CommentRepository commentRepository;
    private CommentInjectionService commentInjectionService;
    private CodeArea editor;
    private FindBarController findBarController;

    @FXML
    @SuppressWarnings("rawtypes")
    private ListView reportListView;
    @FXML
    private SplitPane splitPane;
    @FXML
    private VBox editorContainer;

    private Path currentReport;
    private double fontSize = DEFAULT_FONT_SIZE;
    private boolean programmaticEdit = false;
    private String lastSearchText = "";
    private int totalMatches = 0;
    private int currentMatchNumber = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configureReportListView();
        configureSplitPanePersistence();
        setupEditor();
        configureReportSelection();
        configureEditorChangeTracking();
        configureFontHandling();
        configureAutosave();
        configureFindBar();
        initializeCommentSystem();
    }

    @SuppressWarnings("unchecked")
    private void configureReportListView() {
        reportListView.setCellFactory(_ -> new ReportCell(stateMap));
        reportListView.setItems(reports);
    }

    private void configureSplitPanePersistence() {
        Platform.runLater(() -> {
            double width = uiState.loadSidebarWidth(DEFAULT_SIDEBAR_WIDTH);
            splitPane.setDividerPositions(width / splitPane.getWidth());
        });
        splitPane.getDividers().getFirst().positionProperty()
                .addListener((_, _, pos) ->
                        uiState.saveSidebarWidth(
                                pos.doubleValue() * splitPane.getWidth()
                        ));
    }

    private void setupEditor() {
        editor = new CodeArea();
        editor.setWrapText(false);
        editorScroll = new VirtualizedScrollPane<>(editor);
        VBox.setVgrow(editorScroll, javafx.scene.layout.Priority.ALWAYS);
        editorContainer.getChildren().setAll(editorScroll);
    }

    @SuppressWarnings("unchecked")
    private void configureReportSelection() {
        reportListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((_, _, newReport) -> {
                    saveCurrentState();
                    if (newReport != null) {
                        try {
                            loadState((Path) newReport);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                });
    }

    private void configureEditorChangeTracking() {
        editor.textProperty().addListener((_, _, text) -> {
            if (!programmaticEdit && currentReport != null) {
                ReportState state =
                        stateMap.computeIfAbsent(currentReport, _ -> new ReportState());
                state.recordEdit(text);
                state.markDirty();
                reportListView.refresh();
                autosaveTimer.playFromStart();
                editor.setStyleSpans(
                        0,
                        JavaSyntaxHighlighter.computeHighlighting(text)
                );
            }
        });
        editor.caretPositionProperty().addListener((_, _, n) -> {
            if (!programmaticEdit && currentReport != null) {
                ReportState state = stateMap.get(currentReport);
                if (state != null) {
                    state.updateCaret(n);
                }
            }
        });
        editor.plainTextChanges().subscribe(change -> {
            if (!programmaticEdit && currentReport != null) {
                ReportState state = stateMap.get(currentReport);
                if (state != null) {
                    commentInjectionService.onTextChanged(change, state);
                }
            }
        });

    }

    private void configureFontHandling() {
        fontSize = uiState.loadFontSize(DEFAULT_FONT_SIZE);
        applyFontSize();
    }

    private void configureAutosave() {
        autosaveTimer.setOnFinished(_ -> autosaveCurrentReport());
    }

    private void configureFindBar() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/grading/findbar.fxml"));
            HBox findBar = loader.load();
            findBarController = loader.getController();
            findBarController.bind(this);
            editorContainer.getChildren().addFirst(findBar);
            findBar.setVisible(false);
            findBar.setManaged(false);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FindBar", e);
        }
    }

    private void initializeCommentSystem() {
        ObjectMapper mapper = new ObjectMapper();
        Path commentStore =
                Path.of(System.getProperty("user.home"), ".ghcu", "comments.json");
        commentRepository =
                new JsonCommentRepository(commentStore, mapper);
        commentInjectionService =
                new CommentInjectionService();
    }

    public void openCommentBrowser() {
        if (currentReport != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/comments/comment_browser.fxml")
                );
                Parent root = loader.load();
                CommentBrowserFxController fxController = loader.getController();
                CommentBrowserController logicController =
                        new CommentBrowserController(
                                commentRepository,
                                commentInjectionService
                        );
                // CRITICAL: pass the EXISTING editor and ReportState
                ReportState state = stateMap.get(currentReport);
                logicController.setActiveContext(editor, state);
                fxController.setController(logicController);
                Stage stage = new Stage();
                stage.setTitle("Comment Browser");
                stage.setScene(new Scene(root));
                stage.initOwner(editor.getScene().getWindow());
                stage.show();
            } catch (IOException e) {
                throw new RuntimeException("Failed to open Comment Browser", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
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
        Map<KeyCombination, Runnable> accelerators = scene.getAccelerators();
        accelerators.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.Z,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                this::undo
        );
        accelerators.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.Y,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                this::redo
        );
        accelerators.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.COMMA,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                () -> selectRelative(-1)
        );
        accelerators.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.PERIOD,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                () -> selectRelative(1)
        );
        accelerators.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.EQUALS,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                () -> setFontSize(fontSize + FONT_STEP)
        );
        accelerators.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.MINUS,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                () -> setFontSize(fontSize - FONT_STEP)
        );
        accelerators.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.DIGIT0,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                () -> setFontSize(DEFAULT_FONT_SIZE)
        );
        accelerators.put(
                new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.S,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                ),
                this::manualSaveCurrentReport
        );
        accelerators.put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN),
                () -> {
                    findBarController.setReplaceMode(false);
                    findBarController.show();
                }
        );
        accelerators.put(
                new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN),
                () -> {
                    findBarController.setReplaceMode(true);
                    findBarController.show();
                }
        );
        accelerators.put(
                new KeyCodeCombination(KeyCode.ESCAPE),
                () -> {
                    findBarController.hide();
                    clearFindHighlighting();
                }
        );
        accelerators.put(
                new KeyCodeCombination(KeyCode.SLASH, KeyCombination.CONTROL_DOWN),
                this::openCommentBrowser
        );
    }

    public void onClose() {
        // Stop autosave timer
        autosaveTimer.stop();
        // Save current editor state first
        saveCurrentState();
        // Flush all dirty reports
        for (Map.Entry<Path, ReportState> entry : stateMap.entrySet()) {
            Path report = entry.getKey();
            ReportState state = entry.getValue();
            if (state.isDirty()) {
                try {
                    Files.writeString(report, state.getText());
                    state.markClean();
                } catch (IOException e) {
                    System.err.println("Failed to save report on close: " + report);
                }
            }
        }
    }

    private void applyFontSize() {
        editor.setStyle("-fx-font-size: " + fontSize + "pt;");
    }

    private void saveCurrentState() {
        if (currentReport != null) {
            stateMap.computeIfAbsent(currentReport, _ -> new ReportState());
            uiState.saveScroll(
                    currentReport,
                    editorScroll.getEstimatedScrollY()
            );
            autosaveCurrentReport(); // one forced save on switch
        }
    }

    private void loadState(Path report) throws IOException {
        clearFindHighlighting();
        ReportState state =
                stateMap.computeIfAbsent(report, _ -> new ReportState());
        String text = Files.readString(report);
        programmaticEdit = true;
        state.reset(text);
        editor.replaceText(text);
        editor.setStyleSpans(
                0,
                JavaSyntaxHighlighter.computeHighlighting(text)
        );
        editor.moveTo(
                Math.min(state.getCaretPosition(), editor.getLength())
        );
        Platform.runLater(() -> {
            centerCaretInView();
            editorScroll.scrollYToPixel(uiState.loadScroll(report));
        });
        programmaticEdit = false;
        currentReport = report;
        uiState.saveLastReport(report);
    }

    private void centerCaretInView() {
        int caretPos = editor.getCaretPosition();
        int paragraph =
                editor.offsetToPosition(caretPos, CodeArea.Bias.Forward).getMajor();
        editor.getParagraphBoundsOnScreen(paragraph).ifPresent(bounds -> {
            double caretCenterY =
                    bounds.getMinY() + bounds.getHeight() / 2;
            double viewportCenterY =
                    editorScroll.localToScreen(editorScroll.getBoundsInLocal())
                            .getMinY()
                            + editorScroll.getHeight() / 2;
            double delta = caretCenterY - viewportCenterY;
            editorScroll.scrollYBy(delta);
        });
    }

    private void selectRelative(int delta) {
        int size = reports.size();
        if (size != 0) {
            int index = reportListView.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                int target = (index + delta + size) % size;
                if (target >= 0 && target < reports.size()) {
                    reportListView.getSelectionModel().select(target);
                    reportListView.scrollTo(target);
                }
            }
        }
    }

    private void autosaveCurrentReport() {
        ReportState state = stateMap.get(currentReport);
        if (currentReport != null && state != null && state.isDirty()) {
            try {
                Files.writeString(currentReport, state.getText());
                state.markClean();
                reportListView.refresh();
            } catch (IOException e) {
                System.err.println("Autosave failed: " + currentReport);
            }
        }
    }

    private void setFontSize(double newSize) {
        fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, newSize));
        applyFontSize();
        uiState.saveFontSize(fontSize);
    }

    private void undo() {
        ReportState state = stateMap.get(currentReport);
        if (currentReport != null & state != null || Objects.requireNonNull(state).canUndo()) {
            programmaticEdit = true;
            editor.replaceText(state.undo());
            programmaticEdit = false;
            editor.setStyleSpans(
                    0,
                    JavaSyntaxHighlighter.computeHighlighting(editor.getText())
            );
            editor.moveTo(
                    Math.min(state.getCaretPosition(), editor.getLength())
            );
            editorScroll.scrollYToPixel(state.getScrollLocation());
        }

    }

    private void redo() {
        ReportState state = stateMap.get(currentReport);
        if (currentReport != null && state != null && state.canRedo()) {
            programmaticEdit = true;
            editor.replaceText(state.redo());
            programmaticEdit = false;
            editor.setStyleSpans(
                    0,
                    JavaSyntaxHighlighter.computeHighlighting(editor.getText())
            );
            editor.moveTo(
                    Math.min(state.getCaretPosition(), editor.getLength())
            );
            editorScroll.scrollYToPixel(state.getScrollLocation());
        }
    }

    private void manualSaveCurrentReport() {
        if (currentReport != null) {
            ReportState state = stateMap.get(currentReport);
            if (state != null) {
                try {
                    Files.writeString(currentReport, editor.getText());
                    state.markClean();
                    reportListView.refresh();
                } catch (IOException e) {
                    System.err.println("Manual save failed: " + currentReport);
                }
            }
        }
    }

    void findNext(String query) {
        find(query, true);
    }

    void findPrevious(String query) {
        find(query, false);
    }

    private void find(String query, boolean forward) {
        if (query == null || query.isEmpty()) {
            clearFindHighlighting();
            totalMatches = 0;
            currentMatchNumber = 0;
            updateFindStatus();
        } else {
            String text = editor.getText();
            // NEW: find all match positions
            java.util.List<Integer> matches = findAllMatches(text, query);
            totalMatches = matches.size();
            if (totalMatches == 0) {
                clearFindHighlighting();
                currentMatchNumber = 0;
                updateFindStatus();
            } else {
                boolean newSearch = !query.equalsIgnoreCase(lastSearchText);
                // NEW: reset match index on new search
                if (newSearch) {
                    currentMatchNumber = 1;
                } else {
                    currentMatchNumber += forward ? 1 : -1;
                    // wrap around
                    if (currentMatchNumber > totalMatches) {
                        currentMatchNumber = 1;
                    }
                    if (currentMatchNumber < 1) {
                        currentMatchNumber = totalMatches;
                    }
                }
                int index = matches.get(currentMatchNumber - 1);
                lastSearchText = query;
                editor.selectRange(index, index + query.length());
                editor.requestFollowCaret();
                centerOnIndex(index);
                editor.setStyleSpans(
                        0,
                        computeFindHighlights(text, query, index)
                );
                updateFindStatus();
            }
        }
    }

    private java.util.List<Integer> findAllMatches(String text, String query) {
        java.util.List<Integer> matches = new java.util.ArrayList<>();
        if (query == null || query.isEmpty()) {
            return matches;
        }
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int index = 0;
        while ((index = lowerText.indexOf(lowerQuery, index)) >= 0) {
            matches.add(index);
            index += query.length();
        }
        return matches;
    }

    private void updateFindStatus() {
        if (findBarController != null) {
            findBarController.updateMatchCount(
                    currentMatchNumber,
                    totalMatches
            );
        }
    }

    private void centerOnIndex(int index) {
        int paragraph = editor.offsetToPosition(index,
                        org.fxmisc.richtext.model.TwoDimensional.Bias.Forward)
                .getMajor();

        editor.showParagraphAtCenter(paragraph);
    }

    void clearFindHighlighting() {
        editor.setStyleSpans(
                0,
                JavaSyntaxHighlighter.computeHighlighting(editor.getText())
        );
    }

    void replaceCurrent(String find, String replace) {
        if (!editor.getSelectedText().equals(find)) {
            findNext(find);
        } else {
            programmaticEdit = true;
            editor.replaceSelection(replace);
            programmaticEdit = false;
            findNext(find);
        }
    }

    void replaceAll(String find, String replace) {
        if (find != null && !find.isEmpty()) {
            programmaticEdit = true;
            editor.replaceText(editor.getText().replace(find, replace));
            programmaticEdit = false;
            clearFindHighlighting();
        }
    }

    private StyleSpans<Collection<String>> computeFindHighlights(
            String text,
            String query,
            int currentIndex
    ) {
        StyleSpansBuilder<Collection<String>> spans =
                new StyleSpansBuilder<>();
        if (query == null || query.isEmpty()) {
            spans.add(java.util.Collections.emptyList(), text.length());
            return spans.create();
        }
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int last = 0;
        int index;
        while ((index = lowerText.indexOf(lowerQuery, last)) >= 0) {
            spans.add(java.util.Collections.emptyList(), index - last);
            boolean isCurrent = index == currentIndex;
            spans.add(
                    java.util.List.of(
                            isCurrent ? "find-current" : "find-match"
                    ),
                    query.length()
            );

            last = index + query.length();
        }
        spans.add(java.util.Collections.emptyList(), text.length() - last);
        return spans.create();
    }
}
