package com.cab302.eduplanner;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AppLauncher {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}