module com.cab302.eduplanner {
    requires javafx.controls;
    requires javafx.fxml;

    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires jbcrypt;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires com.fasterxml.jackson.databind;
    requires okhttp3;
    requires org.apache.pdfbox;
    requires org.apache.poi.ooxml;
    requires org.apache.pdfbox.io;

    // for reflection access by FXML
    opens com.cab302.eduplanner.controller to javafx.fxml;
    opens com.cab302.eduplanner.controller.components to javafx.fxml;
    opens com.cab302.eduplanner.model to javafx.fxml, com.fasterxml.jackson.databind;

    // normal exports (public API)
    exports com.cab302.eduplanner;
    exports com.cab302.eduplanner.controller;
    exports com.cab302.eduplanner.model;
}
