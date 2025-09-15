package com.cab302.eduplanner.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;

import com.cab302.eduplanner.App;
import com.cab302.eduplanner.model.Flashcard;
import com.cab302.eduplanner.model.FlashcardDeck;
import com.cab302.eduplanner.model.FlashcardFolder;
import com.cab302.eduplanner.repository.FlashcardRepository;

import java.io.IOException;
import java.util.*;

public class FlashcardController {

    // ==== UI Elements (linked from flashcard.fxml) ====
    @FXML private Button dashboardButton, uploadButton;
    @FXML private Button prevButton, nextButton;
    @FXML private Button shuffleButton, flipButton;
    @FXML private Button resetButton, finishButton;
    @FXML private Button addButton, editButton, deleteButton;
    @FXML private Button newFolderButton, newDeckButton, renameButton, deleteFolderDeckButton;

    @FXML private TreeView<String> folderTree;
    @FXML private Label flashcardText, progressLabel;
    @FXML private ProgressBar progressBar;

    // ==== Data ====
    // Shared repository so data persists across scene switches
    private final List<FlashcardFolder> folders = FlashcardRepository.getFolders();

    // Current working state
    private FlashcardDeck currentDeck;
    private int currentIndex = 0;
    private boolean showingQuestion = true;
    private boolean finished = false;

    // Popup dialog stages (to prevent duplicates)
    private Stage addFlashcardStage;
    private Stage editFlashcardStage;

    @FXML
    public void initialize() {
        setupSidebar();
        setupButtons();

        // Add demo folder/deck only once, for testing
        if (folders.isEmpty()) {
            FlashcardFolder demoFolder = new FlashcardFolder("CAB302");
            FlashcardDeck week6 = new FlashcardDeck("Week 6");
            week6.getFlashcards().addAll(List.of(
                    new Flashcard("What is 2+2?", "4"),
                    new Flashcard("Capital of France?", "Paris")
            ));
            demoFolder.getDecks().add(week6);
            folders.add(demoFolder);
        }

        refreshTree();

        updateFlashcardView();
    }

