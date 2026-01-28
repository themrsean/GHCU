/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
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

public final class CommentBrowserController {
    private final CommentRepository repository;
    private final CommentInjectionService injector;

    private CodeArea activeEditor;
    private ReportState activeReport;

    // Current view state
    private String assignmentFilter;
    private String rubricFilter;
    private String tagFilter;
    private String searchQuery = "";

    public CommentBrowserController(
            CommentRepository repository,
            CommentInjectionService injector
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.injector = Objects.requireNonNull(injector);
    }

    /* ------------------------------------------------------------
       Context wiring (called by GradingController)
       ------------------------------------------------------------ */

    public void setActiveContext(CodeArea editor, ReportState reportState) {
        this.activeEditor = Objects.requireNonNull(editor);
        this.activeReport = Objects.requireNonNull(reportState);
    }

    /* ------------------------------------------------------------
       Query / filtering
       ------------------------------------------------------------ */

    public List<CommentTemplate> getVisibleComments() {
        List<CommentTemplate> results = repository.loadAll();

        if (assignmentFilter != null) {
            results = results.stream()
                    .filter(c -> c.getAssignmentId().equals(assignmentFilter))
                    .toList();
        }

        if (rubricFilter != null) {
            results = results.stream()
                    .filter(c -> c.getRubricId().equals(rubricFilter))
                    .toList();
        }

        if (tagFilter != null) {
            results = results.stream()
                    .filter(c -> c.getTags().contains(tagFilter))
                    .toList();
        }

        if (!searchQuery.isBlank()) {
            String q = searchQuery.toLowerCase();
            results = results.stream()
                    .filter(c ->
                            c.getTitle().toLowerCase().contains(q) ||
                                    c.getBody().toLowerCase().contains(q))
                    .toList();
        }

        return results;
    }

    /* ------------------------------------------------------------
       Filter setters
       ------------------------------------------------------------ */

    public void setAssignmentFilter(String assignmentId) {
        this.assignmentFilter = assignmentId;
    }

    public void setRubricFilter(String rubricId) {
        this.rubricFilter = rubricId;
    }

    public void setTagFilter(String tag) {
        this.tagFilter = tag;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query == null ? "" : query;
    }

    public void clearFilters() {
        assignmentFilter = null;
        rubricFilter = null;
        tagFilter = null;
        searchQuery = "";
    }

    /* ------------------------------------------------------------
       Actions
       ------------------------------------------------------------ */

    public void insertComment(CommentTemplate template) {
        requireActiveContext();
        injector.inject(template, activeEditor, activeReport);
        activeReport.markDirty();
    }

    public void saveComment(CommentTemplate template) {
        repository.save(template);
    }

    public void deleteComment(UUID templateId) {
        repository.delete(templateId);
    }

    public void importComments(Path path) {
        repository.importFrom(path);
    }

    public void exportComments(Path path) {
        repository.exportTo(path);
    }

    /* ------------------------------------------------------------
       Validation
       ------------------------------------------------------------ */

    private void requireActiveContext() {
        if (activeEditor == null || activeReport == null) {
            throw new IllegalStateException(
                    "Active editor and report must be set before inserting comments"
            );
        }
    }
}

