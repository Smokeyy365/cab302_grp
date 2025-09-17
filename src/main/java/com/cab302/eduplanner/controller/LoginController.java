package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.service.AuthService;
import com.cab302.eduplanner.App;
import com.cab302.eduplanner.appcontext.UserSession;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;
    @FXML private VBox authContainer;

    private final AuthService auth = new AuthService();

    @FXML
    private void initialize() {
        messageLabel.setText("");

        // -------- LOGIN FLOW --------
        if (usernameField != null && passwordField != null && confirmPasswordField == null) {
            usernameField.setOnAction(e -> passwordField.requestFocus());
            usernameField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.DOWN) {
                    passwordField.requestFocus();
                }
            });

            passwordField.setOnAction(this::onLogin);
            passwordField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.UP) {
                    usernameField.requestFocus();
                }
            });
        }

        // -------- REGISTER FLOW --------
        if (usernameField != null && passwordField != null && confirmPasswordField != null) {
            usernameField.setOnAction(e -> passwordField.requestFocus());
            usernameField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.DOWN) {
                    passwordField.requestFocus();
                }
            });

            passwordField.setOnAction(e -> confirmPasswordField.requestFocus());
            passwordField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.UP) {
                    usernameField.requestFocus();
                } else if (e.getCode() == KeyCode.DOWN) {
                    confirmPasswordField.requestFocus();
                }
            });

            confirmPasswordField.setOnAction(this::onRegister);
            confirmPasswordField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.UP) {
                    passwordField.requestFocus();
                }
            });
        }

        // If we have the authContainer (register screen), bind left/right padding to 30% of scene width
        if (authContainer != null) {
            authContainer.sceneProperty().addListener((sObs, oldScene, newScene) -> {
                if (newScene == null) return;
                newScene.widthProperty().addListener((wObs, oldW, newW) -> {
                    double horizontal = newW.doubleValue() * 0.3;
                    authContainer.setPadding(new javafx.geometry.Insets(20, horizontal, 20, horizontal));
                });
                double initialWidth = newScene.getWidth() > 0 ? newScene.getWidth() : authContainer.getWidth();
                double horizontal = initialWidth * 0.3;
                authContainer.setPadding(new javafx.geometry.Insets(20, horizontal, 20, horizontal));
            });
        }
    }

    // -------- LOGIN --------
    @FXML
    private void onLogin(ActionEvent event) {
        String u = usernameField.getText();
        String p = passwordField.getText();

        // Uses AuthService instead of re-querying
        if (auth.authenticate(u, p)) {
            UserSession.setCurrentUser(auth.getCurrentUser());
            openMainUI(event);
        } else {
            messageLabel.setText("Invalid credentials");
        }
    }

    private void openMainUI(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            App.changeScene(stage,"/com/cab302/eduplanner/dashboard.fxml", "EduPlanner — Dashboard");
            stage.centerOnScreen();
        } catch (IOException e) {
            messageLabel.setText("Unable to open main UI.");
        }
    }

    // -------- NAVIGATION --------
    @FXML
    private void openRegister(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            App.changeScene(stage,"/com/cab302/eduplanner/register.fxml", "EduPlanner — Register");
        } catch (IOException e) {
            messageLabel.setText("Unable to open register UI.");
        }
    }

    @FXML
    private void openLogin(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            App.changeScene(stage,"/com/cab302/eduplanner/login.fxml", "EduPlanner — Login");
        } catch (IOException e) {
            messageLabel.setText("Unable to open login UI.");
        }
    }

    // -------- REGISTER --------
    @FXML
    private void onRegister(ActionEvent event) {
        String u = usernameField.getText();
        String email = emailField.getText();
        String fname = firstNameField.getText();
        String lname = lastNameField.getText();
        String p = passwordField.getText();
        String cp = confirmPasswordField.getText();

        if (!p.equals(cp)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        if (u.isBlank() || email.isBlank() || fname.isBlank() || lname.isBlank() || p.isBlank()) {
            messageLabel.setText("All fields are required.");
            return;
        }

        boolean ok = auth.register(u, email, fname, lname, p);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        try {
            App.changeScene(stage,"/com/cab302/eduplanner/login.fxml", "EduPlanner — Login");
        } catch (IOException e) {
            messageLabel.setText("Unable to return to login UI.");
            return;
        }

        messageLabel.setText(ok ? "Registered. You can log in now." : "Register failed (exists or invalid).");
    }
}
