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

    // PDF export (PDFBox 3.x)
    requires org.apache.pdfbox;
    requires org.apache.pdfbox.io;

    // Optional Office (you already had it)
    requires org.apache.poi.ooxml;

    // Logging
    requires org.apache.logging.log4j;
    // If you added log4j-core to silence warnings:
    // requires org.apache.logging.log4j.core;

    // AWT / file choosers & Desktop
    requires java.desktop;

    // Preferences API for remembering Drive folder
    requires java.prefs;

    // FXML reflection access
    opens com.cab302.eduplanner.controller to javafx.fxml;
    opens com.cab302.eduplanner.controller.components to javafx.fxml;
    opens com.cab302.eduplanner.model to javafx.fxml, com.fasterxml.jackson.databind;

    // API exports
    exports com.cab302.eduplanner;
    exports com.cab302.eduplanner.controller;
    exports com.cab302.eduplanner.model;
}

