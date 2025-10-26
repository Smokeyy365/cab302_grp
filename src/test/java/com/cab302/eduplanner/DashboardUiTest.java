package com.cab302.eduplanner;

import com.cab302.eduplanner.controller.DashboardController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardUiTest {

    private static String read(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read " + relativePath, ex);
        }
    }

    @Test
    @DisplayName("Dashboard keeps four short motivational quips on hand")
    void motivationalQuipListHasFourEntries() {
        assertEquals(4, DashboardController.motivationalQuips().size(),
                "Expected the dashboard to rotate through four motivational quips");
    }

    @Test
    @DisplayName("Motivational quip selection respects supplied randomness")
    void motivationalQuipSelectionRespectsSeed() {
        String quip = DashboardController.randomMotivationalQuip(new Random(0));
        assertEquals("Your effort today fuels success.", quip,
                "Seeded random should deterministically select the third quip");
    }

    @Test
    @DisplayName("Dashboard header exposes a local date label")
    void headerDeclaresLocalDateLabel() {
        String fxml = read("src/main/resources/com/cab302/eduplanner/dashboard.fxml");
        assertTrue(fxml.contains("fx:id=\"localDateLabel\""),
                "Expected dashboard.fxml to declare a localDateLabel for the header");
    }

    @Test
    @DisplayName("Dashboard header includes a motivational quip label")
    void headerDeclaresMotivationalLabel() {
        String fxml = read("src/main/resources/com/cab302/eduplanner/dashboard.fxml");
        assertTrue(fxml.contains("fx:id=\"motivationalLabel\""),
                "Dashboard header should provide a dedicated motivationalLabel beneath the greeting");
    }

    @Test
    @DisplayName("Dashboard stylesheet defines the local date style hook")
    void stylesheetDefinesLocalDateClass() {
        String css = read("src/main/resources/com/cab302/eduplanner/styles/dashboard.css");
        assertTrue(css.contains(".local-date"),
                "dashboard.css should define the local-date style class");
    }

    @Test
    @DisplayName("Dashboard stylesheet styles motivational quips")
    void stylesheetDefinesMotivationClass() {
        String css = read("src/main/resources/com/cab302/eduplanner/styles/dashboard.css");
        assertTrue(css.contains(".motivation-quip"),
                "dashboard.css should expose a motivation-quip style class for the subtitle");
    }
}