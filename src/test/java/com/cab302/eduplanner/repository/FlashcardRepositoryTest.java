package com.cab302.eduplanner.repository;

import com.cab302.eduplanner.model.FlashcardDeck;
import com.cab302.eduplanner.model.FlashcardFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlashcardRepositoryTest {

    @BeforeEach
    void setUp() {
        FlashcardRepository.clear();
    }

    @AfterEach
    void tearDown() {
        FlashcardRepository.clear();
    }

    @Test
    void getFoldersReflectsNewlyAddedFolder() {
        List<FlashcardFolder> folders = FlashcardRepository.getFolders();
        FlashcardFolder folder = new FlashcardFolder("Test Folder");
        FlashcardDeck deck = new FlashcardDeck("Deck A");
        folder.getDecks().add(deck);

        folders.add(folder);

        assertTrue(FlashcardRepository.getFolders().contains(folder));
        assertEquals(1, FlashcardRepository.getFolders().size());
        assertSame(deck, FlashcardRepository.getFolders().getFirst().getDecks().getFirst());
    }

    @Test
    void clearRemovesAllFolders() {
        FlashcardRepository.getFolders().add(new FlashcardFolder("Folder One"));
        FlashcardRepository.getFolders().add(new FlashcardFolder("Folder Two"));

        FlashcardRepository.clear();

        assertTrue(FlashcardRepository.getFolders().isEmpty(), "Repository should be empty after clear()");
    }
}
