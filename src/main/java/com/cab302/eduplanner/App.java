package com.cab302.eduplanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Loads flashcard.fxml from resources/com/cab302/eduplanner/
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 854, 480);
        scene.getStylesheets().add(App.class.getResource("styles/app.css").toExternalForm());
        stage.setTitle("EduPlanner — Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public static void changeScene(Stage stage, String fxml, String title) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml));
    Scene scene = new Scene(fxmlLoader.load(), 1280, 720);

    scene.getStylesheets().add(App.class.getResource("styles/app.css").toExternalForm());
    stage.setTitle(title);
    stage.setScene(scene);
    stage.setResizable(false);
    stage.centerOnScreen();
    stage.show();
    }
}
