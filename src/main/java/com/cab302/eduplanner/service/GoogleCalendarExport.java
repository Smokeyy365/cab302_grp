package com.cab302.eduplanner.service;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Calendar export and Google Calendar */
public class GoogleCalendarExport {


    private static final DateTimeFormatter UTC_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    /** Export Link Creation  */
    public record Event(
            String uid,
            String title,
            ZonedDateTime start,
            ZonedDateTime end,
            String location,
            String description
    ) {}

    /** Write events to an .ics file (UTF-8, CRLF, folding, DTSTAMP) */
    public File exportToIcs(File target, List<Event> events) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8)) {
            writeln(w, "BEGIN:VCALENDAR");
            writeln(w, "VERSION:2.0");
            writeln(w, "PRODID:-//EduPlanner//GoogleCalendarExport//EN");
            writeln(w, "CALSCALE:GREGORIAN");
            writeln(w, "METHOD:PUBLISH");
            // Optional nice-to-have name:
            writeln(w, "X-WR-CALNAME:EduPlanner Tasks");

            for (Event e : events) {
                writeln(w, "BEGIN:VEVENT");
                // UID should be stable to avoid duplicates on re-import
                prop(w, "UID", e.uid());

                // DTSTAMP = time we generated the event
                String nowUtc = ZonedDateTime.now(ZoneId.of("UTC")).format(UTC_STAMP);
                prop(w, "DTSTAMP", nowUtc);

                // SUMMARY / LOCATION / DESCRIPTION (escaped + folded)
                if (e.title() != null && !e.title().isBlank()) {
                    prop(w, "SUMMARY", escText(e.title()));
                }
                if (e.location() != null && !e.location().isBlank()) {
                    prop(w, "LOCATION", escText(e.location()));
                }
                if (e.description() != null && !e.description().isBlank()) {
                    prop(w, "DESCRIPTION", escText(e.description()));
                }

                // UTC format
                String dtStart = e.start().withZoneSameInstant(ZoneId.of("UTC")).format(UTC_STAMP);
                String dtEnd   = e.end()  .withZoneSameInstant(ZoneId.of("UTC")).format(UTC_STAMP);
                prop(w, "DTSTART", dtStart);
                prop(w, "DTEND",   dtEnd);

                // Optional quality-of-life defaults
                prop(w, "STATUS", "CONFIRMED");
                prop(w, "SEQUENCE", "0");
                writeln(w, "END:VEVENT");
            }
            writeln(w, "END:VCALENDAR");
        }
        return target;
    }


    /** Open URL using default browser */
    public static void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new URI(url));
            else System.out.println("Open this URL manually:\n" + url);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Convenience: open Google Calendar’s Import page */
    public static void openGoogleImportPage() {
        openInBrowser("https://calendar.google.com/calendar/u/0/r/settings/import");
    }

    /* ---------- Helpers ---------- */

    private static void writeln(Writer w, String line) throws IOException {
        w.write(line);
        w.write("\r\n"); // CRLF required by RFC5545
    }

    private static void prop(Writer w, String name, String value) throws IOException {
        if (value == null) return;
        // Fold long content lines: initial line ≤73 chars, then continuation lines start with a single space
        String line = name + ":" + value;
        int max = 73;
        int i = 0;
        while (i < line.length()) {
            int end = Math.min(i + max, line.length());
            String chunk = line.substring(i, end);
            if (i == 0) writeln(w, chunk);
            else        writeln(w, " " + chunk);
            i = end;
            // After first chunk, continuation lines can be a bit longer (still keep it simple)
            max = 73;
        }
    }

    /**
     * Escape TEXT values per RFC5545: backslash, comma, semicolon, newline.
     * Normalize CRLF/CR to LF then replace LF with \\n.
     */
    private static String escText(String s) {
        String norm = s.replace("\r\n", "\n").replace("\r", "\n");
        return norm
                .replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n");
    }
}
