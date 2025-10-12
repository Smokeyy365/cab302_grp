package com.cab302.eduplanner.model;

/**
 * Represents a single flashcard with a question and answer.
 */
public class Flashcard {

    private String question;
    private String answer;

    /**
     * Constructs a flashcard with the given question and answer.
     * @param question the question text
     * @param answer the answer text
     */
    public Flashcard(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    /**
     * Gets the question text.
     * @return the question
     */
    public String getQuestion() {
        return question;
    }

    /**
     * Gets the answer text.
     * @return the answer
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * Sets the question text.
     * @param question the question
     */
    public void setQuestion(String question) {
        this.question = question;
    }

    /**
     * Sets the answer text.
     * @param answer the answer
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
