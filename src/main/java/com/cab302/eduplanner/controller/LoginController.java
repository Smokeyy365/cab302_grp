package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.service.AuthService;
import com.cab302.eduplanner.App;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;

/**
 * Controller for handling login and registration UI logic.
 * Manages authentication, navigation, and user input validation.
 */
public class LoginController {
    /** Username input field. */
    @FXML private TextField usernameField;
    /** Password input field. */
    @FXML private PasswordField passwordField;
    /** Confirm password input field (for registration). */
    @FXML private PasswordField confirmPasswordField;
    /** Email input field (for registration). */
    @FXML private TextField emailField;
    /** First name input field (for registration). */
    @FXML private TextField firstNameField;
    /** Last name input field (for registration). */
    @FXML private TextField lastNameField;
    /** Login button. */
    @FXML private Button loginButton;
    /** Register button. */
    @FXML private Button registerButton;
    /** Label for displaying messages to the user. */
    @FXML private Label messageLabel;
    /** Container for authentication UI. */
    @FXML private VBox authContainer;

    /** Service for authentication and registration logic. */
    private final AuthService auth = new AuthService();

    /**
     * Initializes the controller, sets up event handlers and UI bindings.
     */
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

    /**
     * Handles login button action. Authenticates the user.
     * @param event the action event
     */
    @FXML
    private void onLogin(ActionEvent event) {
        String u = usernameField.getText();
        String p = passwordField.getText();

        // Uses AuthService instead of re-querying
        if (auth.authenticate(u, p)) {
            openMainUI(event);
        } else {
            messageLabel.setText("Invalid credentials");
        }
    }

    /**
     * Opens the main UI after successful login.
     * @param event the action event
     */
    private void openMainUI(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            App.changeScene(stage,"/com/cab302/eduplanner/dashboard.fxml", "EduPlanner — Dashboard");
            stage.centerOnScreen();
        } catch (IOException e) {
            messageLabel.setText("Unable to open main UI.");
        }
    }

    /**
     * Navigates to the registration screen.
     * @param event the action event
     */
    @FXML
    private void openRegister(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            App.changeScene(stage,"/com/cab302/eduplanner/register.fxml", "EduPlanner — Register");
        } catch (IOException e) {
            messageLabel.setText("Unable to open register UI.");
        }
    }

    /**
     * Navigates to the login screen.
     * @param event the action event
     */
    @FXML
    private void openLogin(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            App.changeScene(stage,"/com/cab302/eduplanner/login.fxml", "EduPlanner — Login");
        } catch (IOException e) {
            messageLabel.setText("Unable to open login UI.");
        }
    }

    /**
     * Handles registration button action. Registers a new user.
     * @param event the action event
     */
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

        AuthService.RegisterResult result = auth.registerWithResult(u, email, fname, lname, p);
        switch (result) {
            case SUCCESS:
                break; // continue to login screen
            case INVALID_INPUT:
                messageLabel.setText("All fields are required.");
                return;
            case USERNAME_TAKEN:
                messageLabel.setText("Username already in use.");
                return;
            case EMAIL_TAKEN:
                messageLabel.setText("Email already in use.");
                return;
            default:
                messageLabel.setText("Registration failed due to an internal error.");
                return;
        }

        // Stays on registration screen on fail
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        try {
            App.changeScene(stage,"/com/cab302/eduplanner/login.fxml", "EduPlanner — Login");
        } catch (IOException e) {
            messageLabel.setText("Unable to return to login UI.");
            return;
        }

        messageLabel.setText("Registered. You can log in now.");
    }
}
