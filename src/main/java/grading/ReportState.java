/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package main.java.grading;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ReportState {
    private static final int MAX_HISTORY = 100;
    /* ---------- Document State ---------- */

    private String text = "";
    private boolean dirty = false;

    private int caretPosition = 0;
    private double scrollLocation = 0.0;

    /* ---------- Undo / Redo ---------- */

    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();

    /* ---------- Lifecycle ---------- */

    public ReportState() {}

    public ReportState(String initialText) {
        this.text = initialText;
    }

    /* ---------- State Updates ---------- */

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

    /* ---------- Undo / Redo ---------- */

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public String undo() {
        if (!canUndo()) return text;

        redoStack.push(text);
        text = undoStack.pop();
        dirty = true;
        return text;
    }

    public String redo() {
        if (!canRedo()) return text;

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

    /* ---------- Persistence Helpers ---------- */

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
}
