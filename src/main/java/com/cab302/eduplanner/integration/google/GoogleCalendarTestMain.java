package com.cab302.eduplanner.integration.google;

// Libraries
import com.google.api.services.calendar.model.Event;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Testing Menu for Google Calendar

public class GoogleCalendarTestMain {

    public static void main(String[] args) {
        System.out.println("==== EDUPLANNER GOOGLE CALENDAR TEST ====\n");

        try {
            // Connect to Google Calendar
            System.out.println("Connecting to Google Calendar...");
            GoogleCalendarService calendar = new GoogleCalendarService();
            System.out.println("Connection successful!\n");

            // List upcoming events
            System.out.println("Listing upcoming events...");
            List<Event> events = calendar.listUpcomingEvents(20);// max events added

            // Check if there are any events
            if (events.isEmpty()) {
                // No events found

                System.out.println("No upcoming events found.");

            }

            else {

                // Display events if found
                System.out.println("Found " + events.size() + " upcoming events:");

                // Loop through each event
                for (Event event : events) {

                    // Get the event title

                    String eventTitle = event.getSummary();

                    // Get the start time - events can have either a specific time OR just a date
                    String startTime;

                    // Check if event has specific time
                    if (event.getStart().getDateTime() != null) {

                        // This event has a specific time
                        startTime = event.getStart().getDateTime().toString();
                    } else {

                        // This is an all day or doesn't have a specific time
                        startTime = event.getStart().getDate().toString();
                    }

                    // Print the event info
                    System.out.println("  - " + eventTitle + " (starts: " + startTime + ")");
                }
            }
            System.out.println();

            //Create a test event
            System.out.println("Creating a test event...");

            // Get current time
            ZonedDateTime startTime = ZonedDateTime.now().plusHours(1); // plus 1 hour

            // End time
            ZonedDateTime endTime = ZonedDateTime.now().plusHours(2); // plus 2 hours

            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

            String eventId = calendar.createEvent(
                    "EduPlanner Test Event",
                    "This is a test event created by EduPlanner to verify Calendar integration works!",
                    startTime.format(formatter),
                    endTime.format(formatter)
            );

            System.out.println("✓ Event created successfully!");
            System.out.println("  Event ID: " + eventId);
            System.out.println("  Starts: " + startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            System.out.println("  Ends: " + endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            System.out.println("  View at: https://calendar.google.com");
            System.out.println();

            // ALL TESTS PASSED
            System.out.println("=== All Tests Passed ===");
            System.out.println(" Google Calendar integration works");
            System.out.println("\nCheck Google Calendar");
            System.out.println("https://calendar.google.com");

        } catch (Exception e) {
            System.err.println("\n✗ Test Failed");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(); // show error path
        }
    }
}
