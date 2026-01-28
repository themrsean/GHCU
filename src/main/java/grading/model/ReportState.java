/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
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

public final class ReportState {
    /* ---------- Undo / Redo ---------- */
    private static final int MAX_HISTORY = 100;
    @JsonIgnore private final Deque<String> undoStack;
    @JsonIgnore private final Deque<String> redoStack;
    /* ---------- Document State ---------- */
    private String text;
    private boolean dirty;
    private int caretPosition;
    private double scrollLocation;
    private List<InjectedComment> injectedComments;

    public ReportState() {
        this.dirty = false;
        this.caretPosition = 0;
        this.scrollLocation = 0.0;
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
        injectedComments = new ArrayList<>();
        this.text = "";
    }

    public ReportState(String initialText) {
        this();
        this.text = initialText;
    }

    @JsonCreator
    public ReportState(
            @JsonProperty("text") String text,
            @JsonProperty("dirty") boolean dirty,
            @JsonProperty("caretPosition") int caretPosition,
            @JsonProperty("scrollLocation") double scrollLocation,
            @JsonProperty("injectedComments") List<InjectedComment> injectedComments
    ) {
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
        this.text = Objects.requireNonNullElse(text, "");
        this.dirty = dirty;
        this.caretPosition = Math.max(0, caretPosition);
        this.scrollLocation = scrollLocation;
        this.injectedComments = List.copyOf(
                injectedComments == null ? List.of() : injectedComments
        );
    }

    public void reset(String text) {
        this.text = text;
        undoStack.clear();
        redoStack.clear();
        dirty = false;
    }

    public void recordEdit(String newText) {
        if (!newText.equals(text)) {
            pushUndo(text);
            text = newText;
            dirty = true;
            redoStack.clear();
        }
    }

    public void updateCaret(int caretPosition) {
        this.caretPosition = caretPosition;
    }

    public void updateScroll(double scrollLocation) {
        this.scrollLocation = scrollLocation;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public String undo() {
        if (!canUndo()) {
            return text;
        }
        redoStack.push(text);
        text = undoStack.pop();
        dirty = true;
        return text;
    }

    public String redo() {
        if (!canRedo()) {
            return text;
        }
        undoStack.push(text);
        text = redoStack.pop();
        dirty = true;
        return text;
    }

    private void pushUndo(String snapshot) {
        if (undoStack.size() >= MAX_HISTORY) {
            undoStack.removeLast();
        }
        undoStack.push(snapshot);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void markClean() {
        dirty = false;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getCaretPosition() {
        return caretPosition;
    }

    public void setCaretPosition(int caretPosition) {
        this.caretPosition = Math.max(0, caretPosition);
    }

    public double getScrollLocation() {
        return scrollLocation;
    }

    public void setScrollLocation(double scrollLocation) {
        this.scrollLocation = scrollLocation;
    }

    public List<InjectedComment> getInjectedComments() {
        return injectedComments;
    }

    public void addInjectedComment(InjectedComment comment) {
        Objects.requireNonNull(comment, "comment");
        List<InjectedComment> next = new ArrayList<>(injectedComments);
        next.add(comment);
        injectedComments = List.copyOf(next);
    }

    public void removeInjectedComment(InjectedComment comment) {
        Objects.requireNonNull(comment, "comment");
        List<InjectedComment> next = new ArrayList<>(injectedComments);
        next.remove(comment);
        injectedComments = List.copyOf(next);
    }

    public void replaceInjectedComments(List<InjectedComment> comments) {
        Objects.requireNonNull(comments, "comments");
        injectedComments = List.copyOf(comments);
    }
}
