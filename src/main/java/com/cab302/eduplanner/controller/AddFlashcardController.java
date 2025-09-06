package com.cab302.eduplanner.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.cab302.eduplanner.model.Flashcard;

public class AddFlashcardController {

    @FXML private TextField questionField;
    @FXML private TextField answerField;
    @FXML private Button saveButton, cancelButton;
    @FXML private Label errorLabel; // inline validation label

    private Flashcard newFlashcard; // result after save

    @FXML
    public void initialize() {
        saveButton.setOnAction(e -> saveFlashcard());
        cancelButton.setOnAction(e -> closeWindow());
    }

    private void saveFlashcard() {
        String q = questionField.getText().trim();
        String a = answerField.getText().trim();

        if (!q.isEmpty() && !a.isEmpty()) {
            newFlashcard = new Flashcard(q, a);
            closeWindow();
        } else {
            // Show error inline instead of popup
            errorLabel.setText("Please enter both a question and an answer.");
            errorLabel.setVisible(true);
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) questionField.getScene().getWindow();
        stage.close();
    }

    public Flashcard getNewFlashcard() {
        return newFlashcard;
    }

    // Pre-fill fields when editing an existing flashcard
    public void setFlashcard(Flashcard flashcard) {
        if (flashcard != null) {
            questionField.setText(flashcard.getQuestion());
            answerField.setText(flashcard.getAnswer());
        }
    }
}
