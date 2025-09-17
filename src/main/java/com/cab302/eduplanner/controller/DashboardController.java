package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.App;
import com.cab302.eduplanner.appcontext.UserSession;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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

    private enum SortMode { DUE_DATE, ALPHA, GROUPED_SUBJECT }
    private SortMode sortMode = SortMode.DUE_DATE;

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


        sortButton.setOnAction(e -> { cycleSort(); render(); });
        newButton.setOnAction(e -> info("New task flow TBD"));
        calendarButton.setOnAction(e -> info("Tasks Calendar view TBD"));
        detailButton.setOnAction(e -> info("Tasks detailed view TBD"));

        flashcardsTile.setOnAction(e -> navigate("/com/cab302/eduplanner/flashcard.fxml", "EduPlanner — Flashcards"));
        notesTile.setOnAction( e -> navigate("/com/cab302/eduplanner/note.fxml", "EduPlanner — Notes"));
        pomodoroTile.setDisable(true);
        darkTile.setDisable(true);
        rubricTile.setOnAction(e -> navigate("/com/cab302/eduplanner/rubric.fxml", "EduPlanner — Rubric Analysis"));
        rubricTile.setDisable(false);

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

    private void render() {
        sortButton.setText(switch (sortMode) {
            case DUE_DATE -> "[Sort: Due Date]";
            case ALPHA -> "[Sort: Alphabetical]";
            case GROUPED_SUBJECT -> "[Sort: Grouped by Subject]";
        });

        cardsBox.getChildren().clear();
        emptyLabel.setVisible(true);
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
