package com.cab302.eduplanner.store;

import com.cab302.eduplanner.model.Folder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** In-memory, app-wide store for Notes (persists while the app is running). */
public final class NoteStore {
    private static final ObservableList<Folder> FOLDERS = FXCollections.observableArrayList();

    private NoteStore() {}

    /** Returns the live list of folders (backed by an ObservableList). */
    public static ObservableList<Folder> getFolders() { return FOLDERS; }

    /** True if the store currently has no folders. */
    public static boolean isEmpty() { return FOLDERS.isEmpty(); }

    /** Utility to clear all data (e.g., for tests). */
    public static void clear() { FOLDERS.clear(); }
}
