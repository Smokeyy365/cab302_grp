package com.cab302.eduplanner.integration.openai;

import com.cab302.eduplanner.model.RubricAnalysisResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

/**
 * Lightweight client wrapper around the OpenAI Responses API for rubric analysis.
 */
public class OpenAiRubricClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    static final String TYPESTRING = "string";
    static final String TYPENUMBER = "number";
    static final String TYPEARRAY = "array";

    public OpenAiRubricClient(OkHttpClient httpClient, ObjectMapper objectMapper, String apiKey, String model) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? "gpt-4.1-mini" : model;
    }

    /**
     * Sends the provided rubric and assignment text to the OpenAI Responses API and returns the structured grading.
     *
     * @param rubricText     normalised rubric description
     * @param assignmentText normalised assignment submission text
     * @return parsed rubric analysis result
     * @throws IOException if the API request fails or the response cannot be parsed
     */
    public RubricAnalysisResult gradeAssignment(String rubricText, String assignmentText) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured. Set the OPENAI_API_KEY environment variable.");
        }

        ObjectNode payload = buildPayload(rubricText, assignmentText);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/responses")
                .post(RequestBody.create(objectMapper.writeValueAsString(payload), JSON))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                if (response.code() == 401) {
                    throw new IOException("OpenAI API request failed with status 401 (Unauthorized). Response body: " + body + ". Ensure OPENAI_API_KEY is valid and available to the running process.");
                }
                throw new IOException("OpenAI API request failed with status " + response.code() + ": " + body);
            }

            if (body.isBlank()) {
                throw new IOException("OpenAI API returned an empty response body.");
            }
            return parseResponse(body);
        }
    }

    private RubricAnalysisResult parseResponse(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        String content = extractContent(root);
        if (content.isBlank()) {
            throw new IOException("OpenAI API response did not contain any output text.");
        }
        return objectMapper.readValue(content, RubricAnalysisResult.class);
    }

    private String extractContent(JsonNode root) {
        StringBuilder builder = new StringBuilder();

        // Try extracting from "output" array
        JsonNode outputArray = root.path("output");
        if (outputArray.isArray()) {
            outputArray.forEach(outputEntry -> {
                JsonNode contentArray = outputEntry.path("content");
                if (contentArray.isArray()) {
                    contentArray.forEach(content -> {
                        String type = content.path("type").asText();
                        if ("output_text".equals(type) || "text".equals(type)) {
                            builder.append(content.path("text").asText());
                        }
                    });
                }
            });
        }

        // Try extracting from "output_text" array
        JsonNode outputText = root.path("output_text");
        if (outputText.isArray()) {
            outputText.forEach(node -> builder.append(node.asText()));
        }

        // Fallback to "response" field if nothing found
        if (builder.isEmpty()) {
            JsonNode responseNode = root.path("response");
            if (responseNode.isTextual()) {
                builder.append(responseNode.asText());
            }
        }

        return builder.toString().trim();
    }

    private ObjectNode buildPayload(String rubricText, String assignmentText) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

    // Create a clear system/context instruction and include it at the top of the input
    String systemContext = "You are a high level academic grader. Evaluate assignments using the provided rubric and respond only with JSON that matches the specified schema. For each rubric category, return: name, score (earned), maxScore (possible), evidence (short quote or pointer), and improvementSteps (array of 3-6 concise actionable steps). If a value is unknown, return 0 for numeric fields and an empty string/array for others.";
    String inputText = systemContext + "\n\nRubric:\n" + rubricText + "\n\nAssignment:\n" + assignmentText;
    root.put("input", inputText);

        // Small metadata block for observability (not secret)
        ObjectNode metadata = root.putObject("metadata");
        metadata.put("application", "eduplanner");
        metadata.put("module", "rubric-analysis");

        // Responses API: the format parameter moved under text.format
        ObjectNode textNode = root.putObject("text");
        ObjectNode format = textNode.putObject("format");
        format.put("type", "json_schema");
        format.put("name", "RubricGrade");

        // json_schema container under text.format -> move schema directly under text.format.schema per API
        ObjectNode schema = format.putObject("schema");
        schema.put("type", "object");
        // Responses API requires additionalProperties to be explicitly false for strict schema validation
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");
    properties.putObject("overallScore").put("type", TYPENUMBER);
    properties.putObject("overallMaxScore").put("type", TYPENUMBER);

    ObjectNode categories = properties.putObject("categories");
    categories.put("type", TYPEARRAY);
    ObjectNode items = categories.putObject("items");
    items.put("type", "object");
    // items should not allow unspecified additional properties
    items.put("additionalProperties", false);
    ObjectNode itemProperties = items.putObject("properties");

    itemProperties.putObject("name").put("type", TYPESTRING);
    itemProperties.putObject("score").put("type", TYPENUMBER);
    itemProperties.putObject("maxScore").put("type", TYPENUMBER);
    itemProperties.putObject("evidence").put("type", TYPESTRING);
    itemProperties.putObject("improvementSteps")
        .put("type", TYPEARRAY)
        .putObject("items").put("type", TYPESTRING);
    ArrayNode itemRequired = items.putArray("required");
    itemRequired.add("name");
    itemRequired.add("score");
    itemRequired.add("maxScore");
    itemRequired.add("evidence");
    // Responses API requires that 'required' include every key in properties when additionalProperties=false
    itemRequired.add("improvementSteps");

    ArrayNode required = schema.putArray("required");
    required.add("overallScore");
    required.add("overallMaxScore");
    required.add("categories");

        return root;
    }
}