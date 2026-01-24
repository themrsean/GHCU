/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/23/2026
 */
package edu.msoe.csse.jones.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.msoe.csse.jones.model.Assignment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class AssignmentStore {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void save(Path file, List<Assignment> assignments)
            throws IOException {
        MAPPER.writerWithDefaultPrettyPrinter()
                .writeValue(file.toFile(), assignments);
    }

    public static List<Assignment> load(Path file)
            throws IOException {
        return MAPPER.readValue(
                file.toFile(),
                new TypeReference<>() {

                }
        );
    }
}
