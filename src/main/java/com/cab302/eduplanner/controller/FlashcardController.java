package com.cab302.eduplanner.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import com.cab302.eduplanner.model.Flashcard;

import java.util.*;

public class FlashcardController {

    // UI elements from flashcard.fxml
    @FXML private Button homeButton, manageButton;
    @FXML private Button prevButton, nextButton;
    @FXML private Button shuffleButton, flipButton;
    @FXML private Button resetButton, finishButton;
    @FXML private StackPane flashcardPane;
    @FXML private Label flashcardText, progressLabel;
    @FXML private ProgressBar progressBar;

    // Temporary in-memory list of flashcards (replace with DB/repo later)
    private final List<Flashcard> flashcards = new ArrayList<>();
    private int currentIndex = 0;          // index of current card
    private boolean showingQuestion = true; // true = question, false = answer
    private boolean finished = false;      // track if deck is completed

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

        // placeholders for future integration
        homeButton.setOnAction(e -> System.out.println("TODO: Navigate Home"));
        manageButton.setOnAction(e -> System.out.println("TODO: Open Add/Edit/Delete dialog"));

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
            return;
        }

        Flashcard current = flashcards.get(currentIndex);
        flashcardText.setText(showingQuestion ? current.getQuestion() : current.getAnswer());
        progressLabel.setText("Progress: " + (currentIndex + 1) + "/" + flashcards.size());
        progressBar.setProgress((double) (currentIndex + 1) / flashcards.size());

        // grey out buttons depending on state
        prevButton.setDisable(currentIndex == 0 || finished);
        nextButton.setDisable(finished);
        flipButton.setDisable(finished);
    }

    // Next card, or show complete message if at end
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
        }
    }

    // Previous card, unless at start or deck finished
    private void prevFlashcard() {
        if (flashcards.isEmpty() || finished) return;
        if (currentIndex > 0) {
            currentIndex--;
            showingQuestion = true;
            updateFlashcardView();
        }
    }

    // Flip between question and answer
    private void flipFlashcard() {
        if (finished) return;
        showingQuestion = !showingQuestion;
        updateFlashcardView();
    }

    // Shuffle deck order and restart
    private void shuffleFlashcards() {
        if (flashcards.isEmpty()) return;
        Collections.shuffle(flashcards);
        currentIndex = 0;
        showingQuestion = true;
        finished = false;
        updateFlashcardView();
    }

    // Reset to start of deck
    private void resetDeck() {
        if (flashcards.isEmpty()) return;
        currentIndex = 0;
        showingQuestion = true;
        finished = false;
        updateFlashcardView();
    }

    // End deck early and lock controls
    private void finishDeck() {
        if (flashcards.isEmpty()) return;
        flashcardText.setText("🎉 You ended the deck early!");
        progressLabel.setText("Progress: " + (currentIndex + 1) + "/" + flashcards.size());
        progressBar.setProgress(1.0);

        finished = true;

        nextButton.setDisable(true);
        prevButton.setDisable(true);
        flipButton.setDisable(true);
    }
}
