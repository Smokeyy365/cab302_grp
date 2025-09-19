// src/test/java/com/cab302/eduplanner/integration/google/GoogleCalendarClientTest.java
package com.cab302.eduplanner.integration.google;

import com.google.api.services.calendar.model.Event;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoogleCalendarClientTest {

    @Test
    void testCreateQuickStudyEvent() {
        // make a fake calendar client
        GoogleCalendarClient client = new GoogleCalendarClient("test-user");

        // create a 10-minute study event
        Event e = client.createQuickStudyEvent("Flashcards Study", 10);

        // check the title
        assertEquals("Flashcards Study", e.getSummary());

        // check start and end times are set
        assertNotNull(e.getStart().getDateTime());
        assertNotNull(e.getEnd().getDateTime());
    }
}

