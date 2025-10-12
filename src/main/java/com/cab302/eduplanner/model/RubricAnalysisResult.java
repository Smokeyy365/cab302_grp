package com.cab302.eduplanner.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Container for the complete grading response returned by the OpenAI rubric analysis.
 */
public class RubricAnalysisResult {

    private final double overallScore;
    private final double overallMaxScore;
    private final List<RubricCategoryEvaluation> categories;

    @JsonCreator
    public RubricAnalysisResult(
            @JsonProperty(value = "overallScore", required = false) Double overallScore,
            @JsonProperty(value = "overallMaxScore", required = false) Double overallMaxScore,
            @JsonProperty(value = "categories", required = false) List<RubricCategoryEvaluation> categories) {
        this.overallScore = overallScore == null ? 0.0 : overallScore.doubleValue();
        this.overallMaxScore = overallMaxScore == null ? 0.0 : overallMaxScore.doubleValue();
        this.categories = categories == null ? List.of() : List.copyOf(categories);
    }

    public double getOverallScore() {
        return overallScore;
    }

    public double getOverallMaxScore() {
        return overallMaxScore;
    }

    public List<RubricCategoryEvaluation> getCategories() {
        return Collections.unmodifiableList(categories);
    }
}