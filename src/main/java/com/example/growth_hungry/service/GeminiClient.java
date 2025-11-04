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

    private final AiProps props;          // baseUrl, apiKey, defaultModel, timeout
    private final HttpClient http;
    private final ObjectMapper om;

    public GeminiClient(AiProps props, HttpClient http, ObjectMapper om) {
        this.props = props;
        this.http = http;
        this.om = om;
    }

    @Override
    public String generate(String message, String systemPrompt, String model) throws Exception {
        // 1) Валидация
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        // 2) Модель
        final String effectiveModel = (model == null || model.isBlank())
                ? props.getDefaultModel()
                : model;

        // 3) URL (с ?key=...)
        final String url = buildUrl(effectiveModel);

        // 4) Тело запроса
        Map<String, Object> user = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", message))
        );

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("contents", List.of(user));

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            root.put("systemInstruction",
                    Map.of("parts", List.of(Map.of("text", systemPrompt))));
        }

        final String json = om.writeValueAsString(root);

        // 5) HTTP-запрос (ключ в query, заголовок с ключом НЕ нужен)
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        log.info("Gemini URL: {}", url);

        // 6) Отправка
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        // 7) Статус
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new RuntimeException("Gemini HTTP " + res.statusCode() + ": " + res.body());
        }

        // 8) Парсинг
        JsonNode node = om.readTree(res.body());
        String text = extractText(node);

        // 9) Результат
        return (text == null || text.isBlank()) ? "" : text.trim();
    }

    /** Строит URL и гарантирует /v1beta и ?key=... */
    private String buildUrl(String model) {
        String base = stripTrailingSlash(requireNonEmpty(props.getBaseUrl(), "Missing ai.base-url"));

        // если /v1beta ещё не добавлен — добавляем
        if (!base.endsWith("/v1beta")) {
            base = base + "/v1beta";
        }

        String encodedKey = URLEncoder.encode(
                requireNonEmpty(props.getApiKey(), "Missing ai.api-key").trim(),
                StandardCharsets.UTF_8
        );

        // 💡 удаляем возможный префикс "models/" у модели
        String cleanModel = (model != null && model.startsWith("models/"))
                ? model.substring("models/".length())
                : model;

        return base + "/models/" + cleanModel + ":generateContent?key=" + encodedKey;
    }

    // ---------- helpers ----------

    private static String stripTrailingSlash(String s) {
        if (s == null) return null;
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String requireNonEmpty(String s, String msg) {
        if (s == null || s.isBlank()) throw new IllegalStateException(msg);
        return s;
    }

    /** Склеивает ВСЕ parts[].text без лишних разделителей. */
    private String extractText(JsonNode root) {
        // Частый случай: один текст
        JsonNode single = root.at("/candidates/0/content/parts/0/text");
        if (single.isTextual()) {
            // но это покроется и общим случаем; оставим для быстрого пути
        }

        // Общий случай: parts[] с несколькими кусками
        JsonNode parts = root.at("/candidates/0/content/parts");
        if (parts.isArray() && parts.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode p : parts) {
                JsonNode t = p.get("text");
                if (t != null && t.isTextual()) {
                    sb.append(t.asText()); // без \n, чтобы совпасть с "Hello, world!"
                }
            }
            if (sb.length() > 0) return sb.toString();
        }

        // Фоллбек
        JsonNode direct = root.at("/candidates/0/content/text");
        if (direct.isTextual()) return direct.asText();

        return null;
    }
}
