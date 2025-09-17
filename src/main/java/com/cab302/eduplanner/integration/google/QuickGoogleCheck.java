package com.cab302.eduplanner.integration.google;

import com.google.api.services.calendar.model.Event;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class QuickGoogleCheck {
    public static void main(String[] args) throws Exception {
        String userID = "ID-demo";

        GoogleCalendarClient calendar = new GoogleCalendarClient(userID);
        Event flashcardsStudy = calendar.createQuickStudyEvent("Flashcards Study", 10);
        System.out.println("Calendar Event Created " + flashcardsStudy.getHtmlLink());

        File tmp = File.createTempFile("note-", ".txt");
        try (FileWriter fw = new FileWriter(tmp)) {
            fw.write("Testing notes.\n");
            fw.write("Date: " + java.time.LocalDateTime.now());
        }

        DriveStorage drive = new DriveStorage(userID);
        String fileId = drive.upload(tmp, "text/plain");
        System.out.println("Uploaded Google Drive File: https://drive.google.com/file/d/" + fileId + "/view");

        List<Event> events = calendar.listUpcoming(6);
        System.out.println("Next few events:");
        for (Event e : events) {
            System.out.println(" - " + e.getSummary() + " at " + e.getStart().getDateTime());
        }
    }
}
