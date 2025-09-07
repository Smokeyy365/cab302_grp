package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.service.AuthService;


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
    private void onRegister() {
        String u = usernameField.getText();
        String p = passwordField.getText();
        boolean ok = auth.register(u, p);
        messageLabel.setText(ok ? "Registered. You can log in now." : "Register failed (exists or invalid).");
    }

    private void openMainUI() {
        try {
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/com/cab302/eduplanner/flashcard.fxml"));
            Scene scene = new Scene(loader.load(), 960, 600);
            Stage stage = (Stage) ((Node) usernameField).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("EduPlanner — Flashcards");
        } catch (IOException e) {
            messageLabel.setText("Unable to open main UI.");
        }
    }
}
