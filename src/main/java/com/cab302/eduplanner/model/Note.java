package com.cab302.eduplanner.model;

/**
 * Represents a note with a title and content.
 */
public class Note {
    private String title;
    private String content;

    /**
     * Constructs a note with the given title and content.
     * @param title the note title
     * @param content the note content
     */
    public Note(String title, String content) {
        this.title = title;
        this.content = content;
    }

    /**
     * Gets the note title.
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the note content.
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the note title.
     * @param title the title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets the note content.
     * @param content the content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns the note title as a string.
     * @return the note title
     */
    @Override
    public String toString() {
        return title;
    }
}
