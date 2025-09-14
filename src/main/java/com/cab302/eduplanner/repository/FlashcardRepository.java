package com.cab302.eduplanner.repository;

import com.cab302.eduplanner.model.FlashcardFolder;
import java.util.ArrayList;
import java.util.List;

public class FlashcardRepository {
    private static final List<FlashcardFolder> folders = new ArrayList<>();

    private FlashcardRepository() {} // prevent instantiation

    public static List<FlashcardFolder> getFolders() {
        return folders;
    }

    public static void clear() {
        folders.clear();
    }
}
