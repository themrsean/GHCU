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
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a grading rubric for an {@link Assignment}.
 * <p>
 * A rubric consists of a list of {@link RubricItem}s, each containing a grading
 * criterion and a point value. This class maintains a derived total point value
 * that updates automatically when rubric items are added/removed or when any
 * item's point value changes.
 * </p>
 *
 * <p>
 * This class is designed for use in a JavaFX UI. The rubric items are stored in an
 * {@link ObservableList} and the total points are exposed as an {@link IntegerProperty}
 * for easy binding to UI controls.
 * </p>
 *
 * <p>
 * Rubric objects are also persisted to JSON using Jackson. Derived values such as
 * total points and validity are ignored during serialization.
 * </p>
 *
 * @author Sean Jones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Rubric {
    private static final int ASSIGNMENT_POINTS = 100;
    private final ObservableList<RubricItem> items;
    private final IntegerProperty totalPoints;

    private final Map<RubricItem, ChangeListener<Number>> pointListeners = new HashMap<>();

    /**
     * Constructs an empty rubric.
     * <p>
     * The rubric starts with no items and a total point value of 0. Listeners are
     * installed so that {@link #totalPointsProperty()} is kept in sync with the
     * contents of {@link #getItems()}.
     * </p>
     */
    public Rubric() {
        totalPoints = new SimpleIntegerProperty(0);
        items = FXCollections.observableArrayList();

        items.addListener((ListChangeListener<RubricItem>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (RubricItem item : c.getRemoved()) {
                        detachPointListener(item);
                    }
                }
                if (c.wasAdded()) {
                    for (RubricItem item : c.getAddedSubList()) {
                        attachPointListener(item);
                    }
                }
            }
            recomputeTotal();
        });
    }


    /**
     * Returns the observable list of rubric items.
     * <p>
     * The returned list is the live backing list; modifications to it will update
     * the rubric total automatically.
     * </p>
     *
     * @return the observable list of rubric items
     */
    public ObservableList<RubricItem> getItems() {
        return items;
    }

    /**
     * Returns the JavaFX property representing the total point value of the rubric.
     * <p>
     * This property is derived from the sum of all {@link RubricItem#getPoints()}
     * values and is automatically updated when items or their points change.
     * </p>
     *
     * @return the total points property
     */
    public IntegerProperty totalPointsProperty() {
        return totalPoints;
    }

    /**
     * Returns the total number of points in the rubric.
     * <p>
     * This value is derived from the rubric items and is not serialized to JSON.
     * </p>
     *
     * @return the sum of all rubric item point values
     */
    @JsonIgnore
    public int getTotalPoints() {
        return totalPoints.get();
    }

    /**
     * Replaces the rubric's item list with the given items.
     * <p>
     * This clears the current list and inserts the provided items. The total point
     * value is recomputed automatically and listeners are attached to the new items.
     * </p>
     *
     * @param items list of rubric items to set; must be non-null
     * @throws NullPointerException if {@code items} is {@code null}
     */
    public void setItems(List<RubricItem> items) {
        Objects.requireNonNull(items, "items");
        this.items.setAll(items);
    }

    /**
     * Returns whether this rubric is valid for an assignment.
     * <p>
     * A rubric is considered valid when the total point value equals
     * {@value #ASSIGNMENT_POINTS}.
     * </p>
     *
     * @return {@code true} if the rubric totals {@value #ASSIGNMENT_POINTS} points;
     *         {@code false} otherwise
     */
    @JsonIgnore
    public boolean isValid() {
        return getTotalPoints() == ASSIGNMENT_POINTS;
    }

    private void attachPointListener(RubricItem item) {
        if (!pointListeners.containsKey(item)) {
            ChangeListener<Number> listener = (_, _, _) -> recomputeTotal();
            pointListeners.put(item, listener);
            item.pointsProperty().addListener(listener);
        }
    }

    private void detachPointListener(RubricItem item) {
        ChangeListener<Number> listener = pointListeners.remove(item);
        if (listener != null) {
            item.pointsProperty().removeListener(listener);
        }
    }

    private void recomputeTotal() {
        totalPoints.set(
                items.stream().mapToInt(RubricItem::getPoints).sum()
        );
    }
}