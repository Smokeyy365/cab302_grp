package com.cab302.eduplanner.service;

import com.cab302.eduplanner.integration.openai.OpenAiRubricClient;
import com.cab302.eduplanner.model.RubricAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * High-level orchestration service that converts uploaded documents to text and calls the OpenAI rubric grader.
 */
public class RubricAnalysisService {

    private final DocumentTextExtractor textExtractor;
    private final OpenAiRubricClient openAiClient;

    public RubricAnalysisService() {
        this(new DocumentTextExtractor(),
                new OpenAiRubricClient(new OkHttpClient(), new ObjectMapper(), System.getenv("OPENAI_API_KEY"), "gpt-4.1-mini"));
    }

    public RubricAnalysisService(DocumentTextExtractor textExtractor, OpenAiRubricClient openAiClient) {
        this.textExtractor = Objects.requireNonNull(textExtractor, "textExtractor");
        this.openAiClient = Objects.requireNonNull(openAiClient, "openAiClient");
    }

    public RubricAnalysisResult analyse(Path assignmentPath, Path rubricPath) throws IOException {
        String assignmentText = textExtractor.extractText(assignmentPath);
        String rubricText = textExtractor.extractText(rubricPath);
        return openAiClient.gradeAssignment(rubricText, assignmentText);
    }
}