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

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class CommentTemplate {

    private final UUID id;
    private final String title;
    private final String body;
    private final String assignmentId;
    private final String rubricId;
    private final Set<String> tags;
    private final Instant createdAt;
    private final Instant updatedAt;

    @JsonCreator
    public CommentTemplate(
            @JsonProperty("id") UUID id,
            @JsonProperty("title") String title,
            @JsonProperty("body") String body,
            @JsonProperty("assignmentId") String assignmentId,
            @JsonProperty("rubricId") String rubricId,
            @JsonProperty("tags") Set<String> tags,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.title = requireNonBlank(title, "title");
        this.body = requireNonBlank(body, "body");
        this.assignmentId = requireNonBlank(assignmentId, "assignmentId");
        this.rubricId = requireNonBlank(rubricId, "rubricId");
        this.tags = Set.copyOf(tags == null ? Set.of() : tags);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    /* ---------- Factory Methods ---------- */

    public static CommentTemplate create(
            String title,
            String body,
            String assignmentId,
            String rubricId,
            Set<String> tags
    ) {
        Instant now = Instant.now();
        return new CommentTemplate(
                UUID.randomUUID(),
                title,
                body,
                assignmentId,
                rubricId,
                tags,
                now,
                now
        );
    }

    public CommentTemplate withUpdatedContent(
            String newTitle,
            String newBody,
            Set<String> newTags
    ) {
        return new CommentTemplate(
                this.id,
                newTitle,
                newBody,
                this.assignmentId,
                this.rubricId,
                newTags,
                this.createdAt,
                Instant.now()
        );
    }

    /* ---------- Accessors ---------- */

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public String getRubricId() {
        return rubricId;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /* ---------- Equality ---------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommentTemplate)) return false;
        CommentTemplate that = (CommentTemplate) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /* ---------- Helpers ---------- */

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
