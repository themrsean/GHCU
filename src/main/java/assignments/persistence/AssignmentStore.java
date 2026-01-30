/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/30/2026
 */
package assignments.persistence;

import assignments.model.Assignment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Persistence utility class for saving and loading {@link Assignment} data.
 * <p>
 * This class provides JSON serialization/deserialization for assignment definitions
 * used by the GitHub Classroom Utilities application. Assignments are stored as a
 * JSON array (a {@code List<Assignment>}) using Jackson.
 * </p>
 *
 * <p>
 * This is a pure utility class and cannot be instantiated.
 * </p>
 *
 * @author Sean Jones
 */
public final class AssignmentStore {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AssignmentStore() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    /**
     * Saves the given assignment list to the specified file in JSON format.
     * <p>
     * The output is written using Jackson's default pretty printer to make the file
     * easier to read and edit manually.
     * </p>
     *
     * @param file the file path to write to; must be non-null
     * @param assignments the assignments to serialize; must be non-null
     * @throws NullPointerException if {@code file} or {@code assignments} is {@code null}
     * @throws IOException if the file cannot be written
     */
    public static void save(Path file, List<Assignment> assignments)
            throws IOException {
        Objects.requireNonNull(assignments, "assignments cannot be null");
        Objects.requireNonNull(file, "file cannot be null");
        MAPPER.writerWithDefaultPrettyPrinter()
                .writeValue(file.toFile(), assignments);
    }

    /**
     * Loads assignments from the specified JSON file.
     * <p>
     * The file is expected to contain a JSON array that can be deserialized into a
     * {@code List<Assignment>}.
     * </p>
     *
     * @param file the file path to read from; must be non-null
     * @return the list of loaded assignments
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws IOException if the file cannot be read or the JSON is invalid
     */
    public static List<Assignment> load(Path file)
            throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        return MAPPER.readValue(
                file.toFile(),
                new TypeReference<>() { }
        );
    }
}