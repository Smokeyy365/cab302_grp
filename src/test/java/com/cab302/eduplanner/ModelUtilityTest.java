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