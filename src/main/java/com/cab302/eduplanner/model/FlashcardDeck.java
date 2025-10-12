package com.cab302.eduplanner.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a deck of flashcards.
 */
public class FlashcardDeck {
    private String name;
    private final List<Flashcard> flashcards = new ArrayList<>();

    /**
     * Constructs a flashcard deck with the given name.
     * @param name the deck name
     */
    public FlashcardDeck(String name) {
        this.name = name;
    }

    /**
     * Gets the deck name.
     * @return the name
     */
    public String getName() { return name; }
    /**
     * Sets the deck name.
     * @param name the name
     */
    public void setName(String name) { this.name = name; }
    /**
     * Gets the list of flashcards in the deck.
     * @return the flashcards
     */
    public List<Flashcard> getFlashcards() { return flashcards; }
}
