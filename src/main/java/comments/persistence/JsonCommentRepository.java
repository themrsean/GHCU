/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/29/2026
 */
package comments.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import comments.domain.CommentTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JSON-backed implementation of {@link CommentRepository}.
 * <p>
 * This repository persists {@link CommentTemplate} objects in a single JSON file.
 * During runtime, templates are stored in an in-memory cache that acts as the
 * authoritative data source for query operations.
 * <p>
 * Any modifying operation (save/delete/import) updates the cache and then flushes
 * the updated state back to disk.
 * <p>
 * This repository is not thread-safe. All access should occur from a single thread
 * (typically the JavaFX application thread), or be externally synchronized.
 *
 * @author Sean Jones
 */
public final class JsonCommentRepository implements CommentRepository {
    private final Path storageFile;
    private final ObjectMapper mapper;

    // In-memory cache (authoritative during runtime)
    private List<CommentTemplate> cache = List.of();

    /**
     * Creates a repository that persists comment templates in the given JSON file.
     * <p>
     * If the file does not exist, the repository starts empty. If the file exists,
     * it is loaded immediately into memory.
     *
     * @param storageFile path to the JSON file used for persistence
     * @param mapper Jackson object mapper used to serialize/deserialize templates
     * @throws NullPointerException if {@code storageFile} or {@code mapper} is {@code null}
     * @throws RuntimeException if the storage file exists but cannot be read/parsed
     */
    public JsonCommentRepository(Path storageFile, ObjectMapper mapper) {
        this.storageFile = Objects.requireNonNull(storageFile);
        this.mapper = Objects.requireNonNull(mapper);
        loadFromDisk();
    }

    /* CommentRepository API */
    @Override
    public List<CommentTemplate> loadAll() {
        return List.copyOf(cache);
    }

    @Override
    public Optional<CommentTemplate> findById(UUID id) {
        Objects.requireNonNull(id, "id cannot be null");
        return cache.stream()
                .filter(c -> c.id().equals(id))
                .findFirst();
    }

    @Override
    public void save(CommentTemplate template) {
        Objects.requireNonNull(template, "template cannot be null");
        Map<UUID, CommentTemplate> merged = cache.stream()
                .collect(Collectors.toMap(
                        CommentTemplate::id,
                        c -> c,
                        (a, _) -> a,
                        LinkedHashMap::new
                ));
        merged.put(template.id(), template);
        cache = List.copyOf(merged.values());
        flush();
    }

    @Override
    public void delete(UUID id) {
        Objects.requireNonNull(id, "id cannot be null");
        cache = cache.stream()
                .filter(c -> !c.id().equals(id))
                .toList();
        flush();
    }

    @Override
    public void importFrom(Path path) {
        Objects.requireNonNull(path, "path cannot be null");
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Import file does not exist: " + path);
        }
        try {
            List<CommentTemplate> imported =
                    mapper.readValue(path.toFile(), new TypeReference<>() {
                    });
            Map<UUID, CommentTemplate> merged = new LinkedHashMap<>();
            // Existing first
            for (CommentTemplate c : cache) {
                merged.put(c.id(), c);
            }
            // Imported overwrite by UUID
            for (CommentTemplate c : imported) {
                merged.put(c.id(), c);
            }
            cache = List.copyOf(merged.values());
            flush();

        } catch (IOException e) {
            throw new RuntimeException("Failed to import comments", e);
        }
    }

    @Override
    public void exportTo(Path path) {
        Objects.requireNonNull(path, "path cannot be null");
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(path.toFile(), cache);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export comments", e);
        }
    }

    @Override
    public List<CommentTemplate> findByAssignment(String assignmentId) {
        Objects.requireNonNull(assignmentId, "assignment id cannot be null");
        return cache.stream()
                .filter(c -> c.assignmentId().equals(assignmentId))
                .toList();
    }

    @Override
    public List<CommentTemplate> findByRubric(String rubricId) {
        Objects.requireNonNull(rubricId, "rubric id cannot be null");
        return cache.stream()
                .filter(c -> c.rubricId().equals(rubricId))
                .toList();
    }

    @Override
    public List<CommentTemplate> findByTag(String tag) {
        Objects.requireNonNull(tag, "tag cannot be null");
        return cache.stream()
                .filter(c -> c.tags().contains(tag))
                .toList();
    }

    @Override
    public List<CommentTemplate> search(String query) {
        Objects.requireNonNull(query, "query");
        String q = query.toLowerCase();
        return cache.stream()
                .filter(c ->
                        c.title().toLowerCase().contains(q) ||
                                c.body().toLowerCase().contains(q))
                .toList();
    }

    /* Helper methods */
    private void loadFromDisk() {
        if (!Files.exists(storageFile)) {
            cache = List.of();
        } else {
            try {
                cache = List.copyOf(
                        mapper.readValue(
                                storageFile.toFile(),
                                new TypeReference<List<CommentTemplate>>() {
                                }
                        )
                );
            } catch (IOException e) {
                throw new RuntimeException("Failed to load comment repository", e);
            }
        }
    }

    private void flush() {
        try {
            Path parent = storageFile.getParent();
            if(parent != null) {
                Files.createDirectories(parent);
            }
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(storageFile.toFile(), cache);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist comments", e);
        }
    }
}
