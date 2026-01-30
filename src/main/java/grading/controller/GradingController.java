/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

/**
 * JavaFX controller for the grading window.
 * <p>
 * This controller provides an interactive editor for generated HTML feedback reports.
 * It supports report navigation, syntax highlighting, find/replace operations, and
 * persistent UI state such as sidebar width, font size, scroll position, and the
 * last opened report.
 * <p>
 * The controller also integrates a comment system that allows graders to browse,
 * inject, and manage reusable feedback comments. In addition, it supports deploying
 * feedback reports into student repositories and publishing them to GitHub using
 * the {@link mainui.service.Utilities} git helper methods.
 * <p>
 * This class is designed to be loaded via FXML and therefore should not be
 * instantiated directly.
 *
 * @author Sean Jones
 */
public class GradingController implements Initializable {
    /* Constants */
    private static final double FONT_STEP = 1.0;
    private static final double MIN_FONT_SIZE = 8.0;
    private static final double MAX_FONT_SIZE = 32.0;
    private static final double DEFAULT_FONT_SIZE = 12.0;
    private static final double DEFAULT_SIDEBAR_WIDTH = 125.0;
    private static final Duration AUTOSAVE_DELAY = Duration.millis(500);

    /* Application State */
    private final ObservableList<Path> reports =
            FXCollections.observableArrayList();
    private final Map<Path, ReportState> stateMap = new HashMap<>();
    private ReportRepoIndex reportRepoIndex;
    private Path currentReport;

    /* UI / Persistence */
    private final UIState uiState =
            new UIState(GradingController.class);
    private final PauseTransition autosaveTimer =
            new PauseTransition(AUTOSAVE_DELAY);

    /* Comment system */
    private CommentRepository commentRepository;
    private CommentInjectionService commentInjectionService;

    /* Editor UI state */
    private VirtualizedScrollPane<CodeArea> editorScroll;
    private CodeArea editor;
    private FindBarController findBarController;

    /* Task/editor runtime state */
    private double fontSize = DEFAULT_FONT_SIZE;
    private boolean programmaticEdit = false;

    /* Find/replace state */
    private String lastSearchText = "";
    private int totalMatches = 0;
    private int currentMatchNumber = 0;

    /* FXML Controls */
    @SuppressWarnings("rawtypes")
    @FXML private ListView reportListView;
    @FXML private SplitPane splitPane;
    @FXML private VBox editorContainer;

    /* UI wiring and configuration */
    /**
     * Initializes the grading UI controller after its FXML fields have been injected.
     * <p>
     * This method wires up the report list, editor, autosave behavior, split-pane
     * persistence, find/replace bar, font settings, and comment system integration.
     *
     * @param url the location used to resolve relative paths for the root object,
     *            or {@code null} if not known
     * @param resourceBundle the resources used to localize the root object,
     *                       or {@code null} if not localized
     */
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

    /* Public API (called from MainController / Grading window) */
    /**
     * Injects the report-to-repository mapping used for deploying and publishing
     * feedback reports back into student repositories.
     * <p>
     * This mapping is typically constructed by the main UI pipeline when reports
     * are generated, and must be provided before publish/deploy features can work.
     *
     * @param reportRepoIndex mapping that associates report file paths with the
     *                        corresponding repository root directory
     * @throws NullPointerException if {@code reportRepoIndex} is {@code null}
     */
    public void setReportRepoIndex(ReportRepoIndex reportRepoIndex) {
        Objects.requireNonNull(reportRepoIndex, "reportRepoIndex cannot be null");
        this.reportRepoIndex = reportRepoIndex;
    }


    /**
     * Loads all HTML report files from the given folder into the report list view.
     * <p>
     * Reports are filtered to files ending in {@code .html} and sorted by filename.
     * If the UI has a previously opened report saved in persistent state, this
     * method attempts to restore it; otherwise the first report is selected.
     *
     * @param folder directory containing generated feedback reports
     * @throws IOException if the folder cannot be read
     */
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

