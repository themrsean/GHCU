/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
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

public final class CommentInjectionService {

    /**
     * Injects a comment at the current caret position.
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
                template.getId(),
                insertPos,
                insertPos + renderedText.length(),
                renderedText
        );

        reportState.addInjectedComment(injected);
        return injected;
    }

    /**
     * Removes an injected comment from the editor and report state.
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
                comment.getStartOffset(),
                comment.getEndOffset(),
                ""
        );

        reportState.removeInjectedComment(comment);
    }

    /**
     * Updates injected comment offsets in response to a text change.
     *
     * This must be called for *every* PlainTextChange emitted by the editor,
     * including undo/redo.
     */
    public void onTextChanged(
            PlainTextChange change,
            ReportState reportState
    ) {
        Objects.requireNonNull(change, "change");
        Objects.requireNonNull(reportState, "reportState");

        int position = change.getPosition();
        int removed = change.getRemoved().length();
        int inserted = change.getInserted().length();
        int delta = inserted - removed;

        if (delta == 0) {
            return;
        }

        List<InjectedComment> updated = new ArrayList<>();

        for (InjectedComment c : reportState.getInjectedComments()) {
            updated.add(updateOffsets(c, position, delta));
        }

        reportState.replaceInjectedComments(updated);
    }

    /* ------------------------------------------------------------
       Offset Logic
       ------------------------------------------------------------ */

    private InjectedComment updateOffsets(
            InjectedComment comment,
            int changePos,
            int delta
    ) {
        int start = comment.getStartOffset();
        int end = comment.getEndOffset();

        // Change before comment → shift entire range
        if (changePos < start) {
            return comment.shift(delta);
        }

        // Change inside comment → expand or contract end only
        if (changePos >= start && changePos < end) {
            return comment.expand(delta);
        }

        // Change after comment → no-op
        return comment;
    }

    /* ------------------------------------------------------------
       Rendering
       ------------------------------------------------------------ */

    /**
     * Renders a template into concrete source text.
     * This is intentionally simple and deterministic.
     */
    private String render(CommentTemplate template) {
        // Future extension point: language-aware rendering
        return template.getBody();
    }
}

