package com.cab302.eduplanner.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the model's judgement for a single rubric category.
 */
public class RubricCategoryEvaluation {

    private final String name;
    private final double gpa;
    private final String evidence;

    @JsonCreator
    public RubricCategoryEvaluation(
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "gpa", required = true) double gpa,
            @JsonProperty(value = "evidence", required = true) String evidence) {
        this.name = name;
        this.gpa = gpa;
        this.evidence = evidence;
    }

    public String getName() {
        return name;
    }

    public double getGpa() {
        return gpa;
    }

    public String getEvidence() {
        return evidence;
    }
}