package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.App;
import com.cab302.eduplanner.model.RubricAnalysisResult;
import com.cab302.eduplanner.model.RubricCategoryEvaluation;
import com.cab302.eduplanner.service.RubricAnalysisService;
import javafx.concurrent.Task;

import java.io.File;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Locale;

/**
 * Handles rubric uploads and user-facing feedback for the analysis workflow.
 */
public class RubricController {

    private static final Logger log = LogManager.getLogger(RubricController.class);
    @FXML
    private TextArea feedbackTextArea;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Button generateButton;

    @FXML
    private Button dashboardButton;
    @FXML
    private Button uploadAssignmentButton;
    @FXML
    private Button uploadRubricButton;

    @FXML
    private Label statusLabel;

    private final RubricAnalysisService analysisService = new RubricAnalysisService();
    private File assignmentFile;
    private File rubricFile;

    /**
     * Pre-populates the view with default feedback text and resets status
     * indicators.
     */
    @FXML
    private void initialize() {
        // Initialize any necessary data or state here
        feedbackTextArea.setText("No feedback can be provided until documents are submitted.");
        progressIndicator.setProgress(0);
        progressIndicator.setVisible(false);
        statusLabel.setText("");
        // Ensure the generate button is disabled until both files are uploaded
        if (generateButton != null) {
            generateButton.setDisable(true);
        }
    }

    /**
     * Prompts the user to select an assignment document and updates the status
     * label.
     */
    @FXML
    private void handleUploadAssignment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Assignment");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Supported Document Types", "*.pdf", "*.docx", "*.txt"));
        File file = fileChooser.showOpenDialog(uploadAssignmentButton.getScene().getWindow());
        if (file != null) {
            // Handle the file upload
            assignmentFile = file;
            statusLabel.setText("Assignment uploaded: " + file.getName());
            refreshSubmitState();
        }
    }

    /**
     * Prompts the user to select a rubric document and updates the status label.
     */
    @FXML
    private void handleUploadRubric() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Rubric");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Supported Document Types", "*.pdf", "*.docx", "*.txt"));
        File file = fileChooser.showOpenDialog(uploadRubricButton.getScene().getWindow());
        if (file != null) {
            // Handle the file upload
            rubricFile = file;
            statusLabel.setText("Success! Rubric uploaded: " + file.getName());
            refreshSubmitState();
        }
    }

    /**
     * Refreshes the enabled/disabled state of the submit button based on
     * whether both required files have been uploaded.
     */
    private void refreshSubmitState() {
        boolean ready = assignmentFile != null && rubricFile != null;
        if (generateButton != null) {
            generateButton.setDisable(!ready);
        }
    }

    /**
     * Handles rubric submission by updating the user-visible status message.
     */
    @FXML
    private void handleSubmitButtonAction() {
        // Handle the submit button action here
        if (assignmentFile == null || rubricFile == null) {
            statusLabel.setText("Upload both an assignment and a rubric before requesting analysis.");
            return;
        }

        progressIndicator.setVisible(true);
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
    generateButton.setDisable(true);
        statusLabel.setText("Analysing submission with OpenAI rubric grader...");

        Task<RubricAnalysisResult> analysisTask = new Task<>() {
            @Override
            protected RubricAnalysisResult call() throws Exception {
                return analysisService.analyse(assignmentFile.toPath(), rubricFile.toPath());
            }
        };

        analysisTask.setOnSucceeded(event -> {
            RubricAnalysisResult result = analysisTask.getValue();
            feedbackTextArea.setText(formatResult(result));
            statusLabel.setText("Rubric analysis completed.");
            progressIndicator.setVisible(false);
            progressIndicator.setProgress(1);
            generateButton.setDisable(false);
        });

        analysisTask.setOnFailed(event -> {
            Throwable error = analysisTask.getException();
            if (error != null) {
                error.printStackTrace();
            }
            feedbackTextArea.setText("An error occurred while running the rubric analysis.");
            if (error instanceof IllegalStateException) {
                statusLabel.setText(error.getMessage());
            } else {
                statusLabel.setText("Failed to analyse rubric. Check the application logs for details.");
            }
            progressIndicator.setVisible(false);
            progressIndicator.setProgress(0);
            generateButton.setDisable(false);
        });

        Thread worker = new Thread(analysisTask, "rubric-analysis-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private String formatResult(RubricAnalysisResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.US, "Overall Score: %.2f / %.2f%n%n", result.getOverallScore(),
                result.getOverallMaxScore()));
        if (result.getCategories().isEmpty()) {
            builder.append("No category level feedback was returned by the AI grader.");
            return builder.toString();
        }
        for (RubricCategoryEvaluation category : result.getCategories()) {
            builder.append(category.getName()).append(System.lineSeparator());
            builder.append(
                    String.format(Locale.US, "  Score: %.2f / %.2f%n", category.getScore(), category.getMaxScore()));
            builder.append("  Evidence: ").append(category.getEvidence()).append(System.lineSeparator());
            if (!category.getImprovementSteps().isEmpty()) {
                builder.append("  Improvements:\n");
                for (String step : category.getImprovementSteps()) {
                    builder.append("    • ").append(step).append(System.lineSeparator());
                }
            }
            builder.append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    /**
     * Handles navigation back to the dashboard view.
     */
    @FXML
    private void handleDashboardButtonAction() {
        try {
            Stage stage = (Stage) dashboardButton.getScene().getWindow();
            App.changeScene(stage, "/com/cab302/eduplanner/dashboard.fxml", "EduPlanner — Dashboard");
        } catch (IOException ex) {
            log.error("Failed to switch to Dashboard scene: {}", ex.getMessage());
            statusLabel.setText("Failed to return to dashboard.");
        }
    }

}