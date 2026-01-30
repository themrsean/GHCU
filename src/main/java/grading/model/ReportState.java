/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package grading.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import comments.domain.InjectedComment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Stores editor state for a single feedback report being graded.
 * <p>
 * This model tracks the current report text, undo/redo history, caret position,
 * scroll location, and comment injection metadata. It is designed to support
 * interactive editing in the grading UI.
 * <p>
 * Undo/redo stacks are transient runtime state and are not persisted during JSON
 * serialization.
 * @author Sean Jones
 */
public final class ReportState {
    /* Undo / Redo */
    private static final int MAX_HISTORY = 100;
    @JsonIgnore private final Deque<Snapshot> undoStack;
    @JsonIgnore private final Deque<Snapshot> redoStack;

    /*  Document State */
    private String text;
    @JsonIgnore private boolean dirty;
    private int caretPosition;
    private double scrollLocation;
    private List<InjectedComment> injectedComments;

    /**
     * Constructs a new {@code ReportState} with default values.
     * <p>
     * The report starts with empty text, a clean (not dirty) state, caret at 0,
     * scroll location 0.0, and empty undo/redo history.
     */
    public ReportState() {
        this.dirty = false;
        this.caretPosition = 0;
        this.scrollLocation = 0.0;
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
        injectedComments = new ArrayList<>();
        this.text = "";
    }

    /**
     * Constructs a new {@code ReportState} initialized with the given text.
     * <p>
     * The initial text becomes the current report text. Undo/redo history starts empty.
     *
     * @param initialText initial report text (can be {@code null}, treated as empty)
     */
    public ReportState(String initialText) {
        this();
        this.text = initialText;
    }

    /**
     * Constructs a new {@code ReportState} using explicit values (used primarily for JSON loading).
     * <p>
     * Undo/redo stacks are initialized empty because they are transient runtime state.
     *
     * @param text the current report text (can be {@code null}, treated as empty)
     * @param caretPosition caret position in the document (negative values become 0)
     * @param scrollLocation scroll position in the editor
     * @param injectedComments list of injected comments (can be {@code null}, treated as empty)
     */
    @JsonCreator public ReportState(
            @JsonProperty("text") String text,
            @JsonProperty("caretPosition") int caretPosition,
            @JsonProperty("scrollLocation") double scrollLocation,
            @JsonProperty("injectedComments") List<InjectedComment> injectedComments
    ) {
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
        this.text = Objects.requireNonNullElse(text, "");
        this.caretPosition = Math.max(0, caretPosition);
        this.scrollLocation = scrollLocation;
        this.injectedComments = List.copyOf(
                injectedComments == null ? List.of() : injectedComments
        );
    }

    /**
     * Resets the report state to a clean baseline.
     * <p>
     * This clears undo/redo history, marks the state as not dirty, resets caret and
     * scroll position, and replaces the current text.
     *
     * @param text the new report text (can be {@code null}, treated as empty)
     */
    public void reset(String text) {
        setText(text);
        undoStack.clear();
        redoStack.clear();
        dirty = false;
        caretPosition = 0;
        scrollLocation = 0.0;
    }

    /**
     * Records a text edit and updates undo/redo history.
     * <p>
     * If the new text differs from the current text, the current state is pushed onto
     * the undo stack, redo history is cleared, and the report is marked dirty.
     *
     * @param newText the new document text (can be {@code null}, treated as empty)
     */
    public void recordEdit(String newText) {
        String next = Objects.requireNonNullElse(newText, "");
        if (!next.equals(text)) {
            pushUndoSnapshot();
            text = next;
            dirty = true;
            redoStack.clear();
        }
    }

    /**
     * Updates the caret position stored in this report state.
     *
     * @param caretPosition caret position in the editor (negative values become 0)
     */
    public void updateCaret(int caretPosition) {
        this.caretPosition = Math.max(0, caretPosition);
    }

    /**
     * Updates the scroll position stored in this report state.
     *
     * @param scrollLocation scroll position of the editor view
     */
    public void updateScroll(double scrollLocation) {
        this.scrollLocation = scrollLocation;
    }

    /**
     * Returns whether an undo operation is currently available.
     *
     * @return {@code true} if there is at least one undo snapshot; {@code false} otherwise
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Returns whether a redo operation is currently available.
     *
     * @return {@code true} if there is at least one redo snapshot; {@code false} otherwise
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Undoes the most recent edit, if possible.
     * <p>
     * The current state is pushed to the redo stack and the most recent undo snapshot
     * becomes the active state.
     *
     * @return the updated report text after undo (or unchanged text if undo is unavailable)
     */
    public String undo() {
        if (!canUndo()) {
            return text;
        }
        redoStack.push(new Snapshot(text, caretPosition, scrollLocation, dirty));
        Snapshot prev = undoStack.pop();
        restore(prev);
        return text;
    }

