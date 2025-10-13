package com.cab302.eduplanner.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Timeline-based countdown with phase-finish callback.
 */
class PomodoroTimer {
    private final Consumer<Integer> onTick;
    private final BiFinished onFinished;

    private Timeline timeline;
    private boolean autoSwap;
    private int studySeconds;
    private int breakSeconds;
    private Supplier<PomodoroController.Phase> phaseSupplier;
    private int[] remaining;

    @FunctionalInterface
    interface BiFinished {
        void accept(boolean willAutoSwap, boolean swappedToBreak);
    }

    PomodoroTimer(Consumer<Integer> onTick, BiFinished onFinished) {
        this.onTick = onTick;
        this.onFinished = onFinished;
    }

    void start(int initialSeconds,
               boolean autoSwap,
               int studySeconds,
               int breakSeconds,
               Supplier<PomodoroController.Phase> phaseSupplier) {
        stop();
        this.autoSwap = autoSwap;
        this.studySeconds = studySeconds;
        this.breakSeconds = breakSeconds;
        this.phaseSupplier = phaseSupplier;

        remaining = new int[]{initialSeconds};
        onTick.accept(remaining[0]);

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0] -= 1;
            if (remaining[0] <= 0) {
                onTick.accept(0);
                timeline.stop();
                boolean toBreak = (phaseSupplier.get() == PomodoroController.Phase.STUDY);
                onFinished.accept(this.autoSwap, toBreak);
                return;
            } else {
                onTick.accept(remaining[0]);
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.playFromStart();
    }

    void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    void setRemainingToOne() {
        if (remaining != null && remaining[0] > 1) {
            remaining[0] = 1;
        }
    }
}
