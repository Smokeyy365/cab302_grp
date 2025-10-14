package com.cab302.eduplanner.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

/**
 * Represents the model's judgement for a single rubric category.
 */
public class RubricCategoryEvaluation {

    private final String name;
    private final double score;
    private final double maxScore;
    private final String evidence;
    private final List<String> improvementSteps;

    @JsonCreator
    public RubricCategoryEvaluation(
            @JsonProperty(value = "name", required = false) String name,
            @JsonProperty(value = "score", required = false) Double score,
            @JsonProperty(value = "maxScore", required = false) Double maxScore,
            @JsonProperty(value = "evidence", required = false) String evidence,
            @JsonProperty(value = "improvementSteps", required = false) List<String> improvementSteps) {
        // Provide safe defaults when the model omits fields
        this.name = name == null ? "" : name;
        this.score = score == null ? 0.0 : score.doubleValue();
        this.maxScore = maxScore == null ? 0.0 : maxScore.doubleValue();
        this.evidence = evidence == null ? "" : evidence;
        this.improvementSteps = improvementSteps == null ? List.of() : List.copyOf(improvementSteps);
    }

    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public String getEvidence() {
        return evidence;
    }

    public List<String> getImprovementSteps() {
        return Collections.unmodifiableList(improvementSteps);
    }
}