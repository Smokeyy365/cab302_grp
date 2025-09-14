package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.App;
import com.cab302.eduplanner.model.Note;
import com.cab302.eduplanner.model.Folder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.io.IOException;

public class NoteController {

    // Sidebar
    @FXML private TreeView<Object> folderTree;
    @FXML private Button addFolderButton;
    @FXML private Button addNoteButton;
    @FXML private Button renameButton;
    @FXML private Button deleteButton;
    @FXML private Button dashboardButton;

    // Editor
    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    // Data (temporary in-memory, replace with DB later)
    private final ObservableList<Folder> folders = FXCollections.observableArrayList();
    private Note currentNote = null;

    @FXML
    public void initialize() {
        // Setup tree with sample folder + note
        Folder sampleFolder = new Folder("My Folder");
        sampleFolder.addNote(new Note("Sample Note", "This is an example note."));
        folders.add(sampleFolder);

        refreshTree();

        // Select handler
        folderTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            Object value = newVal.getValue();
            if (value instanceof Note note) {
                loadNoteIntoEditor(note);
            } else {
                clearEditor();
            }
        });

        // Button actions
        addFolderButton.setOnAction(e -> addFolder());
        addNoteButton.setOnAction(e -> addNote());
        renameButton.setOnAction(e -> renameSelected());
        deleteButton.setOnAction(e -> deleteSelected());

        saveButton.setOnAction(e -> saveNote());
        cancelButton.setOnAction(e -> clearEditor());

        dashboardButton.setOnAction(e -> {
            try {
                Stage stage = (Stage) dashboardButton.getScene().getWindow();
                App.changeScene(stage, "/com/cab302/eduplanner/dashboard.fxml", "EduPlanner — Dashboard");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // -------- Keyboard Navigation --------
        // Title field
        titleField.setOnAction(e -> contentArea.requestFocus());
        titleField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                contentArea.requestFocus();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                clearEditor();
            }
        });

        // Content area
        contentArea.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                clearEditor();
            } else if (e.getCode() == KeyCode.S && e.isControlDown()) {
                saveNote();
            } else if (e.getCode() == KeyCode.UP && contentArea.getCaretPosition() == 0) {
                titleField.requestFocus();
            }
        });

        // Save button
        saveButton.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                saveNote();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                clearEditor();
            }
        });

        // Cancel button
        cancelButton.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                clearEditor();
            }
        });

        // -------- TreeView Keyboard Shortcuts --------
        folderTree.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                deleteSelected(); // confirmation dialog
            } else if (e.getCode() == KeyCode.F2) {
                renameSelected(); // rename selected
            } else if (e.getCode() == KeyCode.ENTER) {
                TreeItem<Object> selected = folderTree.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() instanceof Note note) {
                    loadNoteIntoEditor(note);
                }
            }
        });
    }

    // --- Tree Management ---
    private void refreshTree() {
        TreeItem<Object> root = new TreeItem<>("All Folders");
        root.setExpanded(true);

        for (Folder folder : folders) {
            TreeItem<Object> folderItem = new TreeItem<>(folder);
            for (Note note : folder.getNotes()) {
                folderItem.getChildren().add(new TreeItem<>(note));
            }
            root.getChildren().add(folderItem);
        }

        folderTree.setRoot(root);
        folderTree.setShowRoot(false);
    }

    private void addFolder() {
        Folder folder = new Folder("New Folder");
        folders.add(folder);
        refreshTree();
    }

    private void addNote() {
        TreeItem<Object> selected = folderTree.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getValue() instanceof Folder folder)) return;

        Note note = new Note("Untitled Note", "");
        folder.addNote(note);
        refreshTree();
    }

    private void renameSelected() {
        TreeItem<Object> selected = folderTree.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected.getValue().toString());
        dialog.setTitle("Rename");
        dialog.setHeaderText("Rename selected item");
        dialog.setContentText("Enter new name:");
        dialog.showAndWait().ifPresent(name -> {
            if (selected.getValue() instanceof Folder folder) {
                folder.setName(name);
            } else if (selected.getValue() instanceof Note note) {
                note.setTitle(name);
            }
            refreshTree();
        });
    }

    private void deleteSelected() {
        TreeItem<Object> selected = folderTree.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Object value = selected.getValue();

        if (value instanceof Folder folder) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Folder");
            alert.setHeaderText("Are you sure you want to delete this folder?");
            alert.setContentText("Folder: " + folder.getName() + "\n"
                    + "This will also delete " + folder.getNotes().size() + " notes.");
            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    folders.remove(folder);
                    refreshTree();
                    clearEditor();
                }
            });
        } else if (value instanceof Note note) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Note");
            alert.setHeaderText("Are you sure you want to delete this note?");
            alert.setContentText("Title: " + note.getTitle() + "\n"
                    + "Content: " + (note.getContent().length() > 40
                    ? note.getContent().substring(0, 40) + "..."
                    : note.getContent()));
            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    TreeItem<Object> parent = selected.getParent();
                    if (parent != null && parent.getValue() instanceof Folder parentFolder) {
                        parentFolder.removeNote(note);
                    }
                    refreshTree();
                    clearEditor();
                }
            });
        }
    }

    // --- Editor ---
    private void loadNoteIntoEditor(Note note) {
        currentNote = note;
        titleField.setText(note.getTitle());
        contentArea.setText(note.getContent());
    }

    private void clearEditor() {
        currentNote = null;
        titleField.clear();
        contentArea.clear();
    }

    private void saveNote() {
        if (currentNote == null) return;

        currentNote.setTitle(titleField.getText().trim());
        currentNote.setContent(contentArea.getText().trim());
        refreshTree();
    }
}
