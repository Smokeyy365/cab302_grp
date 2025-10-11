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
import java.io.IOException;
import java.util.Locale;


/**
 * Handles rubric uploads and user-facing feedback for the analysis workflow.
 */
public class RubricController {

    @FXML
    private TextArea feedbackTextArea;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Button submitButton;

    @FXML
    private Button dashboardButton, uploadAssignmentButton, uploadRubricButton;

    @FXML
    private Label statusLabel;

    private final RubricAnalysisService analysisService = new RubricAnalysisService();
    private File assignmentFile;
    private File rubricFile;

    /**
     * Pre-populates the view with default feedback text and resets status indicators.
     */
    @FXML
    private void initialize() {
        // Initialize any necessary data or state here
        feedbackTextArea.setText("No feedback can be provided until documents are submitted.");
        progressIndicator.setProgress(0);
        progressIndicator.setVisible(false);
        statusLabel.setText("");
    }

    /**
     * Prompts the user to select an assignment document and updates the status label.
     */
    @FXML
    private void handleUploadAssignment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Assignment");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("Word Documents", "*.docx"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showOpenDialog(uploadAssignmentButton.getScene().getWindow());
        if (file != null) {
            // Handle the file upload
            assignmentFile = file;
            statusLabel.setText("Assignment uploaded: " + file.getName());
        }
    }

    /**
     * Prompts the user to select a rubric document and updates the status label.
     */
    @FXML
    private void handleUploadRubric() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Rubric");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("Word Documents", "*.docx"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        File file = fileChooser.showOpenDialog(uploadRubricButton.getScene().getWindow());
        if (file != null) {
            // Handle the file upload
            rubricFile = file;
            statusLabel.setText("Rubric uploaded: " + file.getName());
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
        submitButton.setDisable(true);
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
            submitButton.setDisable(false);
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
            submitButton.setDisable(false);
        });

        Thread worker = new Thread(analysisTask, "rubric-analysis-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private String formatResult(RubricAnalysisResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.US, "Overall GPA: %.2f%n%n", result.getOverallGpa()));
        if (result.getCategories().isEmpty()) {
            builder.append("No category level feedback was returned by the AI grader.");
            return builder.toString();
        }
        for (RubricCategoryEvaluation category : result.getCategories()) {
            builder.append(category.getName()).append(System.lineSeparator());
            builder.append(String.format(Locale.US, "  GPA: %.2f%n", category.getGpa()));
            builder.append("  Evidence: ").append(category.getEvidence()).append(System.lineSeparator()).append(System.lineSeparator());
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
            System.err.println("Failed to switch to Dashboard scene: " + ex.getMessage());
            statusLabel.setText("Failed to return to dashboard.");
        }
    }

}