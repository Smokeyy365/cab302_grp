package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.App;
import com.cab302.eduplanner.appcontext.UserSession;
import com.cab302.eduplanner.model.Task;
import com.cab302.eduplanner.repository.TaskRepository;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    @FXML private Button calendarButton;
    @FXML private Button detailButton;

    // Tasks panel UI
    @FXML private VBox cardsBox;
    @FXML private Label emptyLabel;

    // Widgets panel
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
        calendarButton.setOnAction(e -> info("Tasks Calendar view TBD"));
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
                    Thread.sleep(60_000);
                    Platform.runLater(this::tickClock);
                }
            } catch (InterruptedException ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

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
                : "Due: " + t.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM")));
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
                : "Due: " + t.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM")));
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

    private Node card(Task t) {
        String dueText = (t.getDueDate() == null)
                ? "No due date"
                : "Due: " + t.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM"));

        Label title = new Label(t.getTitle());
        Label due = new Label(dueText);
        due.getStyleClass().add("task-due");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Delete checkbox (acts as delete for now)
        CheckBox done = new CheckBox();
        done.setOnAction(e -> {
            e.consume();
            if (confirm("Delete this task?")) {
                long userId = UserSession.getCurrentUser().getUserId();
                if (!taskRepo.delete(t.getTaskId(), userId)) {
                    info("Delete failed");
                }
                refreshTasks();
            } else {
                done.setSelected(false);
            }
        });

        HBox box = new HBox(10, title, due, spacer, done);
        box.getStyleClass().add("task-card");

        // Click to edit
        box.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof CheckBox) return;
            openEditForm(t);
        });

        return box;
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
            // pass a copy so we don’t mutate the list until save succeeds
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
}
