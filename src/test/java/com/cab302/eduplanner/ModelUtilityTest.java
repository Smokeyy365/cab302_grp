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

    // Constructing a Task with arguments correctly populates all fields.
    @Test
    void taskConstructorPopulatesFields() {
        LocalDate dueDate = LocalDate.of(2025, 3, 10);
        Task task = new Task(7L, "CAB302", "Finish assignment", dueDate, "Important", 40, 35.5, 40.0);

        assertAll(
                () -> assertNull(task.getTaskId()),
                () -> assertEquals(7L, task.getUserId()),
                () -> assertEquals("CAB302", task.getSubject()),
                () -> assertEquals("Finish assignment", task.getTitle()),
                () -> assertEquals(dueDate, task.getDueDate()),
                () -> assertEquals("Important", task.getNotes()),
                () -> assertEquals(40, task.getWeight()),
                () -> assertEquals(35.5, task.getAchievedMark()),
                () -> assertEquals(40.0, task.getMaxMark())
        );
    }

    // Using Task setters updates each value and timestamp field as expected.
    @Test
    void taskSettersUpdateValues() {
        Task task = new Task();
        LocalDate dueDate = LocalDate.of(2030, 1, 1);
        LocalDateTime created = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime updated = LocalDateTime.of(2024, 2, 2, 12, 0);

        task.setTaskId(10L);
        task.setUserId(3L);
        task.setSubject("Math");
        task.setTitle("Test");
        task.setDueDate(dueDate);
        task.setNotes("Revise chapters");
        task.setWeight(20);
        task.setAchievedMark(18.0);
        task.setMaxMark(20.0);
        task.setCreatedAt(created);
        task.setUpdatedAt(updated);

        assertAll(
                () -> assertEquals(10L, task.getTaskId()),
                () -> assertEquals(3L, task.getUserId()),
                () -> assertEquals("Math", task.getSubject()),
                () -> assertEquals("Test", task.getTitle()),
                () -> assertEquals(dueDate, task.getDueDate()),
                () -> assertEquals("Revise chapters", task.getNotes()),
                () -> assertEquals(20, task.getWeight()),
                () -> assertEquals(18.0, task.getAchievedMark()),
                () -> assertEquals(20.0, task.getMaxMark()),
                () -> assertEquals(created, task.getCreatedAt()),
                () -> assertEquals(updated, task.getUpdatedAt())
        );
    }

    // The default Task constructor leaves all fields unset for later assignment.
    @Test
    void taskDefaultConstructorLeavesFieldsNull() {
        Task task = new Task();
        assertAll(
                () -> assertNull(task.getTaskId()),
                () -> assertNull(task.getUserId()),
                () -> assertNull(task.getSubject()),
                () -> assertNull(task.getTitle()),
                () -> assertNull(task.getDueDate()),
                () -> assertNull(task.getNotes()),
                () -> assertNull(task.getWeight()),
                () -> assertNull(task.getAchievedMark()),
                () -> assertNull(task.getMaxMark()),
                () -> assertNull(task.getCreatedAt()),
                () -> assertNull(task.getUpdatedAt())
        );
    }

    // Task.toString includes key identifiers such as taskId, userId, and title.
    @Test
    void taskToStringContainsKeyFields() {
        Task task = new Task(1L, "CAB302", "Study", LocalDate.of(2024, 5, 1), null, null, null, null);
        task.setTaskId(5L);
        String output = task.toString();
        assertAll(
                () -> assertTrue(output.contains("taskId=5")),
                () -> assertTrue(output.contains("userId=1")),
                () -> assertTrue(output.contains("title='Study'"))
        );
    }
