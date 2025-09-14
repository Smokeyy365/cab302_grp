package com.cab302.eduplanner.model;

import java.util.ArrayList;
import java.util.List;

public class FlashcardDeck {
    private String name;
    private final List<Flashcard> flashcards = new ArrayList<>();

    public FlashcardDeck(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Flashcard> getFlashcards() { return flashcards; }
}
