package com.cab302.eduplanner.repository;

import com.cab302.eduplanner.model.Flashcard;
import com.cab302.eduplanner.model.FlashcardDeck;
import com.cab302.eduplanner.model.FlashcardFolder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FlashcardFileHelper {

    // Setting string lengths for folder names
    private static final String folderPrefix = "FOLDER:";
    private static final String deckPrefix = "  DECK:";
    private static final String qPrefix = "    Q:";
    private static final String aPrefix = "    A:";

    // Convert all flashcards to a simple text format and save to file
    public static File saveToFile(List<FlashcardFolder> folders) throws IOException {
        // Create a temp file
        File tempFile = File.createTempFile("eduPlanner-flashcards-", ".txt");

        // Write file using a buffered UTF-8 writer
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {

            for (FlashcardFolder folder : folders) { // Loop folders
                writer.write(folderPrefix + folder.getName());
                writer.write("\n");

                for (FlashcardDeck deck : folder.getDecks()) { // Loop decks
                    writer.write(deckPrefix + deck.getName());
                    writer.write("\n");

                    for (Flashcard card : deck.getFlashcards()) { // Loop flashcards
                        writer.write(qPrefix + card.getQuestion());
                        writer.write("\n");
                        writer.write(aPrefix + card.getAnswer());
                        writer.write("\n");
                    }
                }
                writer.write("\n"); // Blank line between folders (readability)
            }
        }
        return tempFile; // Give caller file path
    }

    // Load flashcards from a text file
    public static void loadFromFile(File file, List<FlashcardFolder> folders) throws IOException {
        folders.clear();

        FlashcardFolder currentFolder = null; // Track active folder
        FlashcardDeck currentDeck = null;     // Track active deck
        String currentQuestion = null;        // Hold question until answer

        // Read the file one line at a time
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String raw;
            while ((raw = reader.readLine()) != null) {
                String line = raw; // keep original spacing

                // Skip empty/whitespace-only lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Adds Folder
                if (line.startsWith(folderPrefix)) {
                    String folderName = line.substring(folderPrefix.length()).trim();
                    currentFolder = new FlashcardFolder(folderName);
                    folders.add(currentFolder);
                    currentDeck = null;       // reset deck context on new folder
                    currentQuestion = null;   // reset question context

                    // Adds Deck
                } else if (line.startsWith(deckPrefix)) {
                    String deckName = line.substring(deckPrefix.length()).trim();
                    currentDeck = new FlashcardDeck(deckName);
                    if (currentFolder != null) {
                        currentFolder.getDecks().add(currentDeck);
                    }
                    currentQuestion = null;   // reset question context

                    // Question Line
                } else if (line.startsWith(qPrefix)) {
                    currentQuestion = line.substring(qPrefix.length());

                    // Answer Line
                } else if (line.startsWith(aPrefix)) {
                    String answer = line.substring(aPrefix.length());
                    if (currentDeck != null && currentQuestion != null) {
                        currentDeck.getFlashcards().add(new Flashcard(currentQuestion, answer));
                    }
                    currentQuestion = null;
                }

            }
        }
    }
}