    /**
     * Redoes the most recently undone edit, if possible.
     * <p>
     * The current state is pushed to the undo stack and the most recent redo snapshot
     * becomes the active state.
     *
     * @return the updated report text after redo (or unchanged text if redo is unavailable)
     */
    public String redo() {
        if (!canRedo()) {
            return text;
        }
        undoStack.push(new Snapshot(text, caretPosition, scrollLocation, dirty));
        Snapshot next = redoStack.pop();
        restore(next);
        return text;
    }

    private void restore(Snapshot snapshot) {
        text = snapshot.text();
        caretPosition = snapshot.caretPosition();
        scrollLocation = snapshot.scrollLocation();
        dirty = snapshot.dirty();
    }

    private void pushUndoSnapshot() {
        if (undoStack.size() >= MAX_HISTORY) {
            undoStack.removeLast();
        }
        undoStack.push(new Snapshot(text, caretPosition, scrollLocation, dirty));
    }

    /**
     * Returns whether the report state has unsaved changes.
     *
     * @return {@code true} if the report has been modified since the last save/clean mark
     */

    public boolean isDirty() {
        return dirty;
    }

    /**
     * Marks this report state as having unsaved changes.
     */
    public void markDirty() {
        dirty = true;
    }

    /**
     * Marks this report state as clean (no unsaved changes).
     */
    public void markClean() {
        dirty = false;
    }

    /**
     * Returns the current report text.
     *
     * @return the current text (never {@code null})
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the report text without affecting undo/redo history.
     * <p>
     * This method does not automatically mark the report dirty; callers should decide
     * whether to call {@link #markDirty()}.
     *
     * @param text new report text (can be {@code null}, treated as empty)
     */
    public void setText(String text) {
        this.text = Objects.requireNonNullElse(text, "");
    }

    /**
     * Returns the caret position stored for this report.
     *
     * @return caret position (0 or greater)
     */
    public int getCaretPosition() {
        return caretPosition;
    }

    /**
     * Sets the caret position stored for this report.
     *
     * @param caretPosition caret position (negative values become 0)
     */
    public void setCaretPosition(int caretPosition) {
        this.caretPosition = Math.max(0, caretPosition);
    }

    /**
     * Returns the stored scroll location for this report.
     *
     * @return scroll location value
     */
    public double getScrollLocation() {
        return scrollLocation;
    }

    /**
     * Sets the stored scroll location for this report.
     *
     * @param scrollLocation scroll location value
     */
    public void setScrollLocation(double scrollLocation) {
        this.scrollLocation = scrollLocation;
    }

    /**
     * Returns the list of injected comments associated with this report.
     * <p>
     * The returned list is immutable.
     *
     * @return immutable list of injected comments (never {@code null})
     */
    public List<InjectedComment> getInjectedComments() {
        return injectedComments;
    }

    /**
     * Adds an injected comment record to this report.
     *
     * @param comment injected comment to add
     * @throws NullPointerException if {@code comment} is {@code null}
     */
    public void addInjectedComment(InjectedComment comment) {
        Objects.requireNonNull(comment, "comment");
        List<InjectedComment> next = new ArrayList<>(injectedComments);
        next.add(comment);
        injectedComments = List.copyOf(next);
    }

    /**
     * Removes an injected comment record from this report.
     * <p>
     * If the comment is not present, no change occurs.
     *
     * @param comment injected comment to remove
     * @throws NullPointerException if {@code comment} is {@code null}
     */
    public void removeInjectedComment(InjectedComment comment) {
        Objects.requireNonNull(comment, "comment");
        List<InjectedComment> next = new ArrayList<>(injectedComments);
        next.remove(comment);
        injectedComments = List.copyOf(next);
    }

    /**
     * Replaces the injected comment list with the provided list.
     * <p>
     * The stored list becomes immutable.
     *
     * @param comments new injected comment list
     * @throws NullPointerException if {@code comments} is {@code null}
     */
    public void replaceInjectedComments(List<InjectedComment> comments) {
        Objects.requireNonNull(comments, "comments");
        injectedComments = List.copyOf(comments);
    }

    private record Snapshot(String text, int caretPosition, double scrollLocation, boolean dirty) {
        private Snapshot(String text, int caretPosition, double scrollLocation, boolean dirty) {
            this.text = Objects.requireNonNullElse(text, "");
            this.caretPosition = Math.max(0, caretPosition);
            this.scrollLocation = scrollLocation;
            this.dirty = dirty;
        }
    }
}
