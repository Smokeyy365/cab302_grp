package com.cab302.eduplanner.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Container for the complete grading response returned by the OpenAI rubric analysis.
 */
public class RubricAnalysisResult {

    private final double overallGpa;
    private final List<RubricCategoryEvaluation> categories;

    @JsonCreator
    public RubricAnalysisResult(
            @JsonProperty(value = "overallGpa", required = true) double overallGpa,
            @JsonProperty(value = "categories", required = true) List<RubricCategoryEvaluation> categories) {
        this.overallGpa = overallGpa;
        this.categories = categories == null ? List.of() : List.copyOf(categories);
    }

    public double getOverallGpa() {
        return overallGpa;
    }

    public List<RubricCategoryEvaluation> getCategories() {
        return Collections.unmodifiableList(categories);
    }
}