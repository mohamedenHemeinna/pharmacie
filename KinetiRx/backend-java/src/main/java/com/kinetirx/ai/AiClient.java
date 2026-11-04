package com.kinetirx.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinetirx.config.KinetiRxProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Calls the configured AI backend. Two paths, as in Go: an OpenAI-compatible chat endpoint
 * (operator config) and Google's native Gemini REST API (legacy GEMINI_API_KEY).
 * Error handling reproduces the Go messages, including the array-wrapped error shape some providers send.
 */
@Component
public class AiClient {

    static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent";
    private static final Pattern DATA_URL_PREFIX = Pattern.compile("^data:[a-zA-Z0-9/+-]+;base64,");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper;
    private final String geminiKey;

    public AiClient(ObjectMapper mapper, KinetiRxProperties props) {
        this.mapper = mapper;
        this.geminiKey = props.geminiApiKey();
    }

    /** Result of a prompt; empty when no AI backend is configured (callers return a fallback). */
    public record Config(String baseUrl, String model, String apiKey) {}

    public String chatCompletion(Config cfg, String text, String imageBase64, String mime, boolean jsonMode,
                                 Duration timeout) {
        var parts = new java.util.ArrayList<Map<String, Object>>();
        parts.add(Map.of("type", "text", "text", text));
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            String dataUrl = imageBase64.startsWith("data:") ? imageBase64
                    : "data:" + (mime == null || mime.isEmpty() ? "image/jpeg" : mime) + ";base64," + imageBase64;
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)));
        }
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", cfg.model());
        body.put("messages", List.of(Map.of("role", "user", "content", parts)));
        body.put("temperature", 0.1);
        if (jsonMode) body.put("response_format", Map.of("type", "json_object"));

        String url = trimTrailingSlash(cfg.baseUrl()) + "/chat/completions";
        HttpResponse<String> resp = post(url, body, Map.of("Authorization", "Bearer " + cfg.apiKey()), timeout);

        JsonNode parsed;
        try {
            parsed = mapper.readTree(resp.body());
        } catch (Exception e) {
            String snippet = resp.body().length() > 200 ? resp.body().substring(0, 200) + "…" : resp.body();
            throw new AiException("endpoint did not return JSON (got: \"" + snippet + "\") — double-check the Base URL is the actual API endpoint, not a documentation or website link");
        }
        // Some providers wrap an error in a JSON array rather than a bare object.
        if (parsed != null && parsed.isArray() && parsed.size() > 0) parsed = parsed.get(0);
        if (resp.statusCode() != 200) {
            JsonNode msg = parsed == null ? null : parsed.path("error").path("message");
            if (msg != null && msg.isTextual()) {
                throw new AiException("AI endpoint error (" + resp.statusCode() + "): " + msg.asText());
            }
            throw new AiException("AI endpoint error: status " + resp.statusCode());
        }
        JsonNode choices = parsed == null ? null : parsed.path("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new AiException("AI endpoint returned no choices");
        }
        return choices.get(0).path("message").path("content").asText("");
    }

    public String gemini(String promptText, String imageBase64, String mime, boolean jsonMode) {
        var parts = new java.util.ArrayList<Map<String, Object>>();
        parts.add(Map.of("text", promptText));
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            parts.add(Map.of("inline_data", Map.of(
                    "mime_type", mime == null || mime.isEmpty() ? "image/jpeg" : mime,
                    "data", DATA_URL_PREFIX.matcher(imageBase64).replaceFirst(""))));
        }
        Map<String, Object> gen = new java.util.LinkedHashMap<>();
        gen.put("temperature", 0.1);
        if (jsonMode) gen.put("responseMimeType", "application/json");
        Map<String, Object> body = Map.of("contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", gen);

        HttpResponse<String> resp = post(GEMINI_ENDPOINT + "?key=" + geminiKey, body, Map.of(), Duration.ofSeconds(60));
        JsonNode parsed;
        try {
            parsed = mapper.readTree(resp.body());
        } catch (Exception e) {
            throw new AiException("parse gemini response: " + e.getMessage());
        }
        if (resp.statusCode() != 200) {
            JsonNode msg = parsed.path("error").path("message");
            if (msg.isTextual()) throw new AiException("gemini API error (" + resp.statusCode() + "): " + msg.asText());
            throw new AiException("gemini API error: status " + resp.statusCode());
        }
        JsonNode candidates = parsed.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty() || candidates.get(0).path("content").path("parts").isEmpty()) {
            throw new AiException("gemini returned no candidates");
        }
        StringBuilder out = new StringBuilder();
        for (JsonNode p : candidates.get(0).path("content").path("parts")) out.append(p.path("text").asText(""));
        return out.toString();
    }

    private HttpResponse<String> post(String url, Object body, Map<String, String> headers, Duration timeout) {
        try {
            var builder = HttpRequest.newBuilder(URI.create(url)).timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body)));
            headers.forEach(builder::header);
            return http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new AiException("call AI endpoint: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("call AI endpoint interrupted");
        }
    }

    private static String trimTrailingSlash(String s) {
        return s.replaceAll("/+$", "");
    }

    /** Error raised by the AI client; messages are safe to show to the operator. */
    public static class AiException extends RuntimeException {
        public AiException(String message) {
            super(message);
        }
    }
}
