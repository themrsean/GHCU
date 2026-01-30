/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package comments.persistence;

import comments.domain.CommentTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
/**
 * Persistence interface for {@link CommentTemplate} objects.
 * <p>
 * A {@code CommentRepository} provides CRUD operations and query support for
 * reusable comment templates used by the grading/comment system.
 * <p>
 * Implementations may store templates in JSON files, databases, or other backing
 * stores. The interface does not prescribe caching behavior or ordering of
 * returned results unless stated in the method contract.
 *
 * @author Sean Jones
 */
public interface CommentRepository {

    /**
     * Loads and returns all comment templates from the backing store.
     *
     * @return list of all templates (possibly empty)
     */
    List<CommentTemplate> loadAll();

    /**
     * Looks up a template by its unique identifier.
     *
     * @param id template id to search for
     * @return the matching template, or {@link Optional#empty()} if not found
     * @throws NullPointerException if {@code id} is {@code null}
     */
    Optional<CommentTemplate> findById(UUID id);

    /**
     * Saves a template to the backing store.
     * <p>
     * If a template with the same id already exists, it is replaced/updated.
     *
     * @param template template to save
     * @throws NullPointerException if {@code template} is {@code null}
     */
    void save(CommentTemplate template);

    /**
     * Deletes a template from the backing store.
     * <p>
     * If the id does not exist, the implementation may treat this as a no-op.
     *
     * @param id template id to delete
     * @throws NullPointerException if {@code id} is {@code null}
     */
    void delete(UUID id);

    /**
     * Imports templates from the given path into the backing store.
     * <p>
     * Import behavior (merge vs replace) is implementation-defined.
     *
     * @param path path to import from
     * @throws NullPointerException if {@code path} is {@code null}
     */
    void importFrom(Path path);

    /**
     * Exports templates from the backing store to the given path.
     *
     * @param path destination path to export to
     * @throws NullPointerException if {@code path} is {@code null}
     */
    void exportTo(Path path);

    /**
     * Returns all templates associated with the given assignment id.
     *
     * @param assignmentId assignment identifier
     * @return list of matching templates (possibly empty)
     * @throws NullPointerException if {@code assignmentId} is {@code null}
     */
    List<CommentTemplate> findByAssignment(String assignmentId);

    /**
     * Returns all templates associated with the given rubric id.
     *
     * @param rubricId rubric identifier
     * @return list of matching templates (possibly empty)
     * @throws NullPointerException if {@code rubricId} is {@code null}
     */
    List<CommentTemplate> findByRubric(String rubricId);

    /**
     * Returns all templates containing the given tag.
     *
     * @param tag tag to match
     * @return list of matching templates (possibly empty)
     * @throws NullPointerException if {@code tag} is {@code null}
     */
    List<CommentTemplate> findByTag(String tag);

    /**
     * Performs a free-text search over templates.
     * <p>
     * The fields searched (title/body/tags/etc.) and matching rules (substring,
     * case sensitivity, tokenization) are implementation-defined.
     *
     * @param query query text
     * @return list of matching templates (possibly empty)
     * @throws NullPointerException if {@code query} is {@code null}
     */
    List<CommentTemplate> search(String query);
}