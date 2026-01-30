/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package comments.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single comment that has been injected into a feedback report.
 * <p>
 * An {@code InjectedComment} records the association between a rendered comment
 * (copied/expanded from a {@code CommentTemplate}) and its location within the
 * edited report text. The location is stored as a half-open offset range
 * {@code [startOffset, endOffset]} into the report document.
 * <p>
 * This record is designed for JSON persistence using Jackson.
 * <p>
 * Equality and hash code are based solely on {@link #id()} so that injected
 * comments can be uniquely tracked even if their offsets change over time.
 * @author Sean Jones
 * @param id unique identifier for this injected comment; must not be {@code null}
 * @param templateId identifier of the template this comment was injected from;
 *                   must not be {@code null}
 * @param startOffset starting offset in the report text (inclusive); must be {@code >= 0}
 * @param endOffset ending offset in the report text (exclusive); must be {@code >= startOffset}
 * @param renderedText the exact text that was inserted into the report; must not be {@code null}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InjectedComment(UUID id, UUID templateId, int startOffset, int endOffset,
                              String renderedText) {
    /**
     * Creates an {@code InjectedComment}.
     * <p>
     * This constructor is also used by Jackson during JSON deserialization.
     *
     * @param id unique identifier for this injected comment; must not be {@code null}
     * @param templateId identifier of the template this comment was injected from;
     *                   must not be {@code null}
     * @param startOffset starting offset in the report text (inclusive); must be {@code >= 0}
     * @param endOffset ending offset in the report text (exclusive); must be {@code >= startOffset}
     * @param renderedText the exact text that was inserted into the report; must not be
     * {@code null}
     * @throws NullPointerException if {@code id}, {@code templateId}, or {@code renderedText}
     * is {@code null}
     * @throws IllegalArgumentException if {@code startOffset < 0} or
     * {@code endOffset < startOffset}
     */
    @JsonCreator public InjectedComment(
            @JsonProperty("id") UUID id,
            @JsonProperty("templateId") UUID templateId,
            @JsonProperty("startOffset") int startOffset,
            @JsonProperty("endOffset") int endOffset,
            @JsonProperty("renderedText") String renderedText
    ) {
        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("Invalid offsets");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.templateId = Objects.requireNonNull(templateId, "templateId");
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.renderedText = Objects.requireNonNull(renderedText, "renderedText");
    }

    /* ---------- Factory ---------- */
    /**
     * Factory method that creates a new injected comment with a random UUID.
     *
     * @param templateId identifier of the template this comment was injected from;
     *                   must not be {@code null}
     * @param startOffset starting offset in the report text (inclusive); must be {@code >= 0}
     * @param endOffset ending offset in the report text (exclusive); must be {@code >= startOffset}
     * @param renderedText the exact text that was inserted into the report; must not be
     * {@code null}
     * @return a new {@code InjectedComment} instance
     * @throws NullPointerException if {@code templateId} or {@code renderedText} is {@code null}
     * @throws IllegalArgumentException if {@code startOffset < 0} or
     * {@code endOffset < startOffset}
     */
    public static InjectedComment create(
            UUID templateId,
            int startOffset,
            int endOffset,
            String renderedText
    ) {
        return new InjectedComment(
                UUID.randomUUID(),
                templateId,
                startOffset,
                endOffset,
                renderedText
        );
    }

    /* ---------- Offset Adjustment ---------- */
    /**
     * Returns a copy of this injected comment shifted by {@code delta} characters.
     * <p>
     * This is used when edits occur before the injected comment, causing the comment
     * to move forward/backward in the document.
     *
     * @param delta number of characters to shift offsets by (can be negative)
     * @return a new {@code InjectedComment} with updated offsets
     * @throws IllegalArgumentException if the resulting offsets would be invalid
     */
    public InjectedComment shift(int delta) {
        return new InjectedComment(
                this.id,
                this.templateId,
                this.startOffset + delta,
                this.endOffset + delta,
                this.renderedText
        );
    }

    /**
     * Returns a copy of this injected comment with its end offset expanded by {@code delta}.
     * <p>
     * This is used when edits occur inside or at the end of the injected comment region,
     * causing the comment span to grow.
     *
     * @param delta number of characters to expand the end offset by (can be negative)
     * @return a new {@code InjectedComment} with an updated end offset
     * @throws IllegalArgumentException if the resulting offsets would be invalid
     */
    public InjectedComment expand(int delta) {
        return new InjectedComment(
                this.id,
                this.templateId,
                this.startOffset,
                this.endOffset + delta,
                this.renderedText
        );
    }

    /* ---------- Equality ---------- */

    /**
     * Compares this injected comment to another object for equality.
     * <p>
     * Equality is based solely on {@link #id()}.
     *
     * @param o object to compare to
     * @return {@code true} if the other object is an {@code InjectedComment} with the same
     * {@code id}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InjectedComment)) {
            return false;
        }
        return id.equals(((InjectedComment) o).id);
    }

    /**
     * Returns a hash code for this injected comment.
     * <p>
     * The hash code is based solely on {@link #id()} to match {@link #equals(Object)}.
     *
     * @return hash code based on {@code id}
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

