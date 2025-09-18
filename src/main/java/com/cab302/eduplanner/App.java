package com.cab302.eduplanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Boots the JavaFX application and provides helpers for scene management.
 */
public class App extends Application {

    /**
     * Displays the login screen and applies the shared stylesheet.
     *
     * @param stage primary window provided by the JavaFX runtime
     * @throws IOException if the FXML file or stylesheet cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        DatabaseConnection.initSchema();

        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/com/cab302/eduplanner/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 854, 480);
        scene.getStylesheets().add(App.class.getResource("/com/cab302/eduplanner/styles/app.css").toExternalForm());
        stage.setTitle("EduPlanner — Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args command line arguments provided to the JVM
     */
    public static void main(String[] args) {
        launch();
    }

    /**
     * Utility for swapping the current scene while keeping window configuration consistent.
     *
     * @param stage window whose scene should change
     * @param fxml path to the FXML resource to display
     * @param title window title to display after the switch
     * @throws IOException if the requested FXML cannot be loaded
     */
    public static void changeScene(Stage stage, String fxml, String title) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);

        scene.getStylesheets().add(App.class.getResource("/com/cab302/eduplanner/styles/app.css").toExternalForm());
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }
}
