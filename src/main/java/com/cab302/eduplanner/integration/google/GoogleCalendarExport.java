package com.cab302.eduplanner.service;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Calendar export + Google Calendar link helper (no JavaFX deps). */
public class GoogleCalendarExport {

    private static final DateTimeFormatter ICS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    /** Event DTO used for export/link creation. */
    public record Event(
            String uid,
            String title,
            ZonedDateTime start,
            ZonedDateTime end,
            String location,
            String description
    ) {}

    /** Write events to an .ics file (timestamps stored as UTC). */
    public File exportToIcs(File target, List<Event> events) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(target))) {
            w.write("BEGIN:VCALENDAR\r\n");
            w.write("VERSION:2.0\r\n");
            w.write("PRODID:-//EduPlanner//GoogleCalendarExport//EN\r\n");
            w.write("CALSCALE:GREGORIAN\r\n");
            w.write("METHOD:PUBLISH\r\n");
            for (Event e : events) {
                w.write("BEGIN:VEVENT\r\n");
                w.write("UID:" + e.uid() + "\r\n");
                w.write("SUMMARY:" + esc(e.title()) + "\r\n");
                if (e.location() != null && !e.location().isBlank()) w.write("LOCATION:" + esc(e.location()) + "\r\n");
                if (e.description() != null && !e.description().isBlank()) w.write("DESCRIPTION:" + esc(e.description()) + "\r\n");
                w.write("DTSTART:" + e.start().withZoneSameInstant(ZoneId.of("UTC")).format(ICS) + "\r\n");
                w.write("DTEND:"   + e.end().withZoneSameInstant(ZoneId.of("UTC")).format(ICS)   + "\r\n");
                w.write("END:VEVENT\r\n");
            }
            w.write("END:VCALENDAR\r\n");
        }
        return target;
    }

    /** Build a Google Calendar “Create event” link (no API keys needed). */
    public static String createGoogleLink(String title, String description,
                                          ZonedDateTime start, ZonedDateTime end) {
        var fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        String s = start.withZoneSameInstant(ZoneId.of("UTC")).format(fmt);
        String e = end  .withZoneSameInstant(ZoneId.of("UTC")).format(fmt);
        StringBuilder url = new StringBuilder("https://calendar.google.com/calendar/render?action=TEMPLATE");
        url.append("&text=").append(URLEncoder.encode(title, StandardCharsets.UTF_8));
        url.append("&dates=").append(s).append("/").append(e);
        if (description != null && !description.isBlank()) {
            url.append("&details=").append(URLEncoder.encode(description, StandardCharsets.UTF_8));
        }
        return url.toString();
    }

    /** Try to open a URL in the default browser. */
    public static void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new URI(url));
            else System.out.println("Open this URL manually:\n" + url);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
    }
}
