/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/23/2026
 */
package assignments.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Rubric {
    private final ObservableList<RubricItem> items;
    private final IntegerProperty totalPoints;

    public Rubric() {
        totalPoints = new SimpleIntegerProperty(0);
        items = FXCollections.observableArrayList();
        items.addListener((ListChangeListener<RubricItem>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (RubricItem item : c.getAddedSubList()) {
                        item.pointsProperty().addListener((_, _, _) -> recomputeTotal());
                    }
                }
            }
            recomputeTotal();
        });
    }

    public ObservableList<RubricItem> getItems() {
        return items;
    }

    public IntegerProperty totalPointsProperty() {
        return totalPoints;
    }

    @JsonIgnore
    public int getTotalPoints() {
        return totalPoints.get();
    }

    public void setItems(List<RubricItem> items) {
        this.items.setAll(items);
    }

    @JsonIgnore
    public boolean isValid() {
        final int assignmentPoints = 100;
        return getTotalPoints() == assignmentPoints;
    }

    private void recomputeTotal() {
        totalPoints.set(
                items.stream().mapToInt(RubricItem::getPoints).sum()
        );
    }
}
