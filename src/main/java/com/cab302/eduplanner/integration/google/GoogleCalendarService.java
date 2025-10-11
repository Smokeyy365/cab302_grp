package com.cab302.eduplanner.integration.google;

// Libraries for Google Calendar integration
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.drive.DriveScopes;

import java.io.*;
import java.util.Arrays;
import java.util.List;

// Helps connect to Google Calendar
public class GoogleCalendarService {

    // App title display
    private static final String appTitle = "EduPlanner";

    // Converts between json and java
    private static final JsonFactory jsonConverter = GsonFactory.getDefaultInstance();

    // Save log in path
    private static final String tokens = "tokens";

    // Ask permission
    private static final List<String> scopes = Arrays.asList(
            DriveScopes.DRIVE_FILE,
            "https://www.googleapis.com/auth/calendar"
    );

    // Google credentials file location
    private static final String credPath = "/credentials.json";

    // Calendar connection
    private Calendar calendarService;

    // Run when creating new Google Calendar service
    // e.g. GoogleCalendarService myCalendar = new GoogleCalendarService();

    public GoogleCalendarService() throws Exception {
        // Set up the connection to Google Calendar
        this.calendarService = initializeCalendarService();
    }

    // Gets Permission from Google
    private static Credential getCredentials(final NetHttpTransport transport) throws IOException {

        // Try to load the json file (credentials.json)
        //InputStream reads data from json file
        InputStream jsonReader = GoogleCalendarService.class.getResourceAsStream(credPath);

        // If the file doesn't exist, throw an error
        if (jsonReader == null) {
            throw new FileNotFoundException("Credentials file not found: " + credPath);
        }

        // Read the credentials file
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonConverter, new InputStreamReader(jsonReader));

        // Create a process of getting permission
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport,        // How data is sent
                jsonConverter,    // How we format data
                clientSecrets,    // Client's profile
                scopes)           // Type of permission (Drive and Calendar)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokens)))
                .setAccessType("offline") // request refresh tokens
                .build();

        // Set up a local web server to receive the login response
        // Port 8888 is the local host
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        // Check if saved tokens are present, open browser if not
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    // Initialize Calendar service
    private static Calendar initializeCalendarService() throws Exception {
        // Create a secure internet connection via HTTP
        final NetHttpTransport httpConnection = GoogleNetHttpTransport.newTrustedTransport();

        // Build and return the calendar service
        return new Calendar.Builder(httpConnection, jsonConverter, getCredentials(httpConnection))
                .setApplicationName(appTitle)
                .build();
    }

    // List upcoming events from the calendar
    public List<Event> listUpcomingEvents(int maxResults) throws IOException {
        // Get current time
        DateTime now = new DateTime(System.currentTimeMillis());

        // Query Google Calendar for upcoming events
        Events events = calendarService.events().list("primary")
                .setMaxResults(maxResults)           // Maximum number of events to return
                .setTimeMin(now)                     // Only events from now onwards
                .setOrderBy("startTime")             // Sort by start time
                .setSingleEvents(true)               // Expand recurring events
                .execute();

        // Return the list of events
        return events.getItems();
    }

    // Create a new event on the calendar
    public String createEvent(String summary, String description, String startDateTime, String endDateTime) throws IOException {
        // Create a new event object
        Event event = new Event()
                .setSummary(summary)           // Event title
                .setDescription(description);  // Event description

        // Set start time
        EventDateTime start = new EventDateTime() // Create new  start date/time
                .setDateTime(new DateTime(startDateTime)) // Set date
                .setTimeZone("Australia/Brisbane");  // Set timezone
        event.setStart(start);

        // Set end time
        EventDateTime end = new EventDateTime() // Create new  end date/time
                .setDateTime(new DateTime(endDateTime)) // Set date
                .setTimeZone("Australia/Brisbane");  // Set  timezone
        event.setEnd(end);

        // Insert the event into the primary calendar
        Event createdEvent = calendarService.events().insert("primary", event).execute();

        // Return the event's unique ID
        return createdEvent.getId();
    }

    // Update an existing event
    public void updateEvent(String eventId, String newSummary, String newDescription) throws IOException {
        // Get the existing event
        Event event = calendarService.events().get("primary", eventId).execute();

        // Update the fields
        event.setSummary(newSummary);
        event.setDescription(newDescription);

        // Save the changes
        calendarService.events().update("primary", eventId, event).execute();
    }

    // Delete an event from the calendar
    public void deleteEvent(String eventId) throws IOException {
        calendarService.events().delete("primary", eventId).execute();
    }

    // Get the calendar service
    public Calendar getCalendarService() {
        return calendarService;
    }
}


