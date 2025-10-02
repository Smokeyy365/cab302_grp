module com.cab302.eduplanner {
    requires javafx.controls;
    requires javafx.fxml;

    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires jbcrypt;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    // for reflection access by FXML
    opens com.cab302.eduplanner.controller to javafx.fxml;
    opens com.cab302.eduplanner.controller.components to javafx.fxml;
    opens com.cab302.eduplanner.model to javafx.fxml;

    // normal exports (public API)
    exports com.cab302.eduplanner;
    exports com.cab302.eduplanner.controller;
    exports com.cab302.eduplanner.model;
}