    // ==== Sidebar logic (folders + decks) ====
    private void setupSidebar() {
        folderTree.setShowRoot(false);

        // When user selects a deck in the tree, load it
        folderTree.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null && newSel.getParent() != null) {
                currentDeck = getDeck(newSel.getParent().getValue(), newSel.getValue());
                currentIndex = 0;
                showingQuestion = true;
                finished = false;
                updateFlashcardView();
            } else {
                currentDeck = null;
                updateFlashcardView();
            }
        });

        // Create new folder
        newFolderButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("New Folder");
            dialog.setHeaderText("Enter folder name:");
            dialog.showAndWait().ifPresent(name -> {
                folders.add(new FlashcardFolder(name));
                refreshTree();
            });
        });

        // Create new deck under selected folder
        newDeckButton.setOnAction(e -> {
            TreeItem<String> selected = folderTree.getSelectionModel().getSelectedItem();

            // If nothing selected → show popup
            if (selected == null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Create Deck");
                alert.setHeaderText(null);
                alert.setContentText("To create a deck, please create or select a folder first.");
                alert.showAndWait();
                return;
            }

            // If they clicked a deck, go up to its parent folder
            TreeItem<String> folderItem = (selected.getParent() == folderTree.getRoot())
                    ? selected
                    : selected.getParent();

            // Only allow decks under folders
            if (folderItem != null && folderItem.getParent() == folderTree.getRoot()) {
                TextInputDialog dialog = new TextInputDialog("New Deck");
                dialog.setHeaderText("Enter deck name:");
                dialog.showAndWait().ifPresent(name -> {
                    FlashcardFolder folder = getFolder(folderItem.getValue());
                    if (folder != null) {
                        folder.getDecks().add(new FlashcardDeck(name));
                        refreshTree();

                        // Expand and select new deck
                        for (TreeItem<String> fItem : folderTree.getRoot().getChildren()) {
                            if (fItem.getValue().equals(folder.getName())) {
                                fItem.setExpanded(true);
                                for (TreeItem<String> dItem : fItem.getChildren()) {
                                    if (dItem.getValue().equals(name)) {
                                        folderTree.getSelectionModel().select(dItem);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                });
            } else {
                // Safety: show message if clicked something invalid
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Create Deck");
                alert.setHeaderText(null);
                alert.setContentText("Decks can only be created inside folders.");
                alert.showAndWait();
            }
        });

        // Rename folder or deck
        renameButton.setOnAction(e -> {
            TreeItem<String> selected = folderTree.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String oldValue = selected.getValue();
                String parentValue = (selected.getParent() != null) ? selected.getParent().getValue() : null;

                TextInputDialog dialog = new TextInputDialog(oldValue);
                dialog.setHeaderText("Enter new name:");
                dialog.showAndWait().ifPresent(newName -> {
                    if (selected.getParent() == folderTree.getRoot()) {
                        FlashcardFolder folder = getFolder(oldValue);
                        if (folder != null) folder.setName(newName);
                    } else {
                        FlashcardDeck deck = getDeck(parentValue, oldValue);
                        if (deck != null) deck.setName(newName);
                    }
                    refreshTree();

                    // Re-expand and reselect renamed item
                    TreeItem<String> root = folderTree.getRoot();
                    for (TreeItem<String> fItem : root.getChildren()) {
                        if (fItem.getValue().equals(parentValue != null ? parentValue : newName)) {
                            fItem.setExpanded(true);
                            if (parentValue != null) {
                                for (TreeItem<String> dItem : fItem.getChildren()) {
                                    if (dItem.getValue().equals(newName)) {
                                        folderTree.getSelectionModel().select(dItem);
                                        break;
                                    }
                                }
                            } else {
                                folderTree.getSelectionModel().select(fItem);
                            }
                            break;
                        }
                    }
                });
            }
        });

        // Delete folder or deck
        deleteFolderDeckButton.setOnAction(e -> {
            TreeItem<String> selected = folderTree.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (selected.getParent() == folderTree.getRoot()) {
                    folders.removeIf(f -> f.getName().equals(selected.getValue()));
                } else {
                    FlashcardFolder folder = getFolder(selected.getParent().getValue());
                    if (folder != null) {
                        folder.getDecks().removeIf(d -> d.getName().equals(selected.getValue()));
                    }
                }
                refreshTree();
            }
        });
    }

    // ==== Button actions (main controls) ====
    private void setupButtons() {
        nextButton.setOnAction(e -> nextFlashcard());
        prevButton.setOnAction(e -> prevFlashcard());
        flipButton.setOnAction(e -> flipFlashcard());
        shuffleButton.setOnAction(e -> shuffleFlashcards());
        resetButton.setOnAction(e -> resetDeck());
        finishButton.setOnAction(e -> finishDeck());

        // Navigation buttons
        dashboardButton.setOnAction(e -> {
            try {
                Stage stage = (Stage) dashboardButton.getScene().getWindow();
                App.changeScene(stage, "/com/cab302/eduplanner/dashboard.fxml", "EduPlanner — Dashboard");
            } catch (IOException ex) {
                System.err.println("Failed to switch to Dashboard scene: " + ex.getMessage());
            }
        });

        uploadButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Upload Flashcards");
            alert.setHeaderText(null);
            alert.setContentText("Upload functionality not yet implemented");
            alert.showAndWait();
        });

        // CRUD operations
        addButton.setOnAction(e -> openAddFlashcardDialog());       // Create
        editButton.setOnAction(e -> openEditFlashcardDialog());     // Update
        deleteButton.setOnAction(e -> deleteFlashcard());           // Delete
    }

    // ==== Flashcard navigation logic ====
    private void updateFlashcardView() {
        // Case 1: No deck selected
        if (currentDeck == null) {
            flashcardText.setText("Select a folder and deck to view flashcards.");
            progressLabel.setText("Progress: 0/0");

            // Show animated indeterminate bar
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

            // Disable all actions
            nextButton.setDisable(true);
            prevButton.setDisable(true);
            flipButton.setDisable(true);
            shuffleButton.setDisable(true);
            resetButton.setDisable(true);
            finishButton.setDisable(true);
            addButton.setDisable(true);
            editButton.setDisable(true);
            deleteButton.setDisable(true);
            return;
        }

        // Case 2: Deck selected but empty
        if (currentDeck.getFlashcards().isEmpty()) {
            flashcardText.setText("This deck is empty. Add flashcards to get started.");
            progressLabel.setText("Progress: 0/0");

            // Show animated indeterminate bar
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

            // Disable nav/edit/reset/shuffle/finish, but allow Add
            nextButton.setDisable(true);
            prevButton.setDisable(true);
            flipButton.setDisable(true);
            shuffleButton.setDisable(true);
            resetButton.setDisable(true);
            finishButton.setDisable(true);
            addButton.setDisable(false); // let them add cards
            editButton.setDisable(true);
            deleteButton.setDisable(true);
            return;
        }

        // Case 3: Normal deck with cards
        Flashcard current = currentDeck.getFlashcards().get(currentIndex);
        flashcardText.setText(showingQuestion ? current.getQuestion() : current.getAnswer());
        progressLabel.setText("Progress: " + (currentIndex + 1) + "/" + currentDeck.getFlashcards().size());
        progressBar.setProgress((double) (currentIndex + 1) / currentDeck.getFlashcards().size());

        // Enable/disable buttons depending on state
        prevButton.setDisable(currentIndex == 0 || finished);
        nextButton.setDisable(finished);
        flipButton.setDisable(finished);
        shuffleButton.setDisable(finished);
        resetButton.setDisable(finished);
        finishButton.setDisable(finished);
        addButton.setDisable(finished);
        editButton.setDisable(finished);
        deleteButton.setDisable(finished);
    }


    // Next flashcard
    private void nextFlashcard() {
        if (currentDeck == null) return;
        if (currentIndex < currentDeck.getFlashcards().size() - 1) {
            currentIndex++;
            showingQuestion = true;
            updateFlashcardView();
        } else {
            flashcardText.setText("🎉 All cards complete!");
            progressLabel.setText("Progress: " + currentDeck.getFlashcards().size() + "/" + currentDeck.getFlashcards().size());
            progressBar.setProgress(1.0);
            finished = true;
        }
    }

    // Previous flashcard
    private void prevFlashcard() {
        if (currentDeck == null || finished) return;
        if (currentIndex > 0) {
            currentIndex--;
            showingQuestion = true;
            updateFlashcardView();
        }
    }

    // Flip flashcard
    private void flipFlashcard() {
        if (currentDeck == null || finished) return;
        showingQuestion = !showingQuestion;
        updateFlashcardView();
    }

    // Shuffle flashcard
    private void shuffleFlashcards() {
        if (currentDeck == null) return;
        Collections.shuffle(currentDeck.getFlashcards());
        currentIndex = 0;
        showingQuestion = true;
        finished = false;
        updateFlashcardView();
    }

    // Reset deck
    private void resetDeck() {
        if (currentDeck == null) return;
        currentIndex = 0;
        showingQuestion = true;
        finished = false;
        updateFlashcardView();
    }

    // Finish deck
    private void finishDeck() {
        if (currentDeck == null) return;
        flashcardText.setText("🎉 You ended the deck early!");
        progressLabel.setText("Progress: " + (currentIndex + 1) + "/" + currentDeck.getFlashcards().size());
        progressBar.setProgress(1.0);
        finished = true;
    }

    // ==== CRUD flashcards ====
    // Add flashcard
    private void openAddFlashcardDialog() {
        if (currentDeck == null) return;
        try {
            if (addFlashcardStage == null || !addFlashcardStage.isShowing()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cab302/eduplanner/add-flashcard.fxml"));
                Parent root = loader.load();

                addFlashcardStage = new Stage();
                addFlashcardStage.setTitle("Add Flashcard");
                addFlashcardStage.setScene(new Scene(root));
                addFlashcardStage.setOnHidden(e -> addFlashcardStage = null);
                addFlashcardStage.showAndWait();

                AddFlashcardController controller = loader.getController();
                Flashcard newCard = controller.getNewFlashcard();
                if (newCard != null) {
                    currentDeck.getFlashcards().add(newCard);
                    finished = false;
                    updateFlashcardView();
                }
            } else {
                addFlashcardStage.toFront();
            }
        } catch (Exception ex) {
            System.err.println("Error while opening Add Flashcard dialog: " + ex.getMessage());
        }
    }

    // Edit flashcard
    private void openEditFlashcardDialog() {
        if (currentDeck == null || finished) return;
        try {
            if (editFlashcardStage == null || !editFlashcardStage.isShowing()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cab302/eduplanner/add-flashcard.fxml"));
                Parent root = loader.load();

                AddFlashcardController controller = loader.getController();
                Flashcard current = currentDeck.getFlashcards().get(currentIndex);
                controller.setFlashcard(current);

                editFlashcardStage = new Stage();
                editFlashcardStage.setTitle("Edit Flashcard");
                editFlashcardStage.setScene(new Scene(root));
                editFlashcardStage.setOnHidden(e -> editFlashcardStage = null);
                editFlashcardStage.showAndWait();

                Flashcard updated = controller.getNewFlashcard();
                if (updated != null) {
                    currentDeck.getFlashcards().set(currentIndex, updated);
                    showingQuestion = true;
                    finished = false;
                    updateFlashcardView();
                }
            } else {
                editFlashcardStage.toFront();
            }
        } catch (Exception ex) {
            System.err.println("Error while opening Edit Flashcard dialog: " + ex.getMessage());
        }
    }

    // Delete flashcard
    private void deleteFlashcard() {
        if (currentDeck == null || finished) return;

        Flashcard current = currentDeck.getFlashcards().get(currentIndex);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Flashcard");
        alert.setHeaderText("Are you sure you want to delete this flashcard?");
        alert.setContentText("Question: " + current.getQuestion() + "\nAnswer: " + current.getAnswer());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            currentDeck.getFlashcards().remove(currentIndex);
            if (currentDeck.getFlashcards().isEmpty()) {
                updateFlashcardView();
                return;
            }
            if (currentIndex >= currentDeck.getFlashcards().size()) {
                currentIndex = currentDeck.getFlashcards().size() - 1;
            }
            showingQuestion = true;
            updateFlashcardView();
        }
    }

    // ==== Helpers ====
    private void refreshTree() {
        // Rebuild the sidebar tree from model
        TreeItem<String> root = new TreeItem<>("Root");
        for (FlashcardFolder folder : folders) {
            TreeItem<String> folderItem = new TreeItem<>(folder.getName());
            for (FlashcardDeck deck : folder.getDecks()) {
                folderItem.getChildren().add(new TreeItem<>(deck.getName()));
            }
            root.getChildren().add(folderItem);
        }
        folderTree.setRoot(root);
    }

    private FlashcardFolder getFolder(String name) {
        return folders.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
    }

    private FlashcardDeck getDeck(String folderName, String deckName) {
        FlashcardFolder folder = getFolder(folderName);
        if (folder == null) return null;
        return folder.getDecks().stream().filter(d -> d.getName().equals(deckName)).findFirst().orElse(null);
    }
}
