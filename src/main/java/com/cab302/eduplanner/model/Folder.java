package com.cab302.eduplanner.model;

import java.util.ArrayList;
import java.util.List;

public class Folder {
    private String name;
    private List<Note> notes = new ArrayList<>();

    public Folder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void addNote(Note note) {
        notes.add(note);
    }

    public void removeNote(Note note) {
        notes.remove(note);
    }

    @Override
    public String toString() {
        return name;
    }
}
