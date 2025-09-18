package com.cab302.eduplanner.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;

import com.cab302.eduplanner.App;

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
    private Button backButton, uploadAssignmentButton, uploadRubricButton;

    @FXML
    private Label statusLabel;

    /**
     * Pre-populates the view with default feedback text and resets status indicators.
     */
    @FXML
    private void initialize() {
        // Initialize any necessary data or state here
        feedbackTextArea.setText("No feedback can be provided until documents are submitted.");
        progressIndicator.setProgress(0);
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
            statusLabel.setText("Rubric uploaded: " + file.getName());
        }
    }

    /**
     * Handles rubric submission by updating the user-visible status message.
     */
    @FXML
    private void handleSubmitButtonAction() {
        // Handle the submit button action here
        statusLabel.setText("Rubric analysis submitted.");
    }
}