package com.cab302.eduplanner;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Simple JavaFX launcher class for demo purposes.
 */
public class AppLauncher {
    /**
     * Label for displaying welcome text.
     */
    @FXML
    private Label welcomeText;

    /**
     * Handles the hello button click event.
     */
    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}