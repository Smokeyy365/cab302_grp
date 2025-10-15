package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.App;
import com.cab302.eduplanner.appcontext.UserSession;
import com.cab302.eduplanner.model.Task;
import com.cab302.eduplanner.repository.TaskRepository;
import com.cab302.eduplanner.service.GoogleCalendarExport;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the main dashboard view.
 * UI elements are injected via FXML. All scene transitions are modal where editing is involved.
 */
public class DashboardController {

    // User header
    @FXML private Label greetingLabel;
    @FXML private Label localTimeLabel;

    // Tasks panel header
    @FXML private Button sortButton;
    @FXML private Button newButton;
    @FXML private Button detailButton;

    // Tasks panel UI
    @FXML private VBox cardsBox;
    @FXML private Label emptyLabel;

    // Widgets panel
    @FXML private Label widgetsHeader;
    @FXML private Button flashcardsTile;
    @FXML private Button notesTile;
    @FXML private Button pomodoroTile;
    @FXML private Button darkTile;
    @FXML private Button rubricTile;

    /**
     * Supported sort modes for rendering the task list.
     */
    private enum SortMode { DUE_DATE, ALPHA, GROUPED_SUBJECT }
    private SortMode sortMode = SortMode.DUE_DATE;

    private final TaskRepository taskRepo = new TaskRepository();
    /** Latest fetched tasks used for rendering/editing in this controller lifecycle. */
    private List<Task> tasks;

    /**
     * FXML lifecycle hook. Wires UI handlers, starts the clock ticker, and loads tasks for the
     */
    @FXML public void initialize() {
        // Clear any placeholder nodes
        cardsBox.getChildren().clear();

        // Greeting from session
        var user = UserSession.getCurrentUser();
        String first = (user != null && user.getFirstName() != null && !user.getFirstName().isBlank())
                ? user.getFirstName() : "User";
        greetingLabel.setText("Welcome back, " + first);

        tickClock();
        startMinuteTicker();

        // Button handlers
        sortButton.setOnAction(e -> { cycleSort(); render(); });
        newButton.setOnAction(e -> openCreateForm());
        detailButton.setOnAction(e -> info("Tasks detailed view TBD"));

        flashcardsTile.setOnAction(e -> navigate("/com/cab302/eduplanner/flashcard.fxml", "EduPlanner — Flashcards"));
        notesTile.setOnAction( e -> navigate("/com/cab302/eduplanner/note.fxml", "EduPlanner — Notes"));
        pomodoroTile.setDisable(true);
        darkTile.setDisable(true);
        rubricTile.setOnAction(e -> navigate("/com/cab302/eduplanner/rubric.fxml", "EduPlanner — Rubric Analysis"));
        rubricTile.setDisable(false);

        refreshTasks(); // loads from DB and renders
    }

    // Local time helpers

