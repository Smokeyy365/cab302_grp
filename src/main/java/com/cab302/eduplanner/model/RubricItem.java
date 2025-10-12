package com.cab302.eduplanner.model;

/**
 * Represents a rubric item with a name and point value.
 */
public class RubricItem {
    private String name;
    private int points;

    /**
     * Constructs a rubric item with the given name and points.
     * @param name the rubric item name
     * @param points the point value
     */
    public RubricItem(String name, int points) {
        this.name = name;
        this.points = points;
    }

    /**
     * Gets the rubric item name.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the point value.
     * @return the points
     */
    public int getPoints() {
        return points;
    }
}
