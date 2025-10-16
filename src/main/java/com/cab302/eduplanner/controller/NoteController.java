package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.App;
import com.cab302.eduplanner.model.Folder;
import com.cab302.eduplanner.model.Note;
import com.cab302.eduplanner.store.NoteStore;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.prefs.Preferences;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class NoteController {

    private static final Logger LOG = Logger.getLogger(NoteController.class.getName());
    private static final int WRAP_WIDTH = 90;

    // Sidebar
    @FXML private TreeView<Object> folderTree;
    @FXML private Button addFolderButton;
    @FXML private Button addNoteButton;
    @FXML private Button renameButton;
    @FXML private Button deleteButton;

    // Header
    @FXML private Button dashboardButton;

    // Drive Sync controls
    @FXML private Button setupDriveButton;
    @FXML private Button openDriveButton;
    @FXML private Button forgetDriveButton;

    // Export controls (no Quick Export)
    @FXML private Button exportCsvButton;
    @FXML private Button exportPdfButton;
    @FXML private Button exportDriveButton;

    // ----- Editor -----
    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    // ----- Model/state -----
    private final ObservableList<Folder> folders = NoteStore.getFolders();
    private Note currentNote = null;
    private boolean dirty = false;
    private boolean suppressSelectionEvents = false;

    // Preferences keys
    private static final String PREF_NODE = "com.cab302.eduplanner.notes";
    private static final String PREF_DRIVE_PATH = "driveSyncPath";
    private final Preferences prefs = Preferences.userRoot().node(PREF_NODE);

    @FXML
    public void initialize() {
        // Seed example only once for the whole app session
        if (NoteStore.isEmpty()) {
            Folder sample = new Folder("My Folder");
            sample.addNote(new Note("Sample Note", "This is an example note."));
            folders.add(sample);
        }

        refreshTree();

        // Show folder names/note titles in the tree
        folderTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
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
                LOG.log(Level.SEVERE, "Failed to switch to Dashboard scene", ex);
                new Alert(Alert.AlertType.ERROR, "Failed to return to dashboard:\n" + ex.getMessage()).showAndWait();
            }
        });

        setupDriveButton.setOnAction(e -> setupDriveSync());
        openDriveButton.setOnAction(e -> openDriveFolder());
        forgetDriveButton.setOnAction(e -> forgetDriveSync());

        exportCsvButton.setOnAction(e -> exportCurrentNoteAsCsv());
        exportPdfButton.setOnAction(e -> exportCurrentNoteAsPdf());
        exportDriveButton.setOnAction(e -> exportCurrentNoteToDrive());

        renameButton.disableProperty().bind(folderTree.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(folderTree.getSelectionModel().selectedItemProperty().isNull());
        addNoteButton.disableProperty().bind(folderTree.getSelectionModel().selectedItemProperty().isNull());

        saveButton.setDisable(true);
        cancelButton.setDisable(true);

        titleField.textProperty().addListener((o, ov, nv) -> onEditorChanged());
        contentArea.textProperty().addListener((o, ov, nv) -> onEditorChanged());

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
        folderTree.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) deleteSelected();
            else if (e.getCode() == KeyCode.F2) renameSelected();
        });

        updateDriveButtonsState();
        updateExportButtonsState();
    }

    // -------- Google Drive helpers --------

    /** If the user picked the Drive root (e.g., G:\), resolve to a real writable folder (e.g., G:\My Drive). */
    private Path normalizeGoogleDriveRoot(Path picked) {
        try {
            // If it's a root (no parent), try "My Drive" or enterprise/localized variants
            if (picked.getParent() == null) {
                Path myDrive = picked.resolve("My Drive");
                if (Files.isDirectory(myDrive)) return myDrive;

                try (DirectoryStream<Path> ds = Files.newDirectoryStream(picked, "My Drive*")) {
                    for (Path p : ds) {
                        if (Files.isDirectory(p)) return p;
                    }
                }
            }
        } catch (IOException ignored) { }
        return picked; // fallback to what user selected
    }

    /** Write & remove a tiny probe file to confirm the folder is writable. Shows an alert if not. */
    private boolean probeWritable(Path base) {
        try {
            Path probe = base.resolve("EduPlanner/.probe");
            Files.createDirectories(probe.getParent());
            Files.writeString(
                    probe, "ok",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            Files.deleteIfExists(probe);
            return true;
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR,
                    "That folder isn’t writable:\n" + base + "\n\n" + ex.getMessage()
            ).showAndWait();
            return false;
        }
    }

    // -------- Drive Sync --------
    private void setupDriveSync() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose your Google Drive folder (the one that syncs)");

        String existing = prefs.get(PREF_DRIVE_PATH, null);
        if (existing != null) {
            File dir = new File(existing);
            if (dir.isDirectory()) chooser.setInitialDirectory(dir);
        }

        File pickedFile = chooser.showDialog(setupDriveButton.getScene().getWindow());
        if (pickedFile == null) return;

        Path picked = pickedFile.toPath();
        Path normalized = normalizeGoogleDriveRoot(picked);

        // verify it’s writable before saving the preference
        if (!probeWritable(normalized)) return;

        prefs.put(PREF_DRIVE_PATH, normalized.toString());
        new Alert(Alert.AlertType.INFORMATION,
                "Drive sync folder set:\n" + normalized
        ).showAndWait();

        updateDriveButtonsState();
    }

    private void openDriveFolder() {
        String saved = prefs.get(PREF_DRIVE_PATH, null);
        if (saved == null) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Drive sync folder isn’t set yet. Click “Setup Drive Sync” first."
            ).showAndWait();
            return;
        }

        Path base = normalizeGoogleDriveRoot(Path.of(saved));

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(base.toFile());
            } else {
                new Alert(Alert.AlertType.WARNING,
                        "Desktop integration not supported on this system."
                ).showAndWait();
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Could not open folder:\n" + base + "\n\n" + ex.getMessage()
            ).showAndWait();
        }
    }

    private void forgetDriveSync() {
        prefs.remove(PREF_DRIVE_PATH);
        updateDriveButtonsState();
        new Alert(Alert.AlertType.INFORMATION, "Drive sync has been cleared.").showAndWait();
    }

    private void updateDriveButtonsState() {
        boolean has = prefs.get(PREF_DRIVE_PATH, null) != null;
        openDriveButton.setDisable(!has);
        forgetDriveButton.setDisable(!has);
        updateExportButtonsState();
    }

    // -------- Exports --------
    private void exportCurrentNoteAsCsv() {
        if (!canExportCurrent()) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Note as CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName(sanitizeFileName(currentNote.getTitle().isBlank() ? "Untitled Note" : currentNote.getTitle()) + ".csv");
        File dest = fc.showSaveDialog(exportCsvButton.getScene().getWindow());
        if (dest == null) return;

        String csv = "\"" + escapeCsv(currentNote.getTitle()) + "\",\"" + escapeCsv(currentNote.getContent()) + "\"\n";
        try {
            Files.writeString(dest.toPath(), csv, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            new Alert(Alert.AlertType.INFORMATION, "CSV exported:\n" + dest.getAbsolutePath()).showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to export CSV:\n" + ex.getMessage()).showAndWait();
        }
    }

    private void exportCurrentNoteAsPdf() {
        if (!canExportCurrent()) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Note as PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName(sanitizeFileName(currentNote.getTitle().isBlank() ? "Untitled Note" : currentNote.getTitle()) + ".pdf");
        File dest = fc.showSaveDialog(exportPdfButton.getScene().getWindow());
        if (dest == null) return;

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont  = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50f;
                float y = page.getMediaBox().getHeight() - margin;

                cs.setNonStrokingColor(0, 0, 0);

                // Title
                cs.beginText();
                cs.setFont(titleFont, 18);
                cs.newLineAtOffset(margin, y);
                cs.showText(currentNote.getTitle().isBlank() ? "Untitled Note" : currentNote.getTitle());
                cs.endText();

                y -= 28f;

                // Body (simple wrapping)
                cs.beginText();
                cs.setFont(bodyFont, 12);
                cs.newLineAtOffset(margin, y);

                String[] lines = currentNote.getContent().split("\\R");
                float leading = 16f;

                for (String line : lines) {
                    for (String chunk : wrap(line)) {
                        cs.showText(chunk);
                        cs.newLineAtOffset(0, -leading);
                        y -= leading;
                        if (y < margin) break; // single-page guard
                    }
                    if (y < margin) break;
                }
                cs.endText();
            }

            doc.save(dest);
            new Alert(Alert.AlertType.INFORMATION, "PDF exported:\n" + dest.getAbsolutePath()).showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to export PDF:\n" + ex.getMessage()).showAndWait();
        }
    }

    private void exportCurrentNoteToDrive() {
        if (!canExportCurrent()) return;

        String root = prefs.get(PREF_DRIVE_PATH, null);
        if (root == null) {
            new Alert(Alert.AlertType.WARNING, "No Drive folder set. Click “Setup Drive Sync” first.").showAndWait();
            return;
        }

        String folderName = resolveSelectedFolderName();
        String safeTitle = sanitizeFileName(currentNote.getTitle().isBlank() ? "Untitled Note" : currentNote.getTitle());
        Path driveBase = normalizeGoogleDriveRoot(Path.of(root));
        Path dest = driveBase.resolve("EduPlanner/Notes/" + folderName + "/" + safeTitle + ".md");
        String markdown = "# " + safe(currentNote.getTitle()) + "\n\n" + safe(currentNote.getContent()) + "\n";

        try {
            Files.createDirectories(dest.getParent());
            Files.writeString(dest, markdown, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            new Alert(Alert.AlertType.INFORMATION, "Exported to Drive (will sync):\n" + dest).showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Failed to export to Drive:\n" + dest + "\n\n" + ex.getMessage()
            ).showAndWait();
        }
    }

    private boolean canExportCurrent() {
        return currentNote != null && !dirty;
    }

    private void updateExportButtonsState() {
        boolean can = currentNote != null && !dirty;
        exportCsvButton.setDisable(!can);
        exportPdfButton.setDisable(!can);
        boolean hasDrive = prefs.get(PREF_DRIVE_PATH, null) != null;
        exportDriveButton.setDisable(!can || !hasDrive);
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

    private void loadNoteIntoEditor(Note note) {
        currentNote = note;
        titleField.setText(note.getTitle());
        contentArea.setText(note.getContent());
        onEditorChanged();

    }

    private void saveNote() {
        if (currentNote == null) return;

        currentNote.setTitle(titleField.getText().trim());
        currentNote.setContent(contentArea.getText().trim());

        // Mark clean BEFORE any UI refresh to avoid discard dialog
        dirty = false;
        onEditorChanged();

        suppressSelectionEvents = true;
        refreshTree();
        selectInTree(currentNote);
        suppressSelectionEvents = false;

        updateExportButtonsState();
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

                }
            });
            return;
        }
        currentNote = null;
        titleField.clear();
        contentArea.clear();
        onEditorChanged();

    }

    private void onEditorChanged() {
        boolean changed = currentNote != null &&
                (!titleField.getText().equals(currentNote.getTitle())
                        || !contentArea.getText().equals(currentNote.getContent()));
        dirty = changed;
        saveButton.setDisable(!changed);
        cancelButton.setDisable(!changed);
        updateExportButtonsState();
    }

    // --- Upload integration scaffold ----
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

    private static String escapeCsv(String s) { return s == null ? "" : s.replace("\"", "\"\""); }

    private static String[] wrap(String line) {
        if (line == null) return new String[]{""};
        if (line.length() <= WRAP_WIDTH) return new String[]{line};
        StringBuilder sb = new StringBuilder(line);
        java.util.List<String> parts = new java.util.ArrayList<>();
        while (sb.length() > WRAP_WIDTH) {
            parts.add(sb.substring(0, WRAP_WIDTH));
            sb.delete(0, WRAP_WIDTH);
        }
        if (!sb.isEmpty()) parts.add(sb.toString());
        return parts.toArray(String[]::new);
    }
}


