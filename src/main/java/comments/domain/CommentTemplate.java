/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package comments.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * A reusable feedback comment template for the grading comment system.
 * <p>
 * A {@code CommentTemplate} stores the text and metadata for a single reusable
 * comment, including:
 * <ul>
 *     <li>a stable unique identifier ({@link #id()})</li>
 *     <li>title and body content</li>
 *     <li>assignment/rubric association</li>
 *     <li>optional tags for searching/filtering</li>
 *     <li>creation/update timestamps</li>
 * </ul>
 * <p>
 * This record is designed to be serialized/deserialized with Jackson. During
 * deserialization, missing values for {@code id}, {@code createdAt}, and
 * {@code updatedAt} are defaulted in the canonical constructor.
 * <p>
 * Equality and hash code are based solely on {@code id}.
 *
 * @author Sean Jones
 * @param id unique identifier for the template; if {@code null}, a random UUID is generated
 * @param title short title for the template; must not be blank
 * @param body template content; must not be blank
 * @param assignmentId assignment identifier this template belongs to; must not be blank
 * @param rubricId rubric identifier this template belongs to; must not be blank
 * @param tags optional set of tags; if {@code null}, an empty set is used
 * @param createdAt creation timestamp; if {@code null}, defaults to {@link Instant#now()}
 * @param updatedAt last updated timestamp; if {@code null}, defaults to {@code createdAt}
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CommentTemplate(UUID id, String title, String body, String assignmentId,
                              String rubricId, Set<String> tags, Instant createdAt,
                              Instant updatedAt) {
    /**
     * Creates a {@code CommentTemplate}.
     * <p>
     * This constructor is also used by Jackson during JSON deserialization.
     * If {@code id}, {@code createdAt}, or {@code updatedAt} are missing from JSON,
     * default values are generated:
     * <ul>
     *     <li>{@code id} defaults to a new random UUID</li>
     *     <li>{@code createdAt} defaults to {@link Instant#now()}</li>
     *     <li>{@code updatedAt} defaults to {@code createdAt}</li>
     * </ul>
     *
     * @param id unique identifier for the template; if {@code null}, a random UUID is generated
     * @param title short title for the template; must not be blank
     * @param body template content; must not be blank
     * @param assignmentId assignment identifier this template belongs to; must not be blank
     * @param rubricId rubric identifier this template belongs to; must not be blank
     * @param tags optional set of tags; if {@code null}, an empty set is used
     * @param createdAt creation timestamp; if {@code null}, defaults to {@link Instant#now()}
     * @param updatedAt last updated timestamp; if {@code null}, defaults to {@code createdAt}
     * @throws IllegalArgumentException if {@code title}, {@code body}, {@code assignmentId},
     *                                  or {@code rubricId} are null/blank
     */
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
        this.id = id == null ? UUID.randomUUID() : id;
        this.title = requireNonBlank(title, "title");
        this.body = requireNonBlank(body, "body");
        this.assignmentId = requireNonBlank(assignmentId, "assignmentId");
        this.rubricId = requireNonBlank(rubricId, "rubricId");
        this.tags = Set.copyOf(tags == null ? Set.of() : tags);

        Instant now = Instant.now();
        this.createdAt = createdAt == null ? now : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    /* ---------- Factory Methods ---------- */
    /**
     * Factory method that creates a new template with a new random UUID and timestamps.
     * <p>
     * Both {@code createdAt} and {@code updatedAt} are initialized to {@link Instant#now()}.
     *
     * @param title short title for the template; must not be blank
     * @param body template content; must not be blank
     * @param assignmentId assignment identifier this template belongs to; must not be blank
     * @param rubricId rubric identifier this template belongs to; must not be blank
     * @param tags optional set of tags; may be {@code null}
     * @return a newly created {@code CommentTemplate}
     * @throws IllegalArgumentException if {@code title}, {@code body}, {@code assignmentId},
     *                                  or {@code rubricId} are null/blank
     */
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

    /**
     * Returns a copy of this template with updated title/body/tags and a refreshed
     * {@code updatedAt} timestamp.
     * <p>
     * The returned template preserves:
     * <ul>
     *     <li>{@code id}</li>
     *     <li>{@code assignmentId}</li>
     *     <li>{@code rubricId}</li>
     *     <li>{@code createdAt}</li>
     * </ul>
     *
     * @param newTitle updated title; must not be blank
     * @param newBody updated body; must not be blank
     * @param newTags updated tags; may be {@code null}
     * @return a new {@code CommentTemplate} instance with updated content
     * @throws IllegalArgumentException if {@code newTitle} or {@code newBody} are null/blank
     */
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

    /* ---------- Equality ---------- */
    /**
     * Compares this template to another object for equality.
     * <p>
     * Equality is based solely on the template {@code id}.
     *
     * @param o object to compare to
     * @return {@code true} if the other object is a {@code CommentTemplate} with the same
     * {@code id}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommentTemplate that)) {
            return false;
        }
        return id.equals(that.id);
    }

    /**
     * Returns a hash code for this template.
     * <p>
     * The hash code is based solely on the template {@code id} to match {@link #equals(Object)}.
     *
     * @return hash code based on {@code id}
     */
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
