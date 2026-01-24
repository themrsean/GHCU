/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/23/2026
 */
package edu.msoe.csse.jones.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class RubricItem {
    private final StringProperty description;

    private final IntegerProperty points;

    // Jackson
    public RubricItem() {
        description = new SimpleStringProperty();
        points = new SimpleIntegerProperty();
    }

    public RubricItem(String description, int points) {
        this();
        this.description.set(description);
        this.points.set(points);
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public int getPoints() {
        return points.get();
    }

    public void setPoints(int points) {
        this.points.set(points);
    }

    public IntegerProperty pointsProperty() {
        return points;
    }
}
