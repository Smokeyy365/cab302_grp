package com.cab302.eduplanner.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import com.cab302.eduplanner.model.Flashcard;

import java.util.*;

public class FlashcardController {

//    Ui elements from flashcard.fxml
    @FXML private Button homeButton, manageButton;
    @FXML private Button prevButton, nextButton;
    @FXML private Button shuffleButton, flipButton;
    @FXML private Button resetButton, finishButton;
    @FXML private StackPane flashcardPane;
    @FXML private Label flashcardText, progressLabel;
    @FXML private ProgressBar progressBar;

//    Mock flashcard list (replace with service/repo later)
    private final List<Flashcard> flashcards = new ArrayList<>();
    private int currentIndex = 0;
    private boolean showingQuestion = true;

    private boolean finished = false;

    @FXML
    public void initialize() {
//        Mock data so UI works now
        flashcards.addAll(List.of(
                new Flashcard("What is 2 + 2", "4"),
                new Flashcard("Capital of France?", "Paris"),
                new Flashcard("McLaren Number 1 Driver?", "Oscar Piastri")
        ));

//        Button actions
        nextButton.setOnAction(e -> nextFlashcard());
        prevButton.setOnAction(e -> prevFlashcard());
        flipButton.setOnAction(e -> flipFlashcard());
        shuffleButton.setOnAction(e -> shuffleFlashcards());
        resetButton.setOnAction(e -> resetDeck());
        finishButton.setOnAction(e -> finishDeck());

//        Placeholders for future
        homeButton.setOnAction(e -> System.out.println("TODO: Navigate Home"));
        manageButton.setOnAction(e -> System.out.println("TODO: Open Add/Edit/Delete dialog"));

        updateFlashcardView();
    }

    private void updateFlashcardView(){
        if (flashcards.isEmpty()) {
            flashcardText.setText("No flashcards yet.");
            progressLabel.setText("Progress: 0/0");
            progressBar.setProgress(0);
            return;
        }
        Flashcard current = flashcards.get(currentIndex);
        flashcardText.setText(showingQuestion ? current.getQuestion() : current.getAnswer());
        progressLabel.setText("Progress: " + (currentIndex + 1) + "/" + flashcards.size());
        progressBar.setProgress((double) (currentIndex + 1) / flashcards.size());
    }

    private void nextFlashcard() {
        if (flashcards.isEmpty()) return;

        if (currentIndex < flashcards.size() - 1) {
            currentIndex++;
            showingQuestion = true;
            updateFlashcardView();
        }
        else {
            // Show "All done!" state
            flashcardText.setText("🎉 All cards complete!");
            progressLabel.setText("Progress: " + flashcards.size() + "/" + flashcards.size());
            progressBar.setProgress(1.0);

            // Disable Next button until reset
            nextButton.setDisable(true);
            prevButton.setDisable(true);
            finished = true;
        }
    }

    private void prevFlashcard() {
        if (flashcards.isEmpty() || finished) return;   // block going back if finished
        currentIndex = (currentIndex - 1 + flashcards.size()) % flashcards.size();
        showingQuestion = true;
        updateFlashcardView();
    }

    private void flipFlashcard() {
        if (finished) return;   // don't flip once done
        showingQuestion = !showingQuestion;
        updateFlashcardView();
    }

    private void shuffleFlashcards() {
        if (flashcards.isEmpty()) return;
        Collections.shuffle(flashcards);
        currentIndex = 0;
        showingQuestion = true;
        updateFlashcardView();
    }

    private void resetDeck() {
        if (flashcards.isEmpty()) return;
        currentIndex = 0;
        showingQuestion = true;

//      Re-enable navigation
        nextButton.setDisable(false);
        prevButton.setDisable(false);
        finished = false;

        updateFlashcardView();
    }

    private void finishDeck() {
        if (flashcards.isEmpty()) return;
        flashcardText.setText("🎉 You ended the deck early!");
        progressLabel.setText("Progress: " + (currentIndex + 1) + "/" + flashcards.size());
        progressBar.setProgress(1.0);
        nextButton.setDisable(true);
    }
}
