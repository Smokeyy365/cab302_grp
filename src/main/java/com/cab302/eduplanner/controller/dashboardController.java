package com.cab302.eduplanner.controller;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
public class DashboardController {
    @FXML private Label nextTaskLabel;
    public void initialize() { nextTaskLabel.setText("Welcome back — no tasks due today 🎉"); }
}
