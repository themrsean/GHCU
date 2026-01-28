/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package grading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import grading.model.ReportRepoIndex;
import grading.model.ReportState;
import grading.model.UIState;
import grading.service.JavaSyntaxHighlighter;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import mainui.service.Utilities;
import comments.persistence.CommentRepository;
import comments.persistence.JsonCommentRepository;
import comments.service.CommentInjectionService;
import comments.ui.CommentBrowserController;
import comments.ui.CommentBrowserFxController;
import grading.ui.ReportCell;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private ReportRepoIndex reportRepoIndex;

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

    public void setReportRepoIndex(ReportRepoIndex reportRepoIndex) {
        this.reportRepoIndex = reportRepoIndex;
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
        accelerators.put(
                new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN),
                this::deployAndPublishCurrentReport
        );
        accelerators.put(
                new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                this::publishAllTouchedRepos
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

    private void deployCurrentReportToRepo() {
        // Ensure latest edits are saved first
        autosaveCurrentReport();

        if (currentReport == null) {
            showInfo("Deploy Report", "No report selected.");
            return;
        }
        if (reportRepoIndex == null) {
            showError("Deploy Report",
                    "Missing report mapping",
                    "No ReportRepoIndex was provided to GradingController.");
            return;
        }

        Optional<Path> repoOpt = reportRepoIndex.findRepoForReport(currentReport);
        if (repoOpt.isEmpty()) {
            showError("Deploy Report",
                    "Repository not found for report",
                    "No repository mapping exists for:\n" + currentReport);
            return;
        }

        Path repoRoot = repoOpt.get();

        // Destination path inside the repo
        // (choose whatever convention you want)
        Path destDir = repoRoot.resolve("feedback");
        Path destFile = destDir.resolve(currentReport.getFileName());

        try {
            Files.createDirectories(destDir);

            // confirm overwrite if exists
            if (Files.exists(destFile)) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Overwrite feedback?");
                confirm.setHeaderText("Feedback report already exists in repository.");
                confirm.setContentText(destFile.toString());
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return;
                }
            }

            Files.copy(currentReport, destFile, StandardCopyOption.REPLACE_EXISTING);

            showInfo("Deploy Report",
                    "Copied report into repo:\n" + destFile);

        } catch (IOException e) {
            showError("Deploy Report",
                    "Copy failed",
                    e.getMessage());
        }
    }

    private void deployAndPublishCurrentReport() {
        autosaveCurrentReport();

        if (currentReport == null) {
            showInfo("Publish", "No report selected.");
            return;
        }
        if (reportRepoIndex == null) {
            showError("Publish", "Missing mapping", "No ReportRepoIndex provided.");
            return;
        }

        var repoOpt = reportRepoIndex.findRepoForReport(currentReport);
        if (repoOpt.isEmpty()) {
            showError("Publish", "Missing mapping",
                    "No repository mapping exists for:\n" + currentReport);
            return;
        }

        Path repoRoot = repoOpt.get();
        Path destDir = repoRoot.resolve("feedback");
        Path destFile = destDir.resolve(currentReport.getFileName());

        // Run publish off UI thread
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Files.createDirectories(destDir);
                Files.copy(currentReport, destFile, StandardCopyOption.REPLACE_EXISTING);

                // Mark touched
                reportRepoIndex.markRepoTouched(repoRoot);

                // Publish to remote
                Utilities.publishFeedbackReport(repoRoot, destFile);

                return null;
            }
        };

        task.setOnSucceeded(e ->
                showInfo("Publish", "Report deployed + pushed successfully.")
        );
        task.setOnFailed(e ->
                showError("Publish", "Failed", task.getException().getMessage())
        );

        Thread t = new Thread(task, "Publish-Feedback");
        t.setDaemon(true);
        t.start();
    }

    private void publishAllTouchedRepos() {
        if (reportRepoIndex == null) {
            showError("Publish All", "Missing mapping", "No ReportRepoIndex provided.");
            return;
        }

        var repos = reportRepoIndex.touchedReposSnapshot();
        if (repos.isEmpty()) {
            showInfo("Publish All", "No repositories have deployed reports yet.");
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (Path repo : repos) {
                    // publish all reports in repo/feedback
                    Path feedbackDir = repo.resolve("feedback");
                    if (!Files.isDirectory(feedbackDir)) {
                        continue;
                    }
                    try (var stream = Files.list(feedbackDir)) {
                        for (Path f : stream.toList()) {
                            if (Files.isRegularFile(f) && f.toString().endsWith(".html")) {
                                Utilities.publishFeedbackReport(repo, f);
                            }
                        }
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e ->
                showInfo("Publish All", "Pulled + pushed feedback for all touched repositories.")
        );
        task.setOnFailed(e ->
                showError("Publish All", "Failed", task.getException().getMessage())
        );

        Thread t = new Thread(task, "Publish-All-Feedback");
        t.setDaemon(true);
        t.start();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.initOwner(editor.getScene().getWindow());
        a.showAndWait();
    }

    private void showError(String title, String header, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(msg);
        a.initOwner(editor.getScene().getWindow());
        a.showAndWait();
    }

    private void deployAllReportsToRepos() {
        autosaveCurrentReport();

        if (reportRepoIndex == null) {
            showError("Deploy All Reports",
                    "Missing report mapping",
                    "No ReportRepoIndex was provided to GradingController.");
            return;
        }

        int copied = 0;
        int missing = 0;

        for (Path report : reports) {
            Optional<Path> repoOpt = reportRepoIndex.findRepoForReport(report);
            if (repoOpt.isEmpty()) {
                missing++;
                continue;
            }
            Path repoRoot = repoOpt.get();
            Path destDir = repoRoot.resolve("feedback");
            Path destFile = destDir.resolve(report.getFileName());
            try {
                Files.createDirectories(destDir);
                Files.copy(report, destFile, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException ignored) {
                // ignore or count failures
            }
        }

        showInfo("Deploy All Reports",
                "Copied: " + copied + "\nMissing mapping: " + missing);
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
