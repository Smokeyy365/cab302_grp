package com.cab302.eduplanner.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a folder containing flashcard decks.
 */
public class FlashcardFolder {
    private String name;
    private final List<FlashcardDeck> decks = new ArrayList<>();

    /**
     * Constructs a flashcard folder with the given name.
     * @param name the folder name
     */
    public FlashcardFolder(String name) {
        this.name = name;
    }

    /**
     * Gets the folder name.
     * @return the name
     */
    public String getName() { return name; }
    /**
     * Sets the folder name.
     * @param name the name
     */
    public void setName(String name) { this.name = name; }
    /**
     * Gets the list of flashcard decks in the folder.
     * @return the decks
     */
    public List<FlashcardDeck> getDecks() { return decks; }
}
