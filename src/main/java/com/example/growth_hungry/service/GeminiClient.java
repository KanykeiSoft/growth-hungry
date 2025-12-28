package com.example.growth_hungry.service;

import com.example.growth_hungry.config.AiProps;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Низкоуровневый клиент для обращения к Gemini API.
 * Здесь выполняется HTTP-вызов, формируется JSON и парсится ответ.
 * Вся бизнес-логика и обработка ошибок — выше, в ChatServiceImpl.
 */

@Service
public class GeminiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    // Версию API держим В ОДНОМ МЕСТЕ (в коде), а base-url в пропертис без /v1...
    private static final String API_VERSION_PATH = "/v1beta";

    private final AiProps props;
    private final HttpClient http;
    private final ObjectMapper om;

    public GeminiClient(AiProps props, HttpClient http, ObjectMapper om) {
        this.props = props;
        this.http = http;
        this.om = om;
    }

    @Override
    public String generate(String message, String systemPrompt, String model) {
        String msg = normalizeRequired(message, "message must not be blank");

        String effectiveModel = normalize(model);
        if (effectiveModel == null) effectiveModel = normalizeRequired(props.getDefaultModel(), "Missing ai.default-model");

        String url = buildUrl(effectiveModel);

        String json = buildRequestJson(msg, systemPrompt);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        log.info("Gemini URL: {}", url);

        HttpResponse<String> res;
        try {
            res = http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Gemini call interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Gemini call failed", e);
        }

        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            // Сюда попадают 400/401/403/404 и т.д.
            throw new RuntimeException("Gemini HTTP " + res.statusCode() + ": " + safeBody(res.body()));
        }

        JsonNode root;
        try {
            root = om.readTree(res.body());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response JSON", e);
        }

        String text = extractText(root);
        return text == null ? "" : text.trim();
    }

    private String buildUrl(String model) {
        String base = stripTrailingSlash(normalizeRequired(props.getBaseUrl(), "Missing ai.base-url"));

        // Важно: base-url должен быть БЕЗ /v1beta в properties.
        // Мы всегда добавляем версию тут: ... + /v1beta
        String encodedKey = URLEncoder.encode(
                normalizeRequired(props.getApiKey(), "Missing ai.api-key"),
                StandardCharsets.UTF_8
        );

        String cleanModel = model.startsWith("models/") ? model.substring("models/".length()) : model;

        return base + API_VERSION_PATH
                + "/models/" + cleanModel
                + ":generateContent?key=" + encodedKey;
    }

    private String buildRequestJson(String message, String systemPrompt) {
        Map<String, Object> user = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", message))
        );

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("contents", List.of(user));

        String sp = normalize(systemPrompt);
        if (sp != null) {
            root.put("systemInstruction",
                    Map.of("parts", List.of(Map.of("text", sp))));
        }

        try {
            return om.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Gemini request", e);
        }
    }

    private String extractText(JsonNode root) {
        JsonNode parts = root.at("/candidates/0/content/parts");
        if (parts.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode p : parts) {
                JsonNode t = p.get("text");
                if (t != null && t.isTextual()) sb.append(t.asText());
            }
            if (sb.length() > 0) return sb.toString();
        }

        JsonNode direct = root.at("/candidates/0/content/text");
        return direct.isTextual() ? direct.asText() : null;
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }

    private static String normalizeRequired(String s, String error) {
        String t = normalize(s);
        if (t == null) throw new IllegalArgumentException(error);
        return t;
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String safeBody(String body) {
        if (body == null) return "";
        // чтобы не засорять логи огромными JSON-ами
        return body.length() > 3000 ? body.substring(0, 3000) + "…" : body;
    }
}
