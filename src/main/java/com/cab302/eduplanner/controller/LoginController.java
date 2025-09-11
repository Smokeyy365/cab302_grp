package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.service.AuthService;
import com.cab302.eduplanner.App;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;

    private final AuthService auth = new AuthService();

    @FXML
    private void initialize() {
        messageLabel.setText("");
    }

    @FXML
    private void onLogin() {
        String u = usernameField.getText();
        String p = passwordField.getText();
        if (auth.authenticate(u, p)) {
            openMainUI();
        } else {
            messageLabel.setText("Invalid credentials");
        }
    }

    @FXML
    private void openRegister() {
        try {
            // Get current stage by accessing any node in the scene
            Stage stage = (Stage) registerButton.getScene().getWindow();
            App.changeScene(stage,"/com/cab302/eduplanner/register.fxml",  "EduPlanner — Register");
        } catch (IOException e) {
            messageLabel.setText("Unable to open register UI.");
        }
    }

    @FXML
    private void openLogin() {
        try {
            // Get current stage by accessing any node in the scene
            Stage stage = (Stage) loginButton.getScene().getWindow();
            App.changeScene(stage,"/com/cab302/eduplanner/login.fxml",  "EduPlanner — Login");
        } catch (IOException e) {
            messageLabel.setText("Unable to open login UI.");
        }
    }

    @FXML
    private void onRegister() {
        /**
         * Simple registration logic: check if passwords match, then try to register. If
         * registration is successful, return to login screen. Otherwise, show error message.
         */
        String u = usernameField.getText();
        String p = passwordField.getText();
        String cp = confirmPasswordField.getText();
        if (!p.equals(cp)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }
        boolean ok = auth.register(u, p);
        Stage stage = (Stage) registerButton.getScene().getWindow();
        try {
            App.changeScene(stage,"/com/cab302/eduplanner/login.fxml",  "EduPlanner — Login");
        } catch (IOException e) {
            messageLabel.setText("Unable to return to login UI.");
            return;
        }
        messageLabel.setText(ok ? "Registered. You can log in now." : "Register failed (exists or invalid).");
    }

    private void openMainUI() {
        try {
            // Get current stage by accessing any node in the scene
            Stage stage = (Stage) loginButton.getScene().getWindow();
            App.changeScene(stage,"/com/cab302/eduplanner/flashcard.fxml",  "EduPlanner — Main");
        } catch (IOException e) {
            messageLabel.setText("Unable to open main UI.");
        }
    }
}
