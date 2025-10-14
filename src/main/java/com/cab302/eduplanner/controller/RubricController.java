package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.App;
import com.cab302.eduplanner.model.RubricAnalysisResult;
import com.cab302.eduplanner.model.RubricCategoryEvaluation;
import com.cab302.eduplanner.service.RubricAnalysisService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Handles rubric uploads and user-facing feedback for the analysis workflow.
 */
public class RubricController {

    private static final Logger log = LogManager.getLogger(RubricController.class);

    @FXML private TextArea feedbackTextArea;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button generateButton;

    @FXML private Button dashboardButton;
    @FXML private Button uploadAssignmentButton;
    @FXML private Button uploadRubricButton;

    @FXML private Label statusLabel;

    // Bottom-row buttons
    @FXML private Button uploadToCalendarButton;
    @FXML private Button openPlanInGoogleButton;

    private final RubricAnalysisService analysisService = new RubricAnalysisService();
    private File assignmentFile;
    private File rubricFile;

    @FXML
    private void initialize() {
        feedbackTextArea.setText("No feedback can be provided until documents are submitted.");
        progressIndicator.setProgress(0);
        progressIndicator.setVisible(false);
        statusLabel.setText("");

        if (generateButton != null) generateButton.setDisable(true);

        // Open Plan in Google is ALWAYS enabled
        if (openPlanInGoogleButton != null) openPlanInGoogleButton.setDisable(false);
        // Keep upload-to-calendar gated until we have feedback
        if (uploadToCalendarButton != null) uploadToCalendarButton.setDisable(true);
    }

    @FXML
    private void handleUploadAssignment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Assignment");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Supported Document Types", "*.pdf", "*.docx", "*.txt"));
        File file = fileChooser.showOpenDialog(uploadAssignmentButton.getScene().getWindow());
        if (file != null) {
            assignmentFile = file;
            statusLabel.setText("Assignment uploaded: " + file.getName());
            refreshSubmitState();
        }
    }

    @FXML
    private void handleUploadRubric() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Rubric");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Supported Document Types", "*.pdf", "*.docx", "*.txt"));
        File file = fileChooser.showOpenDialog(uploadRubricButton.getScene().getWindow());
        if (file != null) {
            rubricFile = file;
            statusLabel.setText("Success! Rubric uploaded: " + file.getName());
            refreshSubmitState();
        }
    }

    private void refreshSubmitState() {
        boolean ready = assignmentFile != null && rubricFile != null;
        if (generateButton != null) generateButton.setDisable(!ready);
        // Leave bottom buttons alone (Open always on; Upload gated by result)
    }

    @FXML
    private void handleSubmitButtonAction() {
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

            // Enable Upload-to-Calendar now that we have content
            if (uploadToCalendarButton != null) uploadToCalendarButton.setDisable(false);
            // Open button stays enabled regardless
        });

        analysisTask.setOnFailed(event -> {
            Throwable error = analysisTask.getException();
            if (error != null) error.printStackTrace();
            feedbackTextArea.setText("An error occurred while running the rubric analysis.");
            if (error instanceof IllegalStateException) {
                statusLabel.setText(error.getMessage());
            } else {
                statusLabel.setText("Failed to analyse rubric. Check the application logs for details.");
            }
            progressIndicator.setVisible(false);
            progressIndicator.setProgress(0);
            generateButton.setDisable(false);

            // Keep upload-to-calendar disabled on failure; keep Open enabled
            if (uploadToCalendarButton != null) uploadToCalendarButton.setDisable(true);
        });

        Thread worker = new Thread(analysisTask, "rubric-analysis-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private String formatResult(RubricAnalysisResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.US, "Overall Score: %.2f / %.2f%n%n",
                result.getOverallScore(), result.getOverallMaxScore()));
        if (result.getCategories().isEmpty()) {
            builder.append("No category level feedback was returned by the AI grader.");
            return builder.toString();
        }
        for (RubricCategoryEvaluation category : result.getCategories()) {
            builder.append(category.getName()).append(System.lineSeparator());
            builder.append(String.format(Locale.US, "  Score: %.2f / %.2f%n",
                    category.getScore(), category.getMaxScore()));
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

    // ---------------------
    // Bottom row actions
    // ---------------------

    @FXML
    private void handleUploadToCalendar() {
        String title = "Study Plan";
        String details = feedbackTextArea.getText() == null ? "" : feedbackTextArea.getText();
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        String url = buildGoogleCalendarUrl(title, details, start, start.plusHours(1));
        openInBrowser(url);
    }

    @FXML
    private void handleOpenPlanInGoogle() {
        // Open even if feedback is empty
        String title = "Study Plan";
        String details = feedbackTextArea.getText() == null ? "" : feedbackTextArea.getText();
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        String url = buildGoogleCalendarUrl(title, details, start, start.plusHours(1));
        openInBrowser(url);
    }

    private String buildGoogleCalendarUrl(String title, String description,
                                          LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        String s = start.format(fmt);
        String e = end.format(fmt);

        StringBuilder url = new StringBuilder("https://calendar.google.com/calendar/render?action=TEMPLATE");
        url.append("&text=").append(encode(title));
        url.append("&dates=").append(s).append("/").append(e);
        if (description != null && !description.isBlank()) {
            url.append("&details=").append(encode(description));
        }
        return url.toString();
    }

    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                new Alert(Alert.AlertType.INFORMATION,
                        "Copy this link into your browser:\n" + url).showAndWait();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to open browser:\n" + e.getMessage()).showAndWait();
        }
    }
}

