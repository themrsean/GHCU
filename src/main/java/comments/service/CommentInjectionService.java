/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package comments.service;

import comments.domain.CommentTemplate;
import comments.domain.InjectedComment;
import grading.model.ReportState;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service responsible for inserting and tracking injected comments in a grading report.
 * <p>
 * This service supports:
 * <ul>
 *     <li>Injecting rendered template text into an editor</li>
 *     <li>Removing previously injected text</li>
 *     <li>Maintaining injected comment offsets as the user edits the document</li>
 * </ul>
 * <p>
 * Offset tracking is based on the {@link PlainTextChange} events produced by RichTextFX.
 *
 * @author Sean Jones
 */
public final class CommentInjectionService {

    /**
     * Inserts the rendered template text at the editor caret position and records
     * the injected range in the {@link ReportState}.
     *
     * @param template template to render and inject
     * @param editor editor to modify
     * @param reportState report state to update
     * @return the {@link InjectedComment} representing the injected range
     * @throws NullPointerException if any argument is {@code null}
     */
    public InjectedComment inject(
            CommentTemplate template,
            CodeArea editor,
            ReportState reportState
    ) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(editor, "editor");
        Objects.requireNonNull(reportState, "reportState");

        int insertPos = editor.getCaretPosition();
        String renderedText = render(template);

        editor.insertText(insertPos, renderedText);

        InjectedComment injected = InjectedComment.create(
                template.id(),
                insertPos,
                insertPos + renderedText.length(),
                renderedText
        );

        reportState.addInjectedComment(injected);
        return injected;
    }

    /**
     * Removes an injected comment by deleting its text range from the editor and removing
     * the comment from {@link ReportState}.
     *
     * @param comment injected comment to remove
     * @param editor editor to modify
     * @param reportState report state to update
     * @throws NullPointerException if any argument is {@code null}
     */
    public void remove(
            InjectedComment comment,
            CodeArea editor,
            ReportState reportState
    ) {
        Objects.requireNonNull(comment, "comment");
        Objects.requireNonNull(editor, "editor");
        Objects.requireNonNull(reportState, "reportState");

        editor.replaceText(
                comment.startOffset(),
                comment.endOffset(),
                ""
        );

        reportState.removeInjectedComment(comment);
    }

    /**
     * Updates all injected comment offsets in response to a text change.
     * <p>
     * This should be called whenever the editor produces a {@link PlainTextChange}.
     * The injected comment list is rewritten with updated ranges. Any injected comment
     * that is fully deleted by the edit is removed from the state.
     *
     * @param change RichTextFX text change event
     * @param reportState report state to update
     * @throws NullPointerException if any argument is {@code null}
     */
    public void onTextChanged(
            PlainTextChange change,
            ReportState reportState
    ) {
        Objects.requireNonNull(change, "change");
        Objects.requireNonNull(reportState, "reportState");

        int changeStart = change.getPosition();
        int removedLength = change.getRemoved().length();
        int insertedLength = change.getInserted().length();

        // Removed range is [changeStart, changeEnd)
        int changeEnd = changeStart + removedLength;

        int delta = insertedLength - removedLength;

        // If the change is a no-op OR there are no injected comments, do nothing.
        if (removedLength != 0 || insertedLength != 0) {
            List<InjectedComment> current = reportState.getInjectedComments();
            if (!current.isEmpty()) {
                List<InjectedComment> updated = new ArrayList<>(current.size());
                for (InjectedComment c : current) {
                    InjectedComment next = updateOffsets(c, changeStart, changeEnd, delta);
                    if (next != null) {
                        updated.add(next);
                    }
                }
                reportState.replaceInjectedComments(updated);
            }
        }
    }


    /* ------------------------------------------------------------
       Offset Logic
       ------------------------------------------------------------ */

    /**
     * Updates an injected comment range based on a text edit.
     *
     * @param comment injected comment to update
     * @param changeStart start offset of the change (inclusive)
     * @param changeEnd end offset of the removed range (exclusive)
     * @param delta net length change (insertedLength - removedLength)
     * @return updated comment, or {@code null} if the change deletes the comment entirely
     */
    private InjectedComment updateOffsets(
            InjectedComment comment,
            int changeStart,
            int changeEnd,
            int delta
    ) {
        int start = comment.startOffset();
        int end = comment.endOffset();

        boolean insertionOnly = changeStart == changeEnd;

        InjectedComment result;

        // Case 1: edit entirely before comment -> shift whole comment
        if (changeEnd <= start) {
            result = comment.shift(delta);

            // Case 2: edit entirely after comment -> no-op
        } else if (changeStart >= end) {
            result = comment;

            // Case 3: overlap
        } else if (insertionOnly) {
            // insertion inside [start,end) expands end
            result = comment.expand(delta);

        } else if (changeStart <= start && changeEnd >= end) {
            // Entire comment deleted
            result = null;

        } else {
            // Deletion/replacement overlap: compute overlap length inside the comment
            int overlapStart = Math.max(start, changeStart);
            int overlapEnd = Math.min(end, changeEnd);
            int overlapLength = Math.max(0, overlapEnd - overlapStart);

            int newStart = start;
            int newEnd;

            // If deletion overlaps the start boundary, the start becomes changeStart
            if (changeStart < start) {
                newStart = changeStart;
            }

            // Net delta shifts end
            newEnd = end + delta;

            // If deletion removed content inside the comment, shrink end accordingly
            if (overlapLength > 0) {
                newEnd = newEnd - overlapLength;
            }

            // Clamp to valid range
            newStart = Math.max(0, newStart);
            newEnd = Math.max(newStart, newEnd);

            // Drop collapsed comments
            if (newEnd == newStart) {
                result = null;
            } else {
                result = new InjectedComment(
                        comment.id(),
                        comment.templateId(),
                        newStart,
                        newEnd,
                        comment.renderedText()
                );
            }
        }

        return result;
    }




    /* ------------------------------------------------------------
       Rendering
       ------------------------------------------------------------ */

    /**
     * Renders a template into concrete source text.
     * <p>
     * This implementation is intentionally deterministic and simple.
     * Future extension point: language-aware rendering or placeholder expansion.
     *
     * @param template template to render
     * @return rendered template body text
     * @throws NullPointerException if {@code template} is {@code null}
     */
    private String render(CommentTemplate template) {
        Objects.requireNonNull(template, "template");
        return template.body();
    }
}
