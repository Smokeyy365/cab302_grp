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
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI API request failed with status " + response.code() + ": " + response.message());
            }
            String body = response.body() != null ? response.body().string() : "";
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

        JsonNode outputArray = root.path("output");
        if (outputArray.isArray()) {
            for (JsonNode outputEntry : outputArray) {
                JsonNode contentArray = outputEntry.path("content");
                if (contentArray.isArray()) {
                    for (JsonNode content : contentArray) {
                        String type = content.path("type").asText();
                        if ("output_text".equals(type) || "text".equals(type)) {
                            builder.append(content.path("text").asText());
                        }
                    }
                }
            }
        }

        JsonNode outputText = root.path("output_text");
        if (outputText.isArray()) {
            for (JsonNode node : outputText) {
                builder.append(node.asText());
            }
        }

        JsonNode responseNode = root.path("response");
        if (builder.length() == 0 && responseNode.isTextual()) {
            builder.append(responseNode.asText());
        }

        return builder.toString().trim();
    }

    private ObjectNode buildPayload(String rubricText, String assignmentText) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

        ArrayNode input = root.putArray("input");
        input.add(createMessage("system", "You are an academic grader. Evaluate assignments using the provided rubric and respond with JSON."));

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        ArrayNode userContent = userMessage.putArray("content");
        userContent.add(createContent("Rubric:\n" + rubricText));
        userContent.add(createContent("Assignment:\n" + assignmentText));
        input.add(userMessage);

        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_schema");
        ObjectNode schemaContainer = responseFormat.putObject("json_schema");
        schemaContainer.put("name", "RubricGrade");
        ObjectNode schema = schemaContainer.putObject("schema");
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("overallGpa").put("type", "number");
        ObjectNode categories = properties.putObject("categories");
        categories.put("type", "array");
        ObjectNode items = categories.putObject("items");
        items.put("type", "object");
        ObjectNode itemProperties = items.putObject("properties");
        itemProperties.putObject("name").put("type", "string");
        itemProperties.putObject("gpa").put("type", "number");
        itemProperties.putObject("evidence").put("type", "string");
        ArrayNode itemRequired = items.putArray("required");
        itemRequired.add("name");
        itemRequired.add("gpa");
        itemRequired.add("evidence");
        ArrayNode required = schema.putArray("required");
        required.add("overallGpa");
        required.add("categories");

        return root;
    }

    private ObjectNode createMessage(String role, String text) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        ArrayNode content = message.putArray("content");
        content.add(createContent(text));
        return message;
    }

    private ObjectNode createContent(String text) {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("type", "text");
        content.put("text", text);
        return content;
    }
}