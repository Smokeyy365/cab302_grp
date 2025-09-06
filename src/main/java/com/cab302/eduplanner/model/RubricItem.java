package com.cab302.eduplanner.model;

public class RubricItem {
//    simple placeholder implementation
    private String name;
    private int points;

    public RubricItem(String name, int points) {
        this.name = name;
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public int getPoints() {
        return points;
    }
}
