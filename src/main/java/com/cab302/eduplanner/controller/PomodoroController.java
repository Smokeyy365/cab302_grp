package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.App;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the Pomodoro widget: setup, run, and phase transitions.
 */
public class PomodoroController {

    // Header
    @FXML private Button dashboardButton;

    // Setup
    @FXML private VBox setupPane;
    @FXML private VBox runningPane;
    @FXML private CheckBox autoSwapCheck;
    @FXML private Button startButton;

    // Running
    @FXML private Label phaseLabel;
    @FXML private Label timeLabel;
    @FXML private Button finishButton;
    @FXML private Button nextPhaseButton;
    @FXML private Button stopButton;

    private ToggleGroup studyGroup;
    private ToggleGroup breakGroup;

    // State
    private final IntegerProperty remainingSeconds = new SimpleIntegerProperty(0);
    private final IntegerProperty totalSeconds = new SimpleIntegerProperty(0);
    private final ObjectProperty<Phase> currentPhase = new SimpleObjectProperty<>(Phase.STUDY);
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private PomodoroTimer timer;

    public enum Phase { STUDY, BREAK }

    @FXML
    private void initialize() {
        timer = new PomodoroTimer(this::onTick, this::onFinished);

        studyGroup = new ToggleGroup();
        breakGroup = new ToggleGroup();
        wireToggleGroup(childrenToggleButtons(getRowHBox(setupPane, 0)), studyGroup);
        wireToggleGroup(childrenToggleButtons(getRowHBox(setupPane, 1)), breakGroup);

        selectToggleByText(studyGroup, "30");
        selectToggleByText(breakGroup, "5");

        startButton.disableProperty().bind(
                Bindings.createBooleanBinding(() ->
                                studyGroup.getSelectedToggle() == null || breakGroup.getSelectedToggle() == null,
                        studyGroup.selectedToggleProperty(), breakGroup.selectedToggleProperty()));

        nextPhaseButton.setVisible(false);
        nextPhaseButton.setManaged(false);

        showSetup();
        updateTimeLabel(0);
    }

    private HBox getRowHBox(VBox setup, int whichRow) {
        VBox section = (VBox) setup.getChildren().get(whichRow);
        return (HBox) section.getChildren().get(1);
    }

    private List<ToggleButton> childrenToggleButtons(HBox h) {
        return h.getChildren().stream()
                .filter(n -> n instanceof ToggleButton)
                .map(n -> (ToggleButton) n)
                .collect(Collectors.toList());
    }

    private void wireToggleGroup(List<ToggleButton> buttons, ToggleGroup group) {
        for (ToggleButton b : buttons) b.setToggleGroup(group);
    }

    private void selectToggleByText(ToggleGroup g, String text) {
        for (Toggle t : g.getToggles()) {
            if (t instanceof ToggleButton tb && tb.getText().equals(text)) {
                g.selectToggle(tb);
                break;
            }
        }
    }

    @FXML
    private void handleDashboardButtonAction() {
        stopAndReset();
        try {
            Stage stage = (Stage) dashboardButton.getScene().getWindow();
            App.changeScene(stage, "/com/cab302/eduplanner/dashboard.fxml", "EduPlanner — Dashboard");
        } catch (IOException ex) {
            System.err.println("Failed to return to dashboard: " + ex.getMessage());
        }
    }

    // Actions

    @FXML
    private void handleStart() {
        showNextPhaseButton(false, null);
        startPhase(Phase.STUDY);
    }

    @FXML
    private void handleStop() {
        stopAndReset();
        showSetup();
    }

    @FXML
    private void handleFinishPhase() {
        timer.setRemainingToOne();
    }

    @FXML
    private void handleStartNextPhase() {
        Phase next = (currentPhase.get() == Phase.STUDY) ? Phase.BREAK : Phase.STUDY;
        showNextPhaseButton(false, null);
        startPhase(next);
    }

    private void startPhase(Phase phase) {
        int studySec = selectedMinutes(studyGroup) * 60;
        int breakSec = selectedMinutes(breakGroup) * 60;

        currentPhase.set(phase);
        updatePhaseLabel();

        int startSeconds = (phase == Phase.STUDY ? studySec : breakSec);
        totalSeconds.set(startSeconds);
        remainingSeconds.set(startSeconds);

        running.set(true);
        showRunning();

        timer.start(startSeconds, autoSwapCheck.isSelected(), studySec, breakSec, this::currentPhase);
    }

    private void stopAndReset() {
        running.set(false);
        timer.stop();
        remainingSeconds.set(0);
        totalSeconds.set(0);
        currentPhase.set(Phase.STUDY);
        updateTimeLabel(0);
        showNextPhaseButton(false, null);
    }

    // Timer callbacks

    private void onTick(int remaining) {
        remainingSeconds.set(remaining);
        updateTimeLabel(remaining);
    }

    /**
     * Phase end handler.
     * Auto-swap ON: start next phase immediately.
     * Auto-swap OFF: stay on running pane and show "Start Next Phase".
     */
    private void onFinished(boolean willAutoSwap, boolean swappedToBreak) {
        if (!willAutoSwap) {
            Phase next = (currentPhase.get() == Phase.STUDY) ? Phase.BREAK : Phase.STUDY;
            running.set(false);
            showRunning();
            updateTimeLabel(0);
            showNextPhaseButton(true, next);
            return;
        }
        Phase next = swappedToBreak ? Phase.BREAK : Phase.STUDY;
        startPhase(next);
    }

    private void showNextPhaseButton(boolean show, Phase next) {
        if (!show) {
            nextPhaseButton.setVisible(false);
            nextPhaseButton.setManaged(false);
            return;
        }
        nextPhaseButton.setText(next == Phase.BREAK ? "Start Break" : "Start Study");
        nextPhaseButton.setVisible(true);
        nextPhaseButton.setManaged(true);
    }

    // Helpers

    private void updatePhaseLabel() {
        phaseLabel.setText("Phase: " + (currentPhase.get() == Phase.STUDY ? "STUDY" : "BREAK"));
    }

    private void updateTimeLabel(int seconds) {
        int mm = seconds / 60;
        int ss = seconds % 60;
        timeLabel.setText(String.format("%d:%02d", mm, ss));
    }

    private int selectedMinutes(ToggleGroup g) {
        String text = ((ToggleButton) g.getSelectedToggle()).getText();
        return Integer.parseInt(text);
    }

    private void showSetup() {
        setupPane.setVisible(true);
        setupPane.setManaged(true);
        runningPane.setVisible(false);
        runningPane.setManaged(false);
    }

    private void showRunning() {
        setupPane.setVisible(false);
        setupPane.setManaged(false);
        runningPane.setVisible(true);
        runningPane.setManaged(true);
        updatePhaseLabel();
    }

    private Phase currentPhase() {
        return currentPhase.get();
    }
}
