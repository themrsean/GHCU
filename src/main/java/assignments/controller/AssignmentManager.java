/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/23/2026
 */
package assignments.controller;

import assignments.model.Assignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssignmentManager {
    private final List<Assignment> assignments = new ArrayList<>();

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void addAssignment(Assignment assignment) {
        assignments.add(assignment);
    }

    public void removeAssignment(Assignment assignment) {
        assignments.remove(assignment);
    }

    public Optional<Assignment> findByShortName(String shortName) {
        return assignments.stream()
                .filter(a -> a.getShortName().equals(shortName))
                .findFirst();
    }
}
