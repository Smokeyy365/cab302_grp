package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.App;
import com.cab302.eduplanner.model.Folder;
import com.cab302.eduplanner.model.Note;
import com.cab302.eduplanner.store.NoteStore;
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

    // Header
    @FXML private Button uploadButton;
    @FXML private Button dashboardButton;

    // Editor
    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    // Shared in-memory data (persists while app is open)
    private final ObservableList<Folder> folders = NoteStore.getFolders();
    private Note currentNote = null;

    // Track unsaved edits & temporarily suppress selection reactions
    private boolean dirty = false;
    private boolean suppressSelectionEvents = false;

    @FXML
    public void initialize() {
        // Seed example only once for the whole app session
        if (NoteStore.isEmpty()) {
            Folder sampleFolder = new Folder("My Folder");
            sampleFolder.addNote(new Note("Sample Note", "This is an example note."));
            folders.add(sampleFolder);
        }

        refreshTree();

        // Show folder names/note titles in the tree
        folderTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                if (item instanceof Folder f) setText(f.getName());
                else if (item instanceof Note n) setText(n.getTitle().isBlank() ? "(Untitled Note)" : n.getTitle());
                else setText(item.toString());
            }
        });

        // Selection handler (guarded)
        folderTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressSelectionEvents) return;
            if (newVal == null) { clearEditor(); return; }
            Object value = newVal.getValue();
            if (value instanceof Note note) loadNoteIntoEditor(note);
            else clearEditor();
        });

        // Button actions
        addFolderButton.setOnAction(e -> addFolder());
        addNoteButton.setOnAction(e -> addNote());
        renameButton.setOnAction(e -> renameSelected());
        deleteButton.setOnAction(e -> deleteSelected());
        saveButton.setOnAction(e -> saveNote());
        cancelButton.setOnAction(e -> clearEditor());
        uploadButton.setOnAction(e -> onUploadNote()); // Upload handler

        dashboardButton.setOnAction(e -> {
            try {
                Stage stage = (Stage) dashboardButton.getScene().getWindow();
                App.changeScene(stage, "/com/cab302/eduplanner/dashboard.fxml", "EduPlanner — Dashboard");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // Enable/disable buttons
        renameButton.disableProperty().bind(folderTree.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(folderTree.getSelectionModel().selectedItemProperty().isNull());
        addNoteButton.disableProperty().bind(folderTree.getSelectionModel().selectedItemProperty().isNull());
        saveButton.setDisable(true);
        cancelButton.setDisable(true);
        updateUploadButtonState(); // initialize

        // Dirty tracking
        titleField.textProperty().addListener((o, ov, nv) -> onEditorChanged());
        contentArea.textProperty().addListener((o, ov, nv) -> onEditorChanged());

        // -------- Keyboard Navigation --------
        titleField.setOnAction(e -> contentArea.requestFocus());
        titleField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) contentArea.requestFocus();
            else if (e.getCode() == KeyCode.ESCAPE) clearEditor();
        });

        contentArea.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) clearEditor();
            else if (e.getCode() == KeyCode.S && e.isControlDown()) saveNote();
            else if (e.getCode() == KeyCode.UP && contentArea.getCaretPosition() == 0) titleField.requestFocus();
        });

        saveButton.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) { saveNote(); contentArea.requestFocus(); }
            else if (e.getCode() == KeyCode.ESCAPE) clearEditor();
        });

        cancelButton.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) clearEditor();
        });

        // Tree shortcuts
        folderTree.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) deleteSelected();
            else if (e.getCode() == KeyCode.F2) renameSelected();
            else if (e.getCode() == KeyCode.ENTER) {
                TreeItem<Object> selected = folderTree.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() instanceof Note note) loadNoteIntoEditor(note);
            }
        });
    }

    // --- Tree Management ---
    private void refreshTree() {
        suppressSelectionEvents = true;
        Object toReselect = null;
        TreeItem<Object> currentSel = folderTree.getSelectionModel().getSelectedItem();
        if (currentSel != null) toReselect = currentSel.getValue();

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

        // Restore selection if possible
        if (toReselect != null) {
            TreeItem<Object> found = findTreeItem(root, toReselect);
            if (found != null) folderTree.getSelectionModel().select(found);
        }
        suppressSelectionEvents = false;
    }

    private void addFolder() {
        Folder folder = new Folder("New Folder");
        folders.add(folder);
        refreshTree();
        selectInTree(folder);
        renameSelected(); // optional inline rename
    }

    private void addNote() {
        TreeItem<Object> selected = folderTree.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Folder folder = selected.getValue() instanceof Folder f ? f
                : selected.getParent() != null && selected.getParent().getValue() instanceof Folder pf ? pf
                : null;
        if (folder == null) return;

        Note note = new Note("Untitled Note", "");
        folder.addNote(note);
        refreshTree();
        selectInTree(note);
        loadNoteIntoEditor(note);
        titleField.requestFocus();
    }

    private void renameSelected() {
        TreeItem<Object> selected = folderTree.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String currentName = selected.getValue() instanceof Folder f ? f.getName()
                : selected.getValue() instanceof Note n ? n.getTitle()
                : selected.getValue().toString();

        TextInputDialog dialog = new TextInputDialog(currentName);
        dialog.setTitle("Rename");
        dialog.setHeaderText("Rename selected item");
        dialog.setContentText("Enter new name:");
        dialog.showAndWait().ifPresent(name -> {
            if (selected.getValue() instanceof Folder folder) {
                folder.setName(name);
            } else if (selected.getValue() instanceof Note note) {
                note.setTitle(name);
                if (currentNote == note) titleField.setText(name);
            }
            refreshTree();
            selectInTree(selected.getValue());
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
            alert.setContentText("Folder: " + folder.getName() + "\nThis will also delete " + folder.getNotes().size() + " notes.");
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
            alert.setContentText("Title: " + (note.getTitle().isBlank() ? "(Untitled Note)" : note.getTitle()));
            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    TreeItem<Object> parent = selected.getParent();
                    if (parent != null && parent.getValue() instanceof Folder parentFolder) {
                        parentFolder.removeNote(note);
                    }
                    refreshTree();
                    if (currentNote == note) clearEditor();
                }
            });
        }
    }

    // --- Editor ---
    private void loadNoteIntoEditor(Note note) {
        currentNote = note;
        titleField.setText(note.getTitle());
        contentArea.setText(note.getContent());
        onEditorChanged();
        updateUploadButtonState();
    }

    private void saveNote() {
        if (currentNote == null) return;

        // Update model from fields
        currentNote.setTitle(titleField.getText().trim());
        currentNote.setContent(contentArea.getText().trim());

        // Mark clean BEFORE any UI refresh to avoid discard dialog
        dirty = false;
        onEditorChanged();

        // Rebuild tree & keep selection WITHOUT firing selection listeners
        suppressSelectionEvents = true;
        refreshTree();
        selectInTree(currentNote);
        suppressSelectionEvents = false;

        updateUploadButtonState();
    }

    private void clearEditor() {
        if (dirty) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Discard unsaved changes?", ButtonType.OK, ButtonType.CANCEL);
            a.setTitle("Discard Changes?");
            a.setHeaderText("You have unsaved changes.");
            a.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    if (currentNote != null) loadNoteIntoEditor(currentNote);
                    else { titleField.clear(); contentArea.clear(); onEditorChanged(); }
                    dirty = false;
                    onEditorChanged();
                    updateUploadButtonState();
                }
            });
            return;
        }
        currentNote = null;
        titleField.clear();
        contentArea.clear();
        onEditorChanged();
        updateUploadButtonState();
    }

    private void onEditorChanged() {
        boolean changed = currentNote != null &&
                (!titleField.getText().equals(currentNote.getTitle())
                        || !contentArea.getText().equals(currentNote.getContent()));
        dirty = changed;
        saveButton.setDisable(!changed);
        cancelButton.setDisable(!changed);
        updateUploadButtonState();
    }

    // --- Upload integration scaffold ---
    @FXML
    private void onUploadNote() {
        if (currentNote == null || dirty) return; // disabled in UI, double-guard

        String folderName = resolveSelectedFolderName();
        String fileName = sanitizeFileName(currentNote.getTitle().isBlank() ? "Untitled Note" : currentNote.getTitle()) + ".md";
        String drivePath = "EduPlanner/Notes/" + folderName + "/" + fileName;

        String markdown = "# " + safe(currentNote.getTitle()) + "\n\n" + safe(currentNote.getContent()) + "\n";

        try {
            // TODO CALL DRIVE HERE
            // GoogleDriveNotesService.uploadString(markdown, drivePath, "text/markdown");
            new Alert(Alert.AlertType.INFORMATION, "Uploaded to Google Drive:\n" + drivePath).showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Upload failed:\n" + ex.getMessage()).showAndWait();
        }
    }

    private void updateUploadButtonState() {
        if (uploadButton == null) return;
        boolean hasNote = currentNote != null;
        uploadButton.setDisable(!hasNote || dirty);
    }

    private String resolveSelectedFolderName() {
        TreeItem<Object> sel = folderTree.getSelectionModel().getSelectedItem();
        if (sel == null) return "Unsorted";
        if (sel.getValue() instanceof Folder f) return f.getName();
        if (sel.getParent() != null && sel.getParent().getValue() instanceof Folder pf) return pf.getName();
        return "Unsorted";
    }

    private static String sanitizeFileName(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // --- Selection helpers ---
    private void selectInTree(Object target) {
        TreeItem<Object> root = folderTree.getRoot();
        if (root == null) return;
        TreeItem<Object> found = findTreeItem(root, target);
        if (found != null) {
            folderTree.getSelectionModel().select(found);
            folderTree.scrollTo(folderTree.getSelectionModel().getSelectedIndex());
        }
    }

    private TreeItem<Object> findTreeItem(TreeItem<Object> node, Object target) {
        if (node.getValue() == target) return node;
        for (TreeItem<Object> child : node.getChildren()) {
            TreeItem<Object> result = findTreeItem(child, target);
            if (result != null) return result;
        }
        return null;
    }
}

