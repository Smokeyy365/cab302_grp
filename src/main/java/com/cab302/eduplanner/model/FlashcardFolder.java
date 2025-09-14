package com.cab302.eduplanner.model;

import java.util.ArrayList;
import java.util.List;

public class FlashcardFolder {
    private String name;
    private final List<FlashcardDeck> decks = new ArrayList<>();

    public FlashcardFolder(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<FlashcardDeck> getDecks() { return decks; }
}
