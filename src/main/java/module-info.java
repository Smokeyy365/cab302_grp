module com.cab302.eduplanner {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // for reflection access by FXML
    opens com.cab302.eduplanner.controller to javafx.fxml;
    opens com.cab302.eduplanner.controller.components to javafx.fxml;
    opens com.cab302.eduplanner.model to javafx.fxml;

    // normal exports (public API)
    exports com.cab302.eduplanner;
    exports com.cab302.eduplanner.controller;
    exports com.cab302.eduplanner.model;

    // Google Integration
    exports com.cab302.eduplanner.integration.google;

    // Required for Google OAuth
    requires jdk.httpserver;

}