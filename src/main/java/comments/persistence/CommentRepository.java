package comments.persistence;

import comments.domain.CommentTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository {

    List<CommentTemplate> loadAll();

    Optional<CommentTemplate> findById(UUID id);

    void save(CommentTemplate template);

    void delete(UUID id);

    void importFrom(Path path);

    void exportTo(Path path);

    List<CommentTemplate> findByAssignment(String assignmentId);

    List<CommentTemplate> findByRubric(String rubricId);

    List<CommentTemplate> findByTag(String tag);

    List<CommentTemplate> search(String query);
}
