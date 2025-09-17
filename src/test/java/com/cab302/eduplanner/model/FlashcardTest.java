package com.cab302.eduplanner.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FlashcardTest {

    @Test
    void testCreateFlashcard() {
        Flashcard flashcard = new Flashcard("What is Cyberpunk 2077?", "A video game");
        assertEquals("What is Cyberpunk 2077?", flashcard.getQuestion());
        assertEquals("A video game", flashcard.getAnswer());
    }

    @Test
    void testEditFlashcard() {
        Flashcard flashcard = new Flashcard("Old Q", "Old A");
        flashcard.setQuestion("New Q");
        flashcard.setAnswer("New A");

        assertEquals("New Q", flashcard.getQuestion());
        assertEquals("New A", flashcard.getAnswer());
    }
}
