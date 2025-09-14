package com.cab302.eduplanner.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import com.cab302.eduplanner.model.Flashcard;

public class AddFlashcardController {

    @FXML private TextField questionField;
    @FXML private TextField answerField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label errorLabel; // inline validation label

    private Flashcard newFlashcard; // result after save

    @FXML
    public void initialize() {
        // Button handlers
        saveButton.setOnAction(e -> saveFlashcard());
        cancelButton.setOnAction(e -> closeWindow());

        // -------- Navigation --------
        // Question: Enter or down arrow → move to Answer
        questionField.setOnAction(e -> answerField.requestFocus());
        questionField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                answerField.requestFocus();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                closeWindow();
            }
        });

        // Answer: Enter → save | Up arrow -> back to Question
        answerField.setOnAction(e -> saveFlashcard());
        answerField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP) {
                questionField.requestFocus();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                closeWindow();
            }
        });

        // Save button: Enter triggers save
        saveButton.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                saveFlashcard();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                closeWindow();
            }
        });

        // Cancel button: Esc also cancels
        cancelButton.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                closeWindow();
            }
        });
    }

    private void saveFlashcard() {
        String q = questionField.getText().trim();
        String a = answerField.getText().trim();

        if (!q.isEmpty() && !a.isEmpty()) {
            newFlashcard = new Flashcard(q, a);
            closeWindow();
        } else {
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

    // Pre-fill fields when editing
    public void setFlashcard(Flashcard flashcard) {
        if (flashcard != null) {
            questionField.setText(flashcard.getQuestion());
            answerField.setText(flashcard.getAnswer());
        }
    }
}