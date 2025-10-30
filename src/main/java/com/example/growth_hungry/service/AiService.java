package com.example.growth_hungry.service;

import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService implements ChatService {

    @Value("${ai.api.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Value("${ai.model:gemini-1.5-flash}")
    private String defaultModel;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.timeout-ms:15000}")
    private long timeoutMs;
//git commit -m " Add AiService tests and CI config"
    private final ObjectMapper om;
    private HttpClient http; // лениво создадим внутри chat()

    public AiService(ObjectMapper om) {
        this.om = om;
    }

    @Override
    public ChatResponse chat(ChatRequest req) {
        // ленивое создание клиента с таймаутом
        if (http == null) {
            http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .build();
        }

        final String model = (req.getModel() != null && !req.getModel().isBlank())
                ? req.getModel()
                : defaultModel;

        final String base = stripTrailingSlash(baseUrl);
        final String url = String.format("%s/models/%s:generateContent?key=%s", base, model, apiKey);

        try {
            // ---- формируем тело запроса к Gemini (без temperature) ----
            Map<String, Object> userContent = new LinkedHashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", List.of(Map.of("text", safe(req.getMessage()))));

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("contents", List.of(userContent));

            if (req.getSystemPrompt() != null && !req.getSystemPrompt().isBlank()) {
                root.put("systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", req.getSystemPrompt()))
                ));
            }

            String body = om.writeValueAsString(root);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response =
                    http.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode node = om.readTree(response.body());
                String reply = extractText(node);
                if (reply == null || reply.isBlank()) reply = "(пустой ответ модели)";
                return new ChatResponse(reply, req.getContextId(), model);
            } else {
                String msg = "Gemini HTTP " + response.statusCode();
                return new ChatResponse("Ошибка вызова Gemini: " + msg, req.getContextId(), model);
            }

        } catch (HttpTimeoutException te) {
            return new ChatResponse("Ошибка вызова Gemini: таймаут запроса", req.getContextId(), model);
        } catch (Exception e) {
            return new ChatResponse("Ошибка вызова Gemini: " + e.getMessage(), req.getContextId(), model);
        }
    }

    // ---------- helpers ----------

    private static String stripTrailingSlash(String s) {
        if (s == null) return null;
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    /**
     * Пытаемся вытащить текст из типового ответа Gemini.
     * Основной путь: candidates[0].content.parts[0].text
     * Плюс fallback-варианты.
     */
    private String extractText(JsonNode root) {
        // основной путь
        JsonNode text = root.at("/candidates/0/content/parts/0/text");
        if (text.isTextual()) return text.asText();

        // пройтись по parts[], вдруг текст в другом элементе
        JsonNode parts = root.at("/candidates/0/content/parts");
        if (parts.isArray()) {
            for (JsonNode p : parts) {
                if (p.has("text") && p.get("text").isTextual()) {
                    return p.get("text").asText();
                }
            }
        }

        // иногда SDK кладёт сразу content.text
        JsonNode direct = root.at("/candidates/0/content/text");
        if (direct.isTextual()) return direct.asText();

        // ничего не нашли — вернём дамп для дебага
        return "[No text in AI response]\n" + root.toPrettyString();
    }
}