    /**
     * Installs keyboard accelerators (shortcuts) for common grading operations.
     * <p>
     * This binds actions such as undo/redo, switching reports, font resizing,
     * saving, find/replace, publishing feedback, and opening the comment browser
     * to key combinations on the provided {@link Scene}.
     *
     * @param scene the scene to attach accelerators to
     * @throws NullPointerException if {@code scene} is {@code null}
     */
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
                this::publishCurrentReport
        );
        accelerators.put(
                new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN,
                        KeyCombination.SHIFT_DOWN),
                this::publishAllTouchedRepos
        );
        accelerators.put(
                new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN),
                this::deployCurrentReportOnly
        );
        accelerators.put(
                new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN,
                        KeyCombination.SHIFT_DOWN),
                this::deployAllReportsOnly
        );
    }

    /**
     * Performs cleanup and persistence actions when the grading window is closing.
     * <p>
     * This stops autosave, saves the current report state, and flushes all dirty
     * reports to disk to avoid losing edits.
     */
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

    /**
     * Opens the comment browser window for the currently selected report.
     * <p>
     * The comment browser is connected to the existing editor instance and the
     * {@link ReportState} for the current report so that comment insertion and
     * text injection operate on the active grading context.
     * <p>
     * If no report is currently selected, this method does nothing.
     *
     * @throws RuntimeException if the comment browser FXML cannot be loaded
     */
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

    /* Editor/report operations */
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
        if (currentReport != null && state != null && state.canUndo()) {
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

    /* Deploy/publish */
    private void deployCurrentReportOnly() {
        autosaveCurrentReport();
        if (currentReport == null) {
            showInfo("Deploy Report", "No report selected.");
        } else if (reportRepoIndex == null) {
            showError("Deploy Report",
                    "Missing report mapping",
                    "No ReportRepoIndex was provided to GradingController.");
        } else {
            Optional<Path> repoOpt = reportRepoIndex.findRepoForReport(currentReport);
            if (repoOpt.isEmpty()) {
                showError("Deploy Report",
                        "Repository not found for report",
                        "No repository mapping exists for:\n" + currentReport);
            } else {
                Path repoRoot = repoOpt.get();
                Path destDir = repoRoot.resolve("feedback");
                Path destFile = destDir.resolve(currentReport.getFileName());
                try {
                    Files.createDirectories(destDir);
                    boolean shouldCopy;
                    if (Files.exists(destFile)) {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Overwrite feedback?");
                        confirm.setHeaderText("Feedback report already exists in repository.");
                        confirm.setContentText(destFile.toString());
                        Optional<ButtonType> result = confirm.showAndWait();
                        shouldCopy = result.isPresent() && result.get() == ButtonType.OK;
                    } else {
                        shouldCopy = true;
                    }
                    if (shouldCopy) {
                        Files.copy(currentReport, destFile, StandardCopyOption.REPLACE_EXISTING);
                        showInfo("Deploy Report",
                                "Copied report into repo:\n" + destFile);
                    } else {
                        showInfo("Deploy Report", "Deploy cancelled.");
                    }
                } catch (IOException e) {
                    showError("Deploy Report",
                            "Copy failed",
                            e.getMessage());
                }
            }
        }
    }

    private void publishCurrentReport() {
        autosaveCurrentReport();
        if (currentReport == null) {
            showInfo("Publish", "No report selected.");
        } else if (reportRepoIndex == null) {
            showError("Publish", "Missing mapping", "No ReportRepoIndex provided.");
        } else {
            Optional<Path> repoOpt = reportRepoIndex.findRepoForReport(currentReport);
            if (repoOpt.isEmpty()) {
                showError("Publish", "Missing mapping",
                        "No repository mapping exists for:\n" + currentReport);
            } else {
                Path report = currentReport;
                Path repoRoot = repoOpt.get();
                Path destDir = repoRoot.resolve("feedback");
                Thread t = createPublishThread(report, destDir, repoRoot);
                t.start();
            }
        }
    }

    private Thread createPublishThread(Path report, Path destDir, Path repoRoot) {
        Path destFile = destDir.resolve(report.getFileName());
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Files.createDirectories(destDir);
                Files.copy(report, destFile, StandardCopyOption.REPLACE_EXISTING);
                reportRepoIndex.markRepoTouched(repoRoot);
                Utilities.publishFeedbackReport(repoRoot, destFile);
                return null;
            }
        };
        task.setOnSucceeded(_ ->
                showInfo("Publish", "Report deployed + pushed successfully.")
        );
        task.setOnFailed(_ ->
                showError("Publish", "Failed", task.getException().getMessage())
        );
        Thread t = new Thread(task, "Publish-Feedback");
        t.setDaemon(true);
        return t;
    }

    private void deployAllReportsOnly() {
        autosaveCurrentReport();
        if (reportRepoIndex == null) {
            showError("Deploy All Reports",
                    "Missing report mapping",
                    "No ReportRepoIndex was provided to GradingController.");
        } else {
            int copied = 0;
            int missing = 0;
            int failed = 0;
            for (Path report : reports) {
                Optional<Path> repoOpt = reportRepoIndex.findRepoForReport(report);
                if (repoOpt.isEmpty()) {
                    ++missing;
                } else {
                    Path repoRoot = repoOpt.get();
                    Path destDir = repoRoot.resolve("feedback");
                    Path destFile = destDir.resolve(report.getFileName());
                    try {
                        Files.createDirectories(destDir);
                        Files.copy(report, destFile, StandardCopyOption.REPLACE_EXISTING);
                        ++copied;
                    } catch (IOException e) {
                        ++failed;
                        System.err.println("Failed to deploy report: " + report);
                        System.err.println("Destination: " + destFile);
                        System.err.println("Reason: " + e.getMessage());
                    }
                }
            }
            showInfo("Deploy All Reports",
                    "Copied: " + copied
                            + "\nMissing mapping: " + missing
                            + "\nFailed copies: " + failed);
        }
    }

    private void publishAllTouchedRepos() {
        if (reportRepoIndex == null) {
            showError("Publish All", "Missing mapping", "No ReportRepoIndex provided.");
        } else {
            Set<Path> repos = reportRepoIndex.touchedReposSnapshot();
            if (repos.isEmpty()) {
                showInfo("Publish All", "No repositories have deployed reports yet.");
            } else {
                Thread t = createPublishAllThread(repos);
                t.setDaemon(true);
                t.start();
            }
        }
    }

    private Thread createPublishAllThread(Set<Path> repos) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                Set<Path> repoSnapshots = Set.copyOf(repos);
                PublishResult result = publishRepos(repoSnapshots);
                Platform.runLater(() ->
                        showInfo("Publish All",
                                "Published files: " + result.getPublished()
                                        + "\nFailures: " + result.getFailed())
                );
                return null;
            }
        };
        Thread t = new Thread(task, "Publish-All-Feedback");
        t.setDaemon(true);
        return t;
    }

    private PublishResult publishRepos(Set<Path> repos) {
        PublishResult result = new PublishResult();
        for (Path repo : repos) {
            publishRepo(repo, result);
        }
        return result;
    }

    private void publishRepo(Path repo, PublishResult result) {
        Path feedbackDir = repo.resolve("feedback");
        if (Files.isDirectory(feedbackDir)) {
            try (Stream<Path> stream = Files.list(feedbackDir)) {
                List<Path> files = stream.toList();
                for (Path file : files) {
                    publishFileIfHtml(repo, file, result);
                }
            } catch (IOException e) {
                result.incFailed();
                System.err.println("Failed to list feedback dir: " + feedbackDir);
                System.err.println("Reason: " + e.getMessage());
            }
        }
    }

    private void publishFileIfHtml(Path repo, Path file, PublishResult result) {
        boolean isHtml = Files.isRegularFile(file) && file.toString().endsWith(".html");
        if (isHtml) {
            try {
                Utilities.publishFeedbackReport(repo, file);
                result.incPublished();
            } catch (IOException | InterruptedException e) {
                result.incFailed();
                System.err.println("Publish failed: " + repo);
                System.err.println("File: " + file);
                System.err.println("Reason: " + e.getMessage());
            }
        }
    }

    /* Find/replace */
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

    private List<Integer> findAllMatches(String text, String query) {
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

    /* Dialog helpers*/
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

    private static final class PublishResult {
        private int published;
        private int failed;

        private void incPublished() {
            ++published;
        }

        private void incFailed() {
            ++failed;
        }

        private int getPublished() {
            return published;
        }

        private int getFailed() {
            return failed;
        }
    }

}