module com.cab302.eduplanner {
    requires javafx.controls;
    requires javafx.fxml;

    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;

    opens com.cab302.eduplanner to javafx.fxml;
    exports com.cab302.eduplanner;
}