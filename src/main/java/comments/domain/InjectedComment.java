/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package comments.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class InjectedComment {

    private final UUID id;
    private final UUID templateId;
    private final int startOffset;
    private final int endOffset;
    private final String renderedText;

    @JsonCreator
    public InjectedComment(
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

    public InjectedComment shift(int delta) {
        return new InjectedComment(
                this.id,
                this.templateId,
                this.startOffset + delta,
                this.endOffset + delta,
                this.renderedText
        );
    }

    public InjectedComment expand(int delta) {
        return new InjectedComment(
                this.id,
                this.templateId,
                this.startOffset,
                this.endOffset + delta,
                this.renderedText
        );
    }

    /* ---------- Getters ---------- */

    public UUID getId() { return id; }
    public UUID getTemplateId() { return templateId; }
    public int getStartOffset() { return startOffset; }
    public int getEndOffset() { return endOffset; }
    public String getRenderedText() { return renderedText; }

    /* ---------- Equality ---------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InjectedComment)) return false;
        return id.equals(((InjectedComment) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

