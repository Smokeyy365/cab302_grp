package com.cab302.eduplanner;

import com.cab302.eduplanner.appcontext.UserSession;
import com.cab302.eduplanner.model.Flashcard;
import com.cab302.eduplanner.model.FlashcardDeck;
import com.cab302.eduplanner.model.FlashcardFolder;
import com.cab302.eduplanner.model.Folder;
import com.cab302.eduplanner.model.Note;
import com.cab302.eduplanner.model.RubricItem;
import com.cab302.eduplanner.model.Task;
import com.cab302.eduplanner.repository.UserRepository;
import com.cab302.eduplanner.util.DateUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Collection of focused unit tests covering value objects and utilities.
 */
class ModelUtilityTest {

    // Reset the UserSession singleton after each test to avoid cross-test contamination.
    @AfterEach
    void resetSession() {
        UserSession.clear();
    }

    // Valid ISO date strings parse into matching LocalDate instances.
    @Test
    void parseIsoDateOrNullReturnsDateForValidInput() {
        LocalDate date = DateUtil.parseIsoDateOrNull("2024-05-18");
        assertEquals(LocalDate.of(2024, 5, 18), date);
    }

    // Non-ISO date formats throw a DateTimeParseException when parsed.
    @Test
    void parseIsoDateOrNullThrowsForInvalidFormat() {
        assertThrows(DateTimeParseException.class, () -> DateUtil.parseIsoDateOrNull("18/05/2024"));
    }

    // Leap-day ISO dates are accepted and produce the expected LocalDate.
    @Test
    void parseIsoDateOrNullHandlesLeapDay() {
        LocalDate date = DateUtil.parseIsoDateOrNull("2020-02-29");
        assertEquals(LocalDate.of(2020, 2, 29), date);
    }

    // Leading and trailing whitespace is trimmed before parsing ISO dates.
    @Test
    void parseIsoDateOrNullTrimsWhitespace() {
        LocalDate date = DateUtil.parseIsoDateOrNull(" 2023-01-02 ");
        assertEquals(LocalDate.of(2023, 1, 2), date);
    }

    // Null or blank inputs produce a null date result instead of throwing.
    @Test
    void parseIsoDateOrNullReturnsNullForNullOrBlank() {
        assertAll(
                () -> assertNull(DateUtil.parseIsoDateOrNull(null)),
                () -> assertNull(DateUtil.parseIsoDateOrNull("   "))
        );
    }

    // ISO date-times with spaces between date and time parse successfully.
    @Test
    void parseIsoDateTimeOrNullHandlesSpaceSeparator() {
        LocalDateTime dateTime = DateUtil.parseIsoDateTimeOrNull("2024-02-29 23:59:59");
        assertEquals(LocalDateTime.of(2024, 2, 29, 23, 59, 59), dateTime);
    }

    // ISO date-times using the T separator are handled correctly.
    @Test
    void parseIsoDateTimeOrNullAcceptsTSeparator() {
        LocalDateTime dateTime = DateUtil.parseIsoDateTimeOrNull("2023-01-01T01:02:03");
        assertEquals(LocalDateTime.of(2023, 1, 1, 1, 2, 3), dateTime);
    }

    // Whitespace around ISO date-time strings is trimmed before parsing.
    @Test
    void parseIsoDateTimeOrNullTrimsWhitespace() {
        LocalDateTime dateTime = DateUtil.parseIsoDateTimeOrNull(" 2020-12-31 00:00:00 ");
        assertEquals(LocalDateTime.of(2020, 12, 31, 0, 0, 0), dateTime);
    }

    // Trailing newline characters are ignored when parsing ISO date-times.
    @Test
    void parseIsoDateTimeOrNullTrimsTrailingNewline() {
        LocalDateTime dateTime = DateUtil.parseIsoDateTimeOrNull("2024-04-04 04:04:04\n");
        assertEquals(LocalDateTime.of(2024, 4, 4, 4, 4, 4), dateTime);
    }

    // Null or blank date-time input returns null rather than throwing errors.
    @Test
    void parseIsoDateTimeOrNullReturnsNullForNullOrBlank() {
        assertAll(
                () -> assertNull(DateUtil.parseIsoDateTimeOrNull(null)),
                () -> assertNull(DateUtil.parseIsoDateTimeOrNull(""))
        );
    }

    // Non-date strings trigger a DateTimeParseException for date-time parsing.
    @Test
    void parseIsoDateTimeOrNullThrowsForInvalidInput() {
        assertThrows(DateTimeParseException.class, () -> DateUtil.parseIsoDateTimeOrNull("invalid"));
    }

    // Formatting LocalDate values to ISO strings preserves content and handles null.
    @Test
    void toIsoHandlesValuesAndNull() {
        assertAll(
                () -> assertEquals("2022-08-15", DateUtil.toIso(LocalDate.of(2022, 8, 15))),
                () -> assertNull(DateUtil.toIso(null))
        );
    }

    // ISO strings round-trip through parsing and formatting without changes.
    @Test
    void toIsoRoundTripsWithParse() {
        LocalDate date = DateUtil.parseIsoDateOrNull("2021-11-09");
        assertEquals("2021-11-09", DateUtil.toIso(date));
    }