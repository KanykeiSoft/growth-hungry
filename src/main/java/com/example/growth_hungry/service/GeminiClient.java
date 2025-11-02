package com.example.growth_hungry.service;

import com.example.growth_hungry.config.AiProps;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final AiProps props;          // ⚙️ конфигурация (baseUrl, apiKey, model, timeout)
    private final HttpClient http;
    private final ObjectMapper om;

    public GeminiClient(AiProps props, HttpClient http, ObjectMapper om) {
        this.props = props;
        this.http = http;
        this.om = om;
    }

    @Override
    public String generate(String message, String systemPrompt, String model) throws Exception {

        // 1️⃣ Проверка входных данных
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        // 2️⃣ Определяем модель: если не указана — берём дефолтную из пропертей
        final String effectiveModel = (model == null || model.isBlank())
                ? props.getDefaultModel()
                : model;

        // 3️⃣ Формируем URL запроса
        final String base = stripTrailingSlash(props.getBaseUrl());
        final String apiKey = requireNonEmpty(props.getApiKey(), "Missing ai.api-key").trim();
        final String url = String.format(
                "%s/models/%s:generateContent",
                base, effectiveModel
        );

        // 4️⃣ Формируем JSON-тело запроса
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

        // 5️⃣ Создаём HTTP-запрос
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        log.info("Gemini URL: {}", url);
        // 6️⃣ Отправляем запрос и получаем ответ
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        // 7️⃣ Проверяем статус
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new RuntimeException("Gemini HTTP " + res.statusCode() + ": " + res.body());
        }

        // 8️⃣ Парсим JSON и достаём текст
        JsonNode node = om.readTree(res.body());
        String text = extractText(node);

        // 9️⃣ Возвращаем результат
        return (text == null || text.isBlank()) ? "" : text.trim();
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

    private String extractText(JsonNode root) {
        // Пробуем самый частый путь
        JsonNode single = root.at("/candidates/0/content/parts/0/text");
        if (single.isTextual()) {
            return single.asText();
        }

        // Если parts — массив, собираем все тексты
        JsonNode parts = root.at("/candidates/0/content/parts");
        if (parts.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode p : parts) {
                JsonNode t = p.get("text");
                if (t != null && t.isTextual()) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(t.asText());
                }
            }
            if (sb.length() > 0) return sb.toString();
        }

        // Фоллбек — если ответ в другом формате
        JsonNode direct = root.at("/candidates/0/content/text");
        if (direct.isTextual()) {
            return direct.asText();
        }

        return null;
    }
}




