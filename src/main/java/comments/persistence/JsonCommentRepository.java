/*
 * Course: CSC-1120
 * ASSIGNMENT
 * CLASS
 * Name: Sean Jones
 * Last Updated:
 */
package main.java.comments.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.java.comments.domain.CommentTemplate;

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

public final class JsonCommentRepository implements CommentRepository {
    private final Path storageFile;
    private final ObjectMapper mapper;

    // In-memory cache (authoritative during runtime)
    private List<CommentTemplate> cache = List.of();

    public JsonCommentRepository(Path storageFile, ObjectMapper mapper) {
        this.storageFile = Objects.requireNonNull(storageFile);
        this.mapper = Objects.requireNonNull(mapper);
        loadFromDisk();
    }

    @Override
    public List<CommentTemplate> loadAll() {
        return List.copyOf(cache);
    }

    @Override
    public Optional<CommentTemplate> findById(UUID id) {
        return cache.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();
    }

    @Override
    public void save(CommentTemplate template) {
        Objects.requireNonNull(template);
        Map<UUID, CommentTemplate> merged = cache.stream()
                .collect(Collectors.toMap(
                        CommentTemplate::getId,
                        c -> c,
                        (a, _) -> a,
                        LinkedHashMap::new
                ));
        merged.put(template.getId(), template);
        cache = List.copyOf(merged.values());
        flush();
    }

    @Override
    public void delete(UUID id) {
        cache = cache.stream()
                .filter(c -> !c.getId().equals(id))
                .toList();
        flush();
    }

    @Override
    public void importFrom(Path path) {
        Objects.requireNonNull(path);
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
                merged.put(c.getId(), c);
            }
            // Imported overwrite by UUID
            for (CommentTemplate c : imported) {
                merged.put(c.getId(), c);
            }
            cache = List.copyOf(merged.values());
            flush();

        } catch (IOException e) {
            throw new RuntimeException("Failed to import comments", e);
        }
    }

    @Override
    public void exportTo(Path path) {
        Objects.requireNonNull(path);
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(path.toFile(), cache);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export comments", e);
        }
    }

    @Override
    public List<CommentTemplate> findByAssignment(String assignmentId) {
        return cache.stream()
                .filter(c -> c.getAssignmentId().equals(assignmentId))
                .toList();
    }

    @Override
    public List<CommentTemplate> findByRubric(String rubricId) {
        return cache.stream()
                .filter(c -> c.getRubricId().equals(rubricId))
                .toList();
    }

    @Override
    public List<CommentTemplate> findByTag(String tag) {
        return cache.stream()
                .filter(c -> c.getTags().contains(tag))
                .toList();
    }

    @Override
    public List<CommentTemplate> search(String query) {
        String q = query.toLowerCase();
        return cache.stream()
                .filter(c ->
                        c.getTitle().toLowerCase().contains(q) ||
                                c.getBody().toLowerCase().contains(q))
                .toList();
    }

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
                mapper.writerWithDefaultPrettyPrinter()
                        .writeValue(storageFile.toFile(), cache);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist comments", e);
        }
    }
}
