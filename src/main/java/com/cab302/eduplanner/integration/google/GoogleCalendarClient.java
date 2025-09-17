package com.cab302.eduplanner.integration.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class GoogleCalendarClient {
    private static final String appName = "EduPlanner";
    private final Calendar services;

    public GoogleCalendarClient(String appUserId) {
        try {
            var http = GoogleNetHttpTransport.newTrustedTransport();
            var cred = new GoogleAuthClient().getCredential(appUserId);
            this.services = new Calendar.Builder(http, GsonFactory.getDefaultInstance(), cred)
                    .setApplicationName(appName)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Calendar service init failed", e);
        }
    }

    /** Create an event titled "Study: {title}" starting now, lasting {minutes}. */
    public Event createQuickStudyEvent(String title, int minutes) {
        try {
            ZonedDateTime startZdt = ZonedDateTime.now();
            ZonedDateTime endZdt = startZdt.plusMinutes(minutes);
            String tz = ZoneId.systemDefault().getId();

            Event event = new Event()
                    .setSummary("Study: " + title)
                    .setDescription("Created by EduPlanner");

            EventDateTime start = new EventDateTime()
                    .setDateTime(new DateTime(startZdt.toInstant().toEpochMilli()))
                    .setTimeZone(tz);
            EventDateTime end = new EventDateTime()
                    .setDateTime(new DateTime(endZdt.toInstant().toEpochMilli()))
                    .setTimeZone(tz);
            event.setStart(start);
            event.setEnd(end);

            return services.events().insert("primary", event).execute();
        } catch (Exception e) {
            throw new RuntimeException("createQuickStudyEvent failed", e);
        }
    }

    public List<Event> listUpcoming(int n) {
        try {
            DateTime now = new DateTime(System.currentTimeMillis());
            Events events = services.events().list("primary")
                    .setMaxResults(n)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            return events.getItems();
        } catch (Exception e) {
            throw new RuntimeException("listUpcoming failed", e);
        }
    }
}
