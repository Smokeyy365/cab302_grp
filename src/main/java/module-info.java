module com.cab302.eduplanner {
    requires javafx.controls;
    requires javafx.fxml;

    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;

    // for reflection access by FXML
    opens com.cab302.eduplanner.controller to javafx.fxml;

    // if you have nested controllers (like controller.components), open them too
    opens com.cab302.eduplanner.controller.components to javafx.fxml;

    // normal exports (public API)
    exports com.cab302.eduplanner;
    exports com.cab302.eduplanner.controller;
}