    /**
     * Updates the local-time label to the current time in {@code HH:mm} format.
     */
    private void tickClock() {
        localTimeLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    /**
     * Starts a background thread that ticks the clock once per minute.
     */
    private void startMinuteTicker() {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    //noinspection BusyWait (supresses warning)
                    Thread.sleep(60_000);
                    Platform.runLater(this::tickClock);
                }
            } catch (InterruptedException ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    // Date Formatter
    private static final DateTimeFormatter DUE_FMT = DateTimeFormatter.ofPattern("dd MMM");

    /**
     * Cycles the current sort mode in the order: DUE_DATE → ALPHA → GROUPED_SUBJECT → DUE_DATE.
     */
    private void cycleSort() {
        sortMode = switch (sortMode) {
            case DUE_DATE -> SortMode.ALPHA;
            case ALPHA -> SortMode.GROUPED_SUBJECT;
            case GROUPED_SUBJECT -> SortMode.DUE_DATE;
        };
    }

    /**
     * Fetches tasks for the logged-in user and triggers a re-render.
     * If no user is logged in, shows a placeholder message and clears the list.
     */
    private void refreshTasks() {
        if (!UserSession.isLoggedIn()) {
            emptyLabel.setText("Not logged in");
            emptyLabel.setVisible(true);
            cardsBox.getChildren().clear();
            return;
        }
        long userId = UserSession.getCurrentUser().getUserId();
        tasks = taskRepo.findByUserId(userId);
        render();
    }

    // Subject and title null-friendly helpers

    /**
     * Returns a non-blank subject string for display; falls back to {@code "(No Subject)"}.
     *
     * @param t task to read
     * @return subject or placeholder
     */
    private static String safeSubject(Task t) {
        String s = t.getSubject();
        return (s == null || s.isBlank()) ? "(No Subject)" : s.trim();
    }

    private static String safeTitle(Task t) {
        String s = t.getTitle();
        return (s == null || s.isBlank()) ? "(Untitled Task)" : s.trim();
    }

    // Task card builders
    private Node cardWithSubject(Task t) {
        Label subj = new Label(safeSubject(t));
        Label title = new Label(safeTitle(t));
        Label due = new Label(t.getDueDate() == null
                ? "No due date"
                : "Due: " + t.getDueDate().format(DUE_FMT));
        due.getStyleClass().add("task-due");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CheckBox del = new CheckBox();
        del.setOnAction(e -> {
            e.consume();
            if (confirm("Delete this task?")) {
                long userId = UserSession.getCurrentUser().getUserId();
                if (!taskRepo.delete(t.getTaskId(), userId)) info("Delete failed");
                refreshTasks();
            } else del.setSelected(false);
        });

        HBox box = new HBox(10, subj, title, due, spacer, del);
        box.getStyleClass().add("task-card");
        box.setOnMouseClicked(e -> { if (!(e.getTarget() instanceof CheckBox)) openEditForm(t); });
        return box;
    }

    /**
     * Builds a task card without repeating the subject (used in grouped-by-subject view).
     *
     * @param t task to render
     */
    private Node cardWithoutSubject(Task t) {
        Label title = new Label(safeTitle(t));
        Label due = new Label(t.getDueDate() == null
                ? "No due date"
                : "Due: " + t.getDueDate().format(DUE_FMT));
        due.getStyleClass().add("task-due");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CheckBox del = new CheckBox();
        del.setOnAction(e -> {
            e.consume();
            if (confirm("Delete this task?")) {
                long userId = UserSession.getCurrentUser().getUserId();
                if (!taskRepo.delete(t.getTaskId(), userId)) info("Delete failed");
                refreshTasks();
            } else del.setSelected(false);
        });

        HBox box = new HBox(10, title, due, spacer, del);
        box.getStyleClass().add("task-card");
        box.setOnMouseClicked(e -> { if (!(e.getTarget() instanceof CheckBox)) openEditForm(t); });
        return box;
    }

    private void render() {
        // Update sort button label
        sortButton.setText(switch (sortMode) {
            case DUE_DATE -> "[Sort: Due Date]";
            case ALPHA -> "[Sort: Alphabetical]";
            case GROUPED_SUBJECT -> "[Sort: Grouped by Subject]";
        });

        cardsBox.getChildren().clear();
        if (tasks == null || tasks.isEmpty()) {
            emptyLabel.setText("No tasks");
            emptyLabel.setVisible(true);
            return;
        }
        emptyLabel.setVisible(false);

        // Null subject safe render
        switch (sortMode) {
            case DUE_DATE -> {
                var view = new java.util.ArrayList<>(tasks);
                view.sort(
                        java.util.Comparator
                                .comparing((Task t) -> t.getDueDate(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                                .thenComparing(t -> safeTitle(t), String.CASE_INSENSITIVE_ORDER)
                );
                for (Task t : view) cardsBox.getChildren().add(cardWithSubject(t)); // [Subject][Title][Due]
            }
            case ALPHA -> {
                var view = new java.util.ArrayList<>(tasks);
                view.sort(
                        java.util.Comparator
                                .comparing((Task t) -> safeTitle(t), String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(t -> t.getDueDate(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                );
                for (Task t : view) cardsBox.getChildren().add(cardWithSubject(t)); // [Subject][Title][Due]
            }
            case GROUPED_SUBJECT -> {
                var view = new java.util.ArrayList<>(tasks);
                view.sort(
                        java.util.Comparator
                                .comparing((Task t) -> safeSubject(t), String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(t -> t.getDueDate(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                                .thenComparing(t -> safeTitle(t), String.CASE_INSENSITIVE_ORDER)
                );

                String currentHeader = null;
                for (Task t : view) {
                    String subj = safeSubject(t);
                    if (!subj.equals(currentHeader)) {
                        currentHeader = subj;
                        Label header = new Label(subj);
                        header.getStyleClass().add("task-group-header"); // you already use this class
                        cardsBox.getChildren().add(header);
                    }
                    cardsBox.getChildren().add(cardWithoutSubject(t)); // no subject here (header shows it)
                }
            }
        }
    }


    /**
     * Opens the modal task creation dialog.
     * Refreshes the list upon successful save.
     */
    private void openCreateForm() {
        try {
            FXMLLoader fx = new FXMLLoader(App.class.getResource("/com/cab302/eduplanner/task_flow.fxml"));
            Parent root = fx.load();
            TaskFlowController ctl = fx.getController();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(cardsBox.getScene().getWindow());
            dialog.setTitle("New Task");
            dialog.setResizable(false);
            dialog.setScene(new Scene(root));
            dialog.centerOnScreen();
            dialog.showAndWait();

            if (ctl.isSaved()) refreshTasks();
        } catch (IOException ex) {
            info("Open create form failed: " + ex.getMessage());
        }
    }

    /**
     * Opens the modal task edit dialog pre-populated with a copy of the given task.
     * Refreshes the list upon successful save.
     *
     * @param existing the task to edit
     */
    private void openEditForm(Task existing) {
        try {
            FXMLLoader fx = new FXMLLoader(App.class.getResource("/com/cab302/eduplanner/task_flow.fxml"));
            Parent root = fx.load();
            TaskFlowController ctl = fx.getController();
            // pass a copy so we don't mutate the list until save succeeds
            Task copy = new Task(existing.getUserId(), existing.getSubject(), existing.getTitle(),
                    existing.getDueDate(), existing.getNotes(), existing.getWeight(),
                    existing.getAchievedMark(), existing.getMaxMark());
            copy.setTaskId(existing.getTaskId());
            ctl.setEditing(copy);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(cardsBox.getScene().getWindow());
            dialog.setTitle("Edit Task");
            dialog.setResizable(false);
            dialog.setScene(new Scene(root));
            dialog.centerOnScreen();
            dialog.showAndWait();

            if (ctl.isSaved()) refreshTasks();
        } catch (IOException ex) {
            info("Open edit form failed: " + ex.getMessage());
        }
    }

    /**
     * Navigates to another FXML scene hosted by the same stage.
     *
     * @param fxml  classpath to the FXML file
     * @param title window title to set
     */
    private void navigate(String fxml, String title) {
        try {
            Stage stage = (Stage) cardsBox.getScene().getWindow();
            App.changeScene(stage, fxml, title);
        } catch (IOException ex) {
            info("Navigation failed: " + ex.getMessage());
        }
    }

    /**
     * Shows a confirmation dialog with OK/Cancel options.
     *
     * @param message confirmation prompt
     */
    private boolean confirm(String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        a.initOwner(cardsBox.getScene().getWindow());
        a.setHeaderText(null);
        var res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }

    private void info(String msg) {
        System.out.println(msg);
    }

    // Google calendar URL
    @FXML
    private void handleOpenGoogleCalendar() {
        try {
            String url = "https://calendar.google.com/calendar/u/0/r?tab=mc";
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } else {
                new Alert(Alert.AlertType.INFORMATION,
                        "Could not open a browser automatically. Copy this URL:\n" + url
                ).showAndWait();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Failed to open Google Calendar:\n" + ex.getMessage()
            ).showAndWait();
        }
    }

    // ============== NEW: Calendar Export Methods ==============

    /**
     * Export all tasks to Google Calendar via individual create links
     */
    @FXML
    private void handleExportTasksToCalendar() {
        if (tasks == null || tasks.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No tasks to export.").showAndWait();
            return;
        }

        // Ask user if they want to open multiple tabs
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This will open " + tasks.size() + " browser tab(s) to create calendar events. Continue?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Export Tasks to Google Calendar");
        var result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            exportTasksToGoogleCalendar();
        }
    }

    /**
     * Export tasks to .ics file for import into any calendar app
     */
    @FXML
    private void handleExportTasksToICS() {
        if (tasks == null || tasks.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No tasks to export.").showAndWait();
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Calendar Export");
            fileChooser.setInitialFileName("eduplanner_tasks.ics");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("iCalendar Files", "*.ics"));

            File file = fileChooser.showSaveDialog(cardsBox.getScene().getWindow());
            if (file != null) {
                GoogleCalendarExport exporter = new GoogleCalendarExport();
                List<GoogleCalendarExport.Event> events = convertTasksToEvents();
                exporter.exportToIcs(file, events);

                Alert success = new Alert(Alert.AlertType.INFORMATION,
                        "Tasks exported to: " + file.getAbsolutePath());
                success.setHeaderText("Export Successful");
                success.showAndWait();
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Failed to export tasks:\n" + ex.getMessage()).showAndWait();
        }
    }

    /**
     * Convert tasks to calendar events
     */
    private List<GoogleCalendarExport.Event> convertTasksToEvents() {
        List<GoogleCalendarExport.Event> events = new ArrayList<>();

        for (Task task : tasks) {
            if (task.getDueDate() == null) continue; // Skip tasks without due dates

            // Create event for the due date at 9 AM (1 hour duration)
            ZonedDateTime start = task.getDueDate()
                    .atTime(LocalTime.of(9, 0))
                    .atZone(ZoneId.systemDefault());
            ZonedDateTime end = start.plusHours(1);

            String title = safeTitle(task);
            String subject = safeSubject(task);

            // Build description
            StringBuilder desc = new StringBuilder();
            desc.append("Subject: ").append(subject).append("\n");
            if (task.getNotes() != null && !task.getNotes().isBlank()) {
                desc.append("\nNotes:\n").append(task.getNotes());
            }
            if (task.getWeight() != null) {
                desc.append("\n\nWeight: ").append(task.getWeight()).append("%");
            }
            if (task.getAchievedMark() != null && task.getMaxMark() != null) {
                desc.append("\nScore: ").append(task.getAchievedMark())
                        .append("/").append(task.getMaxMark());
            }

            GoogleCalendarExport.Event event = new GoogleCalendarExport.Event(
                    "eduplanner-task-" + task.getTaskId(),
                    title,
                    start,
                    end,
                    subject, // location field
                    desc.toString()
            );

            events.add(event);
        }

        return events;
    }

    /**
     * Opens Google Calendar links for each task
     */
    private void exportTasksToGoogleCalendar() {
        int exported = 0;
        for (Task task : tasks) {
            if (task.getDueDate() == null) continue;

            ZonedDateTime start = task.getDueDate()
                    .atTime(LocalTime.of(9, 0))
                    .atZone(ZoneId.systemDefault());
            ZonedDateTime end = start.plusHours(1);

            String title = safeTitle(task);
            StringBuilder desc = new StringBuilder();
            desc.append("Subject: ").append(safeSubject(task)).append("\n");
            if (task.getNotes() != null && !task.getNotes().isBlank()) {
                desc.append("\n").append(task.getNotes());
            }

            String url = GoogleCalendarExport.createGoogleLink(
                    title, desc.toString(), start, end);

            GoogleCalendarExport.openInBrowser(url);
            exported++;

            // Small delay to avoid overwhelming the browser
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        if (exported > 0) {
            info("Opened " + exported + " calendar event(s) in browser");
        } else {
            new Alert(Alert.AlertType.INFORMATION,
                    "No tasks with due dates to export.").showAndWait();
        }
    }
}