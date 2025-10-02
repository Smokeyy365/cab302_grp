package com.cab302.eduplanner.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** ISO helpers for YYYY-MM-DD from SQLite. */
public final class DateUtil {
    private DateUtil() {}

    /** Parses YYYY-MM-DD or returns null. */
    public static LocalDate parseIsoDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s.trim());
    }

    /** Parses SQLite datetime TEXT ("YYYY-MM-DD HH:MM:SS") to LocalDateTime or null. */
    public static LocalDateTime parseIsoDateTimeOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        // SQLite often returns a space between date and time; LocalDateTime expects 'T'
        return LocalDateTime.parse(s.trim().replace(' ', 'T'));
    }

    /** Formats LocalDate to YYYY-MM-DD for DB writes (or null). */
    public static String toIso(LocalDate d) {
        return d == null ? null : d.toString();
    }
}
