/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/23/2026
 */
package assignments.controller;

import assignments.model.Assignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages the set of {@link Assignment} definitions available in the application.
 * <p>
 * Assignments represent grading configurations (short name, full name, file list,
 * and rubric). This manager provides basic CRUD-style operations for maintaining
 * the in-memory list of assignments and locating assignments by short name.
 * </p>
 *
 * <p>
 * This class does not perform persistence. Saving/loading assignments is handled
 * elsewhere (ex: via an AssignmentStore).
 * </p>
 *
 * @author Sean Jones
 */
public class AssignmentManager {
    private final List<Assignment> assignments = new ArrayList<>();

    /**
     * Returns the list of assignments managed by this instance.
     * <p>
     * The returned list is the live backing list; modifications to it will affect
     * the manager's state.
     * </p>
     *
     * @return the current list of assignments
     */
    public List<Assignment> getAssignments() {
        return assignments;
    }

    /**
     * Adds an assignment to the manager.
     *
     * @param assignment the assignment to add
     */
    public void addAssignment(Assignment assignment) {
        Objects.requireNonNull(assignment, "assignment");
        assignments.add(assignment);
    }

    /**
     * Removes an assignment from the manager.
     * <p>
     * If the assignment is not present, this method has no effect.
     * </p>
     *
     * @param assignment the assignment to remove
     */
    public void removeAssignment(Assignment assignment) {
        Objects.requireNonNull(assignment, "assignment");
        assignments.remove(assignment);
    }

    /**
     * Searches for an assignment by its short name.
     * <p>
     * The short name is the assignment identifier used for report naming and selection
     * in the GHCU UI.
     * </p>
     *
     * @param shortName the short name to search for
     * @return an {@link Optional} containing the matching assignment if found;
     *         otherwise {@link Optional#empty()}
     */
    public Optional<Assignment> findByShortName(String shortName) {
        Objects.requireNonNull(shortName, "shortName");
        return assignments.stream()
                .filter(a -> a.getShortName().equals(shortName))
                .findFirst();
    }
}
