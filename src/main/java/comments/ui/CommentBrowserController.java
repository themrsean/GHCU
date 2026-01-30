/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package comments.ui;

import comments.domain.CommentTemplate;
import comments.persistence.CommentRepository;
import comments.service.CommentInjectionService;
import grading.model.ReportState;
import org.fxmisc.richtext.CodeArea;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Controller/service for browsing, filtering, and inserting reusable comment
 * templates into the currently active grading editor.
 * <p>
 * This class maintains filter state (assignment, rubric, tag, search query) and
 * provides operations to insert templates into the active report, as well as
 * repository operations (save/delete/import/export).
 * <p>
 * An active editor/report context must be provided via
 * {@link #setActiveContext(CodeArea, ReportState)} before calling
 * {@link #insertComment(CommentTemplate)}.
 *
 * @author Sean Jones
 */
public final class CommentBrowserController {
    /* ---------------- Dependencies ---------------- */
    private final CommentRepository repository;
    private final CommentInjectionService injector;

    /* ---------------- Active grading context ---------------- */
    private CodeArea activeEditor;
    private ReportState activeReport;

    /* ---------------- View state (filters) ---------------- */
    private String assignmentFilter;
    private String rubricFilter;
    private String tagFilter;
    private String searchQuery;

    /**
     * Constructs a controller backed by the given repository and injection service.
     *
     * @param repository comment template repository
     * @param injector service used to inject comments into the editor
     * @throws NullPointerException if any argument is null
     */
    public CommentBrowserController(
            CommentRepository repository,
            CommentInjectionService injector
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.injector = Objects.requireNonNull(injector, "injector");
        this.searchQuery = "";
    }

    /* ------------------------------------------------------------
       Context wiring (called by GradingController)
       ------------------------------------------------------------ */

    /**
     * Sets the active editor/report context that comment insertion operates on.
     *
     * @param editor active editor
     * @param reportState active report state
     * @throws NullPointerException if any argument is null
     */
    public void setActiveContext(CodeArea editor, ReportState reportState) {
        this.activeEditor = Objects.requireNonNull(editor, "editor");
        this.activeReport = Objects.requireNonNull(reportState, "reportState");
    }

    /**
     * Clears the active editor/report context.
     * <p>
     * After calling this method, {@link #insertComment(CommentTemplate)} will
     * throw an {@link IllegalStateException} until a new context is set.
     */
    public void clearActiveContext() {
        activeEditor = null;
        activeReport = null;
    }

    /* ------------------------------------------------------------
       Query / filtering
       ------------------------------------------------------------ */

    /**
     * Returns the list of templates visible under the current filter settings.
     * <p>
     * Filters are applied in the following order:
     * assignment, rubric, tag, search query.
     *
     * @return list of visible templates (can be empty)
     */
    public List<CommentTemplate> getVisibleComments() {
        List<CommentTemplate> all = repository.loadAll();

        String assignment = assignmentFilter;
        String rubric = rubricFilter;
        String tag = tagFilter;

        String q = searchQuery == null ? "" : searchQuery.trim().toLowerCase();

        return all.stream()
                .filter(c -> assignment == null || c.assignmentId().equals(assignment))
                .filter(c -> rubric == null || c.rubricId().equals(rubric))
                .filter(c -> tag == null || c.tags().contains(tag))
                .filter(c -> q.isBlank()
                        || c.title().toLowerCase().contains(q)
                        || c.body().toLowerCase().contains(q))
                .toList();
    }

    /* ------------------------------------------------------------
       Filter setters
       ------------------------------------------------------------ */

    /**
     * Sets the assignment filter.
     *
     * @param assignmentId assignment id to filter by; if null/blank, clears filter
     */
    public void setAssignmentFilter(String assignmentId) {
        assignmentFilter = normalizeFilter(assignmentId);
    }

    /**
     * Sets the rubric filter.
     *
     * @param rubricId rubric id to filter by; if null/blank, clears filter
     */
    public void setRubricFilter(String rubricId) {
        rubricFilter = normalizeFilter(rubricId);
    }

    /**
     * Sets the tag filter.
     *
     * @param tag tag to filter by; if null/blank, clears filter
     */
    public void setTagFilter(String tag) {
        tagFilter = normalizeFilter(tag);
    }

    /**
     * Sets the free-text search query used for filtering.
     *
     * @param query query text; if null, becomes empty string
     */
    public void setSearchQuery(String query) {
        searchQuery = query == null ? "" : query;
    }

    /**
     * Clears all filters and resets the search query.
     */
    public void clearFilters() {
        assignmentFilter = null;
        rubricFilter = null;
        tagFilter = null;
        searchQuery = "";
    }

    /* ------------------------------------------------------------
       Actions
       ------------------------------------------------------------ */

    /**
     * Inserts the given template into the active editor at the caret position.
     *
     * @param template template to insert
     * @throws NullPointerException if template is null
     * @throws IllegalStateException if no active context has been set
     */
    public void insertComment(CommentTemplate template) {
        Objects.requireNonNull(template, "template");
        requireActiveContext();

        injector.inject(template, activeEditor, activeReport);

        // Editor was modified directly; ensure state reflects dirty status.
        activeReport.markDirty();
    }

    /**
     * Saves the given template into the repository.
     *
     * @param template template to save
     * @throws NullPointerException if template is null
     */
    public void saveComment(CommentTemplate template) {
        Objects.requireNonNull(template, "template");
        repository.save(template);
    }

    /**
     * Deletes the template with the given id from the repository.
     *
     * @param templateId template id
     * @throws NullPointerException if templateId is null
     */
    public void deleteComment(UUID templateId) {
        Objects.requireNonNull(templateId, "templateId");
        repository.delete(templateId);
    }

    /**
     * Imports templates from the given path.
     *
     * @param path import file path
     * @throws NullPointerException if path is null
     */
    public void importComments(Path path) {
        Objects.requireNonNull(path, "path");
        repository.importFrom(path);
    }

    /**
     * Exports all templates to the given path.
     *
     * @param path export file path
     * @throws NullPointerException if path is null
     */
    public void exportComments(Path path) {
        Objects.requireNonNull(path, "path");
        repository.exportTo(path);
    }

    /* ------------------------------------------------------------
       Validation / helpers
       ------------------------------------------------------------ */

    private void requireActiveContext() {
        if (activeEditor == null || activeReport == null) {
            throw new IllegalStateException(
                    "Active editor and report must be set before inserting comments"
            );
        }
    }

    private static String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
