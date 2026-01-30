/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/23/2026
 */
package assignments.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;

/**
 * Represents a grading assignment definition used by the GHCU application.
 * <p>
 * An {@code Assignment} stores metadata needed for the grading pipeline, including:
 * </p>
 * <ul>
 *     <li>A short name used as an identifier and report filename prefix</li>
 *     <li>A full name displayed in the generated grading report header</li>
 *     <li>A list of source file names that should be included in generated reports</li>
 *     <li>A {@link Rubric} describing the grading criteria and point distribution</li>
 * </ul>
 *
 * <p>
 * This class uses JavaFX properties so it can be bound directly to UI controls.
 * </p>
 *
 * @author Sean Jones
 */
public class Assignment {
    private final StringProperty shortName = new SimpleStringProperty();
    private final StringProperty fullName = new SimpleStringProperty();
    private final ObservableList<String> files;
    private Rubric rubric;

    /**
     * Constructs a new assignment with an empty file list and a default rubric.
     */
    public Assignment() {
        this.files = FXCollections.observableArrayList();
        this.rubric = new Rubric();
    }

    /**
     * Constructs a new assignment with the given short and full names.
     *
     * @param shortName the assignment short name (identifier/report prefix); must be non-null
     *                  and non-blank
     * @param fullName the assignment full name (displayed in reports); must be non-null and
     *                 non-blank
     * @throws NullPointerException if {@code shortName} or {@code fullName} is {@code null}
     * @throws IllegalArgumentException if {@code shortName} or {@code fullName} is blank
     */
    public Assignment(String shortName, String fullName) {
        Objects.requireNonNull(shortName, "shortName");
        Objects.requireNonNull(fullName, "fullName");
        requireNonBlank(shortName, "shortName");
        requireNonBlank(fullName, "fullName");
        this();
        this.shortName.set(shortName);
        this.fullName.set(fullName);
    }

    /**
     * Returns the short name for this assignment.
     *
     * @return the assignment short name
     */
    public String getShortName() {
        return shortName.get();
    }

    /**
     * Sets the short name for this assignment.
     *
     * @param shortName the assignment short name; must be non-null and non-blank
     * @throws NullPointerException if {@code shortName} is {@code null}
     * @throws IllegalArgumentException if {@code shortName} is blank
     */
    public void setShortName(String shortName) {
        Objects.requireNonNull(shortName, "shortName");
        requireNonBlank(shortName, "shortName");
        this.shortName.set(shortName);
    }

    /**
     * Returns the JavaFX property representing the short name.
     *
     * @return the short name property
     */
    public StringProperty shortNameProperty() {
        return shortName;
    }

    /**
     * Returns the full name for this assignment.
     *
     * @return the assignment full name
     */
    public String getFullName() {
        return fullName.get();
    }

    /**
     * Sets the full name for this assignment.
     *
     * @param fullName the assignment full name; must be non-null and non-blank
     * @throws NullPointerException if {@code fullName} is {@code null}
     * @throws IllegalArgumentException if {@code fullName} is blank
     */
    public void setFullName(String fullName) {
        Objects.requireNonNull(fullName, "fullName");
        requireNonBlank(fullName, "fullName");
        this.fullName.set(fullName);
    }

    /**
     * Returns the JavaFX property representing the full name.
     *
     * @return the full name property
     */
    public StringProperty fullNameProperty() {
        return fullName;
    }

    /**
     * Returns the observable list of file names that should be included in the grading report.
     * <p>
     * The returned list is the live backing list; changes to it will be reflected in the UI
     * and in report generation.
     * </p>
     *
     * @return the observable list of file names
     */
    public ObservableList<String> getFiles() {
        return files;
    }

    /**
     * Replaces the current file list with the given list of file names.
     *
     * @param files list of file names to use; must be non-null
     * @throws NullPointerException if {@code files} is {@code null}
     */
    public void setFiles(List<String> files) {
        Objects.requireNonNull(files, "files");
        this.files.setAll(files);
    }

    /**
     * Returns the rubric associated with this assignment.
     *
     * @return the rubric
     */
    public Rubric getRubric() {
        return rubric;
    }

    /**
     * Sets the rubric for this assignment.
     *
     * @param rubric the rubric to assign; must be non-null
     * @throws NullPointerException if {@code rubric} is {@code null}
     */
    public void setRubric(Rubric rubric) {
        Objects.requireNonNull(rubric, "rubric");
        this.rubric = rubric;
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
    }
}