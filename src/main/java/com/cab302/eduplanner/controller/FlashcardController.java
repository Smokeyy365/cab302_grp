package com.cab302.eduplanner.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import com.cab302.eduplanner.App;
import com.cab302.eduplanner.model.Flashcard;
import com.cab302.eduplanner.model.FlashcardDeck;
import com.cab302.eduplanner.model.FlashcardFolder;
import com.cab302.eduplanner.repository.FlashcardRepository;

import com.cab302.eduplanner.service.GoogleDriveService;
import com.cab302.eduplanner.service.FlashcardExportService;
import com.cab302.eduplanner.service.FlashcardExportService.Card;

import java.io.File;
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
    @FXML private Button setupDriveButton, openDriveButton, forgetDriveButton;
    @FXML private Button exportDeckCsvButton, exportDeckPdfButton, exportDeckDriveButton;

    @FXML private TreeView<String> folderTree;
    @FXML private Label flashcardText, progressLabel;
    @FXML private ProgressBar progressBar;

    // ==== Data ====
    private final List<FlashcardFolder> folders = FlashcardRepository.getFolders();

    private FlashcardDeck currentDeck;
    private int currentIndex = 0;
    private boolean showingQuestion = true;
    private boolean finished = false;

    private Stage addFlashcardStage;
    private Stage editFlashcardStage;

    // Services (lazy-init to avoid FXMLLoader crashes if deps/OS features are missing)
    private GoogleDriveService driveService;
    private FlashcardExportService cardExport;

    // Default sample card text
    private static final String SAMPLE_TEXT = "Sample Flashcard\nCreate or select a folder & deck to begin.";

    @FXML
    public void initialize() {
        // Create services safely after FXML injection
        try {
            cardExport = new FlashcardExportService();
        } catch (Throwable t) {
            System.err.println("[Flashcards] PDF/CSV export disabled: " + t);
            cardExport = null;
        }
        try {
            driveService = new GoogleDriveService();
        } catch (Throwable t) {
            System.err.println("[Flashcards] Drive features disabled: " + t);
            driveService = null;
        }

        // Basic setup
        setupSidebar();
        setupButtons();

        // Add one default folder and deck if nothing exists yet
        if (folders.isEmpty()) {
            FlashcardFolder defaultFolder = new FlashcardFolder("Folder");
            FlashcardDeck defaultDeck = new FlashcardDeck("Deck");
            defaultDeck.getFlashcards().add(
                    new Flashcard("Question", "Answer")
            );
            defaultFolder.getDecks().add(defaultDeck);
            folders.add(defaultFolder);
        }

        refreshTree();

        // Show sample card by default
        flashcardText.setText(SAMPLE_TEXT);
        progressLabel.setText("Progress: 0/0");
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        // Disable buttons until valid deck selected
        setButtonsDisabled(true);
        updateDriveButtonsState();
    }

    // ==== Sidebar logic ====
    private void setupSidebar() {
        folderTree.setShowRoot(false);

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

        newFolderButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("New Folder");
            dialog.setHeaderText("Enter folder name:");
            dialog.showAndWait().ifPresent(name -> {
                folders.add(new FlashcardFolder(name));
                refreshTree();
            });
        });

        newDeckButton.setOnAction(e -> {
            TreeItem<String> selected = folderTree.getSelectionModel().getSelectedItem();

            if (selected == null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Create Deck");
                alert.setHeaderText(null);
                alert.setContentText("To create a deck, please create or select a folder first.");
                alert.showAndWait();
                return;
            }

            TreeItem<String> folderItem = (selected.getParent() == folderTree.getRoot())
                    ? selected
                    : selected.getParent();

            if (folderItem != null && folderItem.getParent() == folderTree.getRoot()) {
                TextInputDialog dialog = new TextInputDialog("New Deck");
                dialog.setHeaderText("Enter deck name:");
                dialog.showAndWait().ifPresent(name -> {
                    FlashcardFolder folder = getFolder(folderItem.getValue());
                    if (folder != null) {
                        folder.getDecks().add(new FlashcardDeck(name));
                        refreshTree();
                    }
                });
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Create Deck");
                alert.setHeaderText(null);
                alert.setContentText("Decks can only be created inside folders.");
                alert.showAndWait();
            }
        });

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
                });
            }
        });

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

    // ==== Button setup ====
    private void setupButtons() {
        nextButton.setOnAction(e -> nextFlashcard());
        prevButton.setOnAction(e -> prevFlashcard());
        flipButton.setOnAction(e -> flipFlashcard());
        shuffleButton.setOnAction(e -> shuffleFlashcards());
        resetButton.setOnAction(e -> resetDeck());
        finishButton.setOnAction(e -> finishDeck());

        dashboardButton.setOnAction(e -> {
            try {
                Stage stage = (Stage) dashboardButton.getScene().getWindow();
                App.changeScene(stage, "/com/cab302/eduplanner/dashboard.fxml", "EduPlanner — Dashboard");
            } catch (IOException ex) {
                System.err.println("Failed to switch to Dashboard: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        uploadButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Upload Flashcards");
            alert.setHeaderText(null);
            alert.setContentText("Upload functionality not yet implemented.");
            alert.showAndWait();
        });

        addButton.setOnAction(e -> openAddFlashcardDialog());
        editButton.setOnAction(e -> openEditFlashcardDialog());
        deleteButton.setOnAction(e -> deleteFlashcard());
    }

    // ==== Flashcard logic ====
    private void updateFlashcardView() {
        if (currentDeck == null) {
            flashcardText.setText(SAMPLE_TEXT);
            progressLabel.setText("Progress: 0/0");
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            setButtonsDisabled(true);
            return;
        }

        if (currentDeck.getFlashcards().isEmpty()) {
            flashcardText.setText("This deck is empty. Add flashcards to get started.");
            progressLabel.setText("Progress: 0/0");
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            setButtonsDisabled(true);
            addButton.setDisable(false);
            if (uploadButton != null) uploadButton.setDisable(!hasAnyFlashcards());
            return;
        }

        Flashcard current = currentDeck.getFlashcards().get(currentIndex);
        flashcardText.setText(showingQuestion ? current.getQuestion() : current.getAnswer());
        progressLabel.setText("Progress: " + (currentIndex + 1) + "/" + currentDeck.getFlashcards().size());
        progressBar.setProgress((double) (currentIndex + 1) / currentDeck.getFlashcards().size());

        prevButton.setDisable(currentIndex == 0 || finished);
        nextButton.setDisable(finished);
        flipButton.setDisable(finished);
        shuffleButton.setDisable(finished);
        resetButton.setDisable(finished);
        finishButton.setDisable(finished);
        addButton.setDisable(finished);
        editButton.setDisable(finished);
        deleteButton.setDisable(finished);
        if (uploadButton != null) uploadButton.setDisable(!hasAnyFlashcards());
    }

    private void nextFlashcard() {
        if (currentDeck == null) return;
        if (currentIndex < currentDeck.getFlashcards().size() - 1) {
            currentIndex++;
            showingQuestion = true;
            updateFlashcardView();
        } else {
            flashcardText.setText("All cards complete!");
            progressLabel.setText("Progress: " + currentDeck.getFlashcards().size() + "/" + currentDeck.getFlashcards().size());
            progressBar.setProgress(1.0);
            finished = true;
        }
    }

    private void prevFlashcard() {
        if (currentDeck == null || finished) return;
        if (currentIndex > 0) {
            currentIndex--;
            showingQuestion = true;
            updateFlashcardView();
        }
    }

    private void flipFlashcard() {
        if (currentDeck == null || finished) return;
        showingQuestion = !showingQuestion;
        updateFlashcardView();
    }

    private void shuffleFlashcards() {
        if (currentDeck == null) return;
        Collections.shuffle(currentDeck.getFlashcards());
        currentIndex = 0;
        showingQuestion = true;
        finished = false;
        updateFlashcardView();
    }

    private void resetDeck() {
        if (currentDeck == null) return;
        currentIndex = 0;
        showingQuestion = true;
        finished = false;
        updateFlashcardView();
    }

    private void finishDeck() {
        if (currentDeck == null) return;
        flashcardText.setText("You ended the deck early!");
        progressLabel.setText("Progress: " + (currentIndex + 1) + "/" + currentDeck.getFlashcards().size());
        progressBar.setProgress(1.0);
        finished = true;
    }

    // ==== CRUD ====
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
            ex.printStackTrace();
        }
    }

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
            ex.printStackTrace();
        }
    }

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

    private boolean hasAnyFlashcards() {
        return folders.stream()
                .flatMap(f -> f.getDecks().stream())
                .anyMatch(d -> !d.getFlashcards().isEmpty());
    }

    private void setButtonsDisabled(boolean disabled) {
        prevButton.setDisable(disabled);
        nextButton.setDisable(disabled);
        flipButton.setDisable(disabled);
        shuffleButton.setDisable(disabled);
        resetButton.setDisable(disabled);
        finishButton.setDisable(disabled);
        addButton.setDisable(disabled);
        editButton.setDisable(disabled);
        deleteButton.setDisable(disabled);
        if (uploadButton != null) {
            uploadButton.setDisable(disabled || !hasAnyFlashcards());
        }
    }

    // ==== Google Drive Export Methods ====
    private void updateDriveButtonsState() {
        boolean hasDrive = driveService != null && driveService.getSavedDriveFolder() != null;
        if (openDriveButton != null) openDriveButton.setDisable(!hasDrive);
        if (forgetDriveButton != null) forgetDriveButton.setDisable(!hasDrive);
    }

    private static record DeckExport(String name, List<Card> cards) {}

    private DeckExport gatherActiveDeck() {
        FlashcardDeck deck = currentDeck;

        if (deck == null) {
            outer:
            for (FlashcardFolder f : folders) {
                for (FlashcardDeck d : f.getDecks()) {
                    if (!d.getFlashcards().isEmpty()) { deck = d; break outer; }
                }
            }
        }

        String name = deck == null ? "Deck" : (deck.getName() == null || deck.getName().isBlank() ? "Deck" : deck.getName());
        List<Card> cards = new ArrayList<>();

        if (deck != null) {
            for (Flashcard fc : deck.getFlashcards()) {
                cards.add(new Card(fc.getQuestion(), fc.getAnswer()));
            }
        }

        if (cards.isEmpty()) {
            cards.add(new Card("What is encapsulation?", "Bundling data with methods that operate on it."));
            cards.add(new Card("Big-O of binary search?", "O(log n)"));
        }

        return new DeckExport(name, cards);
    }

    @FXML
    private void handleSetupDriveFolder() {
        if (driveService == null) {
            new Alert(Alert.AlertType.ERROR, "Drive features unavailable (service failed to initialise).").showAndWait();
            return;
        }
        var window = (setupDriveButton != null ? setupDriveButton.getScene().getWindow() : null);
        File base = driveService.pickAndSetupEduPlanner(window);
        if (base != null) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Drive sync folder set:\n" + base.getAbsolutePath() + "\n\nSubfolders created:\n- Notes\n- Flashcards").showAndWait();
        }
        updateDriveButtonsState();
    }

    @FXML
    private void handleOpenDriveFolder() {
        if (driveService == null) {
            new Alert(Alert.AlertType.ERROR, "Drive features unavailable.").showAndWait();
            return;
        }
        var base = driveService.getSavedDriveFolder();
        if (base == null || !base.isDirectory()) {
            new Alert(Alert.AlertType.INFORMATION, "No Drive folder set. Click 'Setup Drive Sync'.").showAndWait();
            return;
        }
        try { if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(base); }
        catch (Exception e) { new Alert(Alert.AlertType.ERROR, "Couldn't open folder:\n" + e.getMessage()).showAndWait(); }
    }

    @FXML
    private void handleForgetDriveFolder() {
        if (driveService == null) {
            new Alert(Alert.AlertType.ERROR, "Drive features unavailable.").showAndWait();
            return;
        }
        driveService.clearSavedDriveFolder();
        new Alert(Alert.AlertType.INFORMATION, "Drive sync folder cleared. Run 'Setup Drive Sync' again.").showAndWait();
        updateDriveButtonsState();
    }

    @FXML
    private void handleExportDeckCsv() {
        if (cardExport == null) {
            new Alert(Alert.AlertType.ERROR, "Export service unavailable.").showAndWait();
            return;
        }
        DeckExport deck = gatherActiveDeck();

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose folder to save CSV");
        File dir = chooser.showDialog(exportDeckCsvButton != null ? exportDeckCsvButton.getScene().getWindow() : null);
        if (dir == null) return;

        try {
            File file = cardExport.exportCsv(deck.name(), deck.cards(), dir);
            new Alert(Alert.AlertType.INFORMATION, "Saved:\n" + file.getAbsolutePath()).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleExportDeckPdf() {
        if (cardExport == null) {
            new Alert(Alert.AlertType.ERROR, "Export service unavailable.").showAndWait();
            return;
        }
        DeckExport deck = gatherActiveDeck();

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose folder to save PDF");
        File dir = chooser.showDialog(exportDeckPdfButton != null ? exportDeckPdfButton.getScene().getWindow() : null);
        if (dir == null) return;

        try {
            File file = cardExport.exportPdf(deck.name(), deck.cards(), dir);
            new Alert(Alert.AlertType.INFORMATION, "Saved:\n" + file.getAbsolutePath()).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleExportDeckToDrive() {
        if (cardExport == null) {
            new Alert(Alert.AlertType.ERROR, "Export service unavailable.").showAndWait();
            return;
        }
        if (driveService == null) {
            new Alert(Alert.AlertType.ERROR, "Drive features unavailable.").showAndWait();
            return;
        }
        DeckExport deck = gatherActiveDeck();

        if (driveService.getSavedDriveFolder() == null) {
            handleSetupDriveFolder();
            if (driveService.getSavedDriveFolder() == null) return;
        }

        try {
            File temp = cardExport.exportPdf(deck.name(), deck.cards(), new File(System.getProperty("java.io.tmpdir")));
            File fcFolder = driveService.ensureSubfolder("Flashcards");
            File copied = cardExport.copyToFolder(temp, fcFolder);
            new Alert(Alert.AlertType.INFORMATION, "Exported to Drive:\n" + copied.getAbsolutePath()).showAndWait();
            try { if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(fcFolder); } catch (Exception ignore) {}
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Drive export failed:\n" + ex.getMessage()).showAndWait();
        }
    }
}
