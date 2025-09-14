package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.App;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    // Task sorter - sets default and cycle order
    private enum SortMode { DUE_DATE, ALPHA, GROUPED_SUBJECT }
    private SortMode sortMode = SortMode.DUE_DATE;

    // Temp tasks - replace once new task flow and task repo complete
    private static final class tempTask {
        final String title, subject;
        final LocalDate due;
        boolean archived = false;
        tempTask(String title, String subject, LocalDate due) {
            this.title = title; this.subject = subject; this.due = due;
        }
    }
    private final List<tempTask> tasks = new ArrayList<>();

    @FXML public void initialize() {
        // Removes scene builder junk (my bad)
        cardsBox.getChildren().clear();

        greetingLabel.setText("Welcome back, User");
        tickClock();
        startMinuteTicker();

        // Temp tasks
        tasks.add(new tempTask("Tutorials 6 and 7", "CAB202", LocalDate.now().plusDays(3)));
        tasks.add(new tempTask("PST", "MXB100", LocalDate.now().plusDays(6)));
        tasks.add(new tempTask("Checkpoint 3", "CAB302", LocalDate.now().plusDays(1)));

        sortButton.setOnAction(e -> { cycleSort(); render(); });
        newButton.setOnAction(e -> info("New task flow TBD"));
        calendarButton.setOnAction(e -> info("Tasks Calendar view TBD"));
        detailButton.setOnAction(e -> info("Tasks detailed view TBD"));

        flashcardsTile.setOnAction(e -> navigate("/com/cab302/eduplanner/flashcard.fxml", "EduPlanner — Flashcards"));
        pomodoroTile.setDisable(true);

        render();
    }

    // Local time helpers
    private void tickClock() {
        localTimeLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

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

    private void cycleSort() {
        sortMode = switch (sortMode) {
            case DUE_DATE -> SortMode.ALPHA;
            case ALPHA -> SortMode.GROUPED_SUBJECT;
            case GROUPED_SUBJECT -> SortMode.DUE_DATE;
        };
    }

    // Displays the tasks in current sort mode
    private void render() {
        // Update sort button label
        sortButton.setText(switch (sortMode) {
            case DUE_DATE -> "[Sort: Due Date]";
            case ALPHA -> "[Sort: Alphabetical]";
            case GROUPED_SUBJECT -> "[Sort: Grouped by Subject]";
        });

        // Geta active tasks
        List<tempTask> active = tasks.stream().filter(t -> !t.archived).toList();
        emptyLabel.setVisible(active.isEmpty());

        // Clear current task cards
        cardsBox.getChildren().clear();

        if (sortMode == SortMode.GROUPED_SUBJECT) {
            // Group by subject, then sort within each subject by due date
            active.stream()
                    .sorted(Comparator.comparing((tempTask t) -> t.subject, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(t -> t.due))
                    .map(t -> t.subject)
                    .distinct()
                    .forEach(subj -> {
                        Label header = new Label(subj);
                        header.setStyle("-fx-font-weight: bold; -fx-padding: 6 0 2 0;");
                        cardsBox.getChildren().add(header);

                        active.stream()
                                .filter(t -> Objects.equals(t.subject, subj))
                                .sorted(Comparator.comparing(t -> t.due))
                                .forEach(t -> cardsBox.getChildren().add(card(t)));
                    });
        } else {
            // Flat list sorted by due date or alphabetical
            List<tempTask> sorted = new ArrayList<>(active);

            if (sortMode == SortMode.DUE_DATE) {
                sorted.sort(Comparator.comparing(t -> t.due));
            } else {
                sorted.sort(Comparator.comparing((tempTask t) -> t.title, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(t -> t.due));
            }

            sorted.forEach(t -> cardsBox.getChildren().add(card(t)));
        }
    }


    // Task node builder
    private Node card(tempTask t) {
        // Title + due date
        Label title = new Label(t.title);
        Label due = new Label(t.due == null ? "No due date"
                : "Due: " + t.due.format(DateTimeFormatter.ofPattern("dd MMM")));
        due.setStyle("-fx-opacity: 0.75;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Archive tasks
        CheckBox done = new CheckBox();
        done.setOnAction(e -> { t.archived = true; render(); });

        HBox box = new HBox(10, title, due, spacer, done);
        box.setStyle("-fx-padding:10; -fx-border-color: -fx-box-border; -fx-border-radius:6; -fx-background-radius:6;");

        // Detailed View
        box.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof CheckBox) return;
            info("Open Task Detail/Edit (Planner) — TBD");
        });

        return box;
    }

    // Switch scene to another FXML file
    private void navigate(String fxml, String title) {
        try {
            Stage stage = (Stage) cardsBox.getScene().getWindow();
            App.changeScene(stage, fxml, title);
        } catch (IOException ex) {
            info("Navigation failed: " + ex.getMessage());
        }
    }

    // Simple console logger
    private void info(String msg) {
        System.out.println(msg);
    }

}
