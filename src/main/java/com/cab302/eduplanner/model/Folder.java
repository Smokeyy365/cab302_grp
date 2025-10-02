package com.cab302.eduplanner.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a folder containing notes.
 */
public class Folder {
    private String name;
    private List<Note> notes = new ArrayList<>();

    /**
     * Constructs a folder with the given name.
     * @param name the folder name
     */
    public Folder(String name) {
        this.name = name;
    }

    /**
     * Gets the folder name.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the folder name.
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the list of notes in the folder.
     * @return the notes
     */
    public List<Note> getNotes() {
        return notes;
    }

    /**
     * Adds a note to the folder.
     * @param note the note to add
     */
    public void addNote(Note note) {
        notes.add(note);
    }

    /**
     * Removes a note from the folder.
     * @param note the note to remove
     */
    public void removeNote(Note note) {
        notes.remove(note);
    }

    /**
     * Returns the folder name as a string.
     * @return the folder name
     */
    @Override
    public String toString() {
        return name;
    }
}
