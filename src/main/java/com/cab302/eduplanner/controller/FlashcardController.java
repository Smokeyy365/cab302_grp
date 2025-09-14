package com.cab302.eduplanner.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;

import com.cab302.eduplanner.App;
import com.cab302.eduplanner.model.Flashcard;

import java.util.*;

public class FlashcardController {

    // UI elements from flashcard.fxml
    @FXML private Button dashboardButton;
    @FXML private Button prevButton, nextButton;
    @FXML private Button shuffleButton, flipButton;
    @FXML private Button resetButton, finishButton;
    @FXML private Button addButton, editButton, deleteButton; // new
    @FXML private Button uploadButton;
    @FXML private StackPane flashcardPane;
    @FXML private Label flashcardText, progressLabel;
    @FXML private ProgressBar progressBar;

    // Temporary in-memory list of flashcards (replace with DB/repo later)
    private final List<Flashcard> flashcards = new ArrayList<>();
    private int currentIndex = 0;
    private boolean showingQuestion = true;
    private boolean finished = false;

    // Keep popup stages so multiple aren't spawned
    private Stage addFlashcardStage;
    private Stage editFlashcardStage;

    @FXML
    public void initialize() {
        // mock data so UI works for now
        flashcards.addAll(List.of(
                new Flashcard("What is 2 + 2", "4"),
                new Flashcard("Capital of France?", "Paris"),
                new Flashcard("McLaren Number 1 Driver?", "Oscar Piastri")
        ));

        // button actions
        nextButton.setOnAction(e -> nextFlashcard());
        prevButton.setOnAction(e -> prevFlashcard());
        flipButton.setOnAction(e -> flipFlashcard());
        shuffleButton.setOnAction(e -> shuffleFlashcards());
        resetButton.setOnAction(e -> resetDeck());
        finishButton.setOnAction(e -> finishDeck());

        // temporary link from home button to dashboard
        dashboardButton.setOnAction(e -> {
            try {
                Stage stage = (Stage) dashboardButton.getScene().getWindow();
                App.changeScene(stage, "/com/cab302/eduplanner/dashboard.fxml", "EduPlanner — Dashboard");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        addButton.setOnAction(e -> openAddFlashcardDialog());
        editButton.setOnAction(e -> openEditFlashcardDialog());
        deleteButton.setOnAction(e -> deleteFlashcard());

        // Upload button
        uploadButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Upload Flashcards");
            alert.setHeaderText(null);
            alert.setContentText("Upload functionality not yet implemented");
            alert.showAndWait();
        });

        updateFlashcardView();
    }

    // Update card text, progress, and button states
    private void updateFlashcardView() {
        if (flashcards.isEmpty()) {
            flashcardText.setText("No flashcards yet.");
            progressLabel.setText("Progress: 0/0");
            progressBar.setProgress(0);

            nextButton.setDisable(true);
            prevButton.setDisable(true);
            flipButton.setDisable(true);
            editButton.setDisable(true);
            deleteButton.setDisable(true);
            return;
        }

        Flashcard current = flashcards.get(currentIndex);
        flashcardText.setText(showingQuestion ? current.getQuestion() : current.getAnswer());
        progressLabel.setText("Progress: " + (currentIndex + 1) + "/" + flashcards.size());
        progressBar.setProgress((double) (currentIndex + 1) / flashcards.size());

        prevButton.setDisable(currentIndex == 0 || finished);
        nextButton.setDisable(finished);
        flipButton.setDisable(finished);
        editButton.setDisable(finished);
        deleteButton.setDisable(finished);
    }

    private void nextFlashcard() {
        if (flashcards.isEmpty()) return;

        if (currentIndex < flashcards.size() - 1) {
            currentIndex++;
            showingQuestion = true;
            updateFlashcardView();
        } else {
            flashcardText.setText("🎉 All cards complete!");
            progressLabel.setText("Progress: " + flashcards.size() + "/" + flashcards.size());
            progressBar.setProgress(1.0);
            finished = true;

            nextButton.setDisable(true);
            prevButton.setDisable(true);
            flipButton.setDisable(true);
            editButton.setDisable(true);
            deleteButton.setDisable(true);
        }
    }

    private void prevFlashcard() {
        if (flashcards.isEmpty() || finished) return;
        if (currentIndex > 0) {
            currentIndex--;
            showingQuestion = true;
            updateFlashcardView();
        }
    }

    private void flipFlashcard() {
        if (finished) return;
        showingQuestion = !showingQuestion;
        updateFlashcardView();
    }

    private void shuffleFlashcards() {
        if (flashcards.isEmpty()) return;
        Collections.shuffle(flashcards);
        currentIndex = 0;
        showingQuestion = true;
        finished = false;
        updateFlashcardView();
    }

    private void resetDeck() {
        if (flashcards.isEmpty()) return;
        currentIndex = 0;
        showingQuestion = true;
        finished = false;
        updateFlashcardView();
    }

    private void finishDeck() {
        if (flashcards.isEmpty()) return;
        flashcardText.setText("🎉 You ended the deck early!");
        progressLabel.setText("Progress: " + (currentIndex + 1) + "/" + flashcards.size());
        progressBar.setProgress(1.0);
        finished = true;

        nextButton.setDisable(true);
        prevButton.setDisable(true);
        flipButton.setDisable(true);
        editButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    // Add
    private void openAddFlashcardDialog() {
        try {
            if (addFlashcardStage == null || !addFlashcardStage.isShowing()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cab302/eduplanner/add-flashcard.fxml"));
                Parent root = loader.load();

                addFlashcardStage = new Stage();
                addFlashcardStage.setTitle("Add Flashcard");
                addFlashcardStage.setScene(new Scene(root));
                addFlashcardStage.setOnHidden(e -> addFlashcardStage = null); // reset when closed
                addFlashcardStage.showAndWait();

                AddFlashcardController controller = loader.getController();
                Flashcard newCard = controller.getNewFlashcard();
                if (newCard != null) {
                    flashcards.add(newCard);
                    finished = false;
                    updateFlashcardView();
                }
            } else {
                addFlashcardStage.toFront();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    // Edit
    private void openEditFlashcardDialog() {
        if (flashcards.isEmpty() || finished) return;
        try {
            if (editFlashcardStage == null || !editFlashcardStage.isShowing()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cab302/eduplanner/add-flashcard.fxml"));
                Parent root = loader.load();

                AddFlashcardController controller = loader.getController();
                Flashcard current = flashcards.get(currentIndex);
                controller.setFlashcard(current); // pre-fill fields

                editFlashcardStage = new Stage();
                editFlashcardStage.setTitle("Edit Flashcard");
                editFlashcardStage.setScene(new Scene(root));
                editFlashcardStage.setOnHidden(e -> editFlashcardStage = null); // reset when closed
                editFlashcardStage.showAndWait();

                Flashcard updated = controller.getNewFlashcard();
                if (updated != null) {
                    flashcards.set(currentIndex, updated);
                    showingQuestion = true;
                    finished = false;
                    updateFlashcardView();
                }
            } else {
                editFlashcardStage.toFront();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    // Delete
    private void deleteFlashcard() {
        if (flashcards.isEmpty() || finished) return;

        Flashcard current = flashcards.get(currentIndex);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Flashcard");
        alert.setHeaderText("Are you sure you want to delete this flashcard?");
        alert.setContentText(
                "Question: " + current.getQuestion() + "\n" +
                        "Answer: " + current.getAnswer()
        );

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            flashcards.remove(currentIndex);

            if (flashcards.isEmpty()) {
                updateFlashcardView();
                return;
            }

            if (currentIndex >= flashcards.size()) {
                currentIndex = flashcards.size() - 1;
            }

            showingQuestion = true;
            updateFlashcardView();
        }
    }
}
