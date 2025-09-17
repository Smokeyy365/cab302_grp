package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.appcontext.UserSession;
import com.cab302.eduplanner.model.Task;
import com.cab302.eduplanner.repository.TaskRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Optional;

public class TaskFlowController {
    @FXML private TextField subjectField, titleField, dueDateField, weightField, achievedMarkField, maxMarkField;
    @FXML private TextArea notesArea;
    @FXML private Label errorLabel;

    private final TaskRepository repo = new TaskRepository();

    private Task editing;     // null → create mode
    private boolean saved;    // whether user successfully saved
    private Task result;      // created/updated task (with id for create)

    public void setEditing(Task t) {
        this.editing = t;
        if (t != null) {
            subjectField.setText(t.getSubject());
            titleField.setText(t.getTitle());
            dueDateField.setText(t.getDueDate() == null ? "" : t.getDueDate().toString());
            notesArea.setText(t.getNotes());
            if (t.getWeight() != null) weightField.setText(t.getWeight().toString());
            if (t.getAchievedMark() != null) achievedMarkField.setText(t.getAchievedMark().toString());
            if (t.getMaxMark() != null) maxMarkField.setText(t.getMaxMark().toString());
        }
    }

    @FXML
    private void onSave() {
        try {
            if (titleField.getText() == null || titleField.getText().isBlank()) {
                throw new IllegalArgumentException("Title is required.");
            }
            Task t = (editing == null) ? new Task() : editing;
            t.setUserId(UserSession.getCurrentUser().getUserId());
            t.setSubject(trimOrNull(subjectField.getText()));
            t.setTitle(titleField.getText().trim());
            t.setDueDate(parseDateOrNull(dueDateField.getText()));
            t.setNotes(trimOrNull(notesArea.getText()));
            t.setWeight(parseIntOrNull(weightField.getText(), "Weight must be an integer."));
            t.setAchievedMark(parseDoubleOrNull(achievedMarkField.getText(), "Achieved must be numeric."));
            t.setMaxMark(parseDoubleOrNull(maxMarkField.getText(), "Max must be numeric."));

            // validate numeric constraints consistent with DB CHECK
            if (t.getWeight() != null && t.getWeight() < 0) throw new IllegalArgumentException("Weight must be ≥ 0.");
            if (t.getAchievedMark() != null && t.getAchievedMark() < 0) throw new IllegalArgumentException("Achieved must be ≥ 0.");
            if (t.getMaxMark() != null && t.getMaxMark() <= 0) throw new IllegalArgumentException("Max must be > 0.");

            boolean ok;
            if (editing == null) {
                Optional<Long> id = repo.insert(t);
                ok = id.isPresent();
                id.ifPresent(t::setTaskId);
            } else {
                ok = repo.update(t);
            }
            if (!ok) { errorLabel.setText("Could not save task."); return; }

            this.saved = true;
            this.result = t;
            close();
        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            errorLabel.setText("Unexpected error.");
        }
    }

    @FXML private void onCancel() { close(); }

    private void close() {
        Stage s = (Stage) errorLabel.getScene().getWindow();
        s.close();
    }

    public boolean isSaved() { return saved; }
    public Task getResult() { return result; }

    // helpers
    private static String trimOrNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private static LocalDate parseDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s.trim()); // expects YYYY-MM-DD
    }
    private static Integer parseIntOrNull(String s, String msg) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { throw new IllegalArgumentException(msg); }
    }
    private static Double parseDoubleOrNull(String s, String msg) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { throw new IllegalArgumentException(msg); }
    }
}
