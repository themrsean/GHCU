/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/30/2026
 */
package assignments.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;

/**
 * Represents a single rubric criterion and its associated point value.
 * <p>
 * A {@code RubricItem} is part of a {@link Rubric} and is used to define grading
 * categories such as "Technical Quality" or "Passing Unit Tests". Each item has a
 * description and a point value. This class uses JavaFX properties so values can
 * be bound directly to UI controls.
 * </p>
 *
 * <p>
 * A no-argument constructor is provided for Jackson deserialization.
 * </p>
 *
 * @author Sean Jones
 */
public class RubricItem {
    private final StringProperty description;

    private final IntegerProperty points;

    /**
     * Constructs a default rubric item.
     * <p>
     * This constructor exists primarily for Jackson deserialization.
     * </p>
     */

    public RubricItem() {
        description = new SimpleStringProperty();
        points = new SimpleIntegerProperty();
    }

    /**
     * Constructs a rubric item with the given description and point value.
     *
     * @param description the rubric criterion description; must be non-null and non-blank
     * @param points the point value for this rubric item; must be non-negative
     * @throws NullPointerException if {@code description} is {@code null}
     * @throws IllegalArgumentException if {@code description} is blank or {@code points}
     * is negative
     */
    public RubricItem(String description, int points) {
        this();
        this.description.set(description);
        this.points.set(points);
    }

    /**
     * Returns the rubric item description.
     *
     * @return the description text
     */
    public String getDescription() {
        return description.get();
    }

    /**
     * Sets the rubric item description.
     *
     * @param description the description text; must be non-null and non-blank
     * @throws NullPointerException if {@code description} is {@code null}
     * @throws IllegalArgumentException if {@code description} is blank
     */
    public void setDescription(String description) {
        Objects.requireNonNull(description, "description");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description cannot be blank");
        }
        this.description.set(description);
    }

    /**
     * Returns the JavaFX property representing the description.
     *
     * @return the description property
     */
    public StringProperty descriptionProperty() {
        return description;
    }

    /**
     * Returns the point value for this rubric item.
     *
     * @return the point value
     */
    public int getPoints() {
        return points.get();
    }

    /**
     * Sets the point value for this rubric item.
     *
     * @param points the point value; must be non-negative
     * @throws IllegalArgumentException if {@code points} is negative
     */
    public void setPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("points cannot be negative");
        }
        this.points.set(points);
    }

    /**
     * Returns the JavaFX property representing the point value.
     *
     * @return the points property
     */
    public IntegerProperty pointsProperty() {
        return points;
    }
}