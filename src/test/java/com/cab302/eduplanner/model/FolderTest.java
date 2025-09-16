package com.cab302.eduplanner.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FolderTest {

    @Test
    void addNoteStoresNoteInFolder() {
        Folder folder = new Folder("CAB302");
        Note note = new Note("Week 1", "Review lecture notes");

        folder.addNote(note);

        assertEquals(1, folder.getNotes().size());
        assertSame(note, folder.getNotes().getFirst());
    }

    @Test
    void removeNoteDeletesExistingNote() {
        Folder folder = new Folder("CAB302");
        Note note1 = new Note("Week 1", "Review lecture notes");
        Note note2 = new Note("Week 2", "Complete tutorial");
        folder.addNote(note1);
        folder.addNote(note2);

        folder.removeNote(note1);

        assertEquals(1, folder.getNotes().size());
        assertSame(note2, folder.getNotes().getFirst());
        assertFalse(folder.getNotes().contains(note1));
    }
}
