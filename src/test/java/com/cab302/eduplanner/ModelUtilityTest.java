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