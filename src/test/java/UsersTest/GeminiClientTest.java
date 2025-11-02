package UsersTest;



import com.example.growth_hungry.config.AiProps;
import com.example.growth_hungry.service.GeminiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GeminiClient with no real HTTP calls.
 * Mocks: AiProps, HttpClient, ObjectMapper, HttpResponse.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // keep tests quiet even if a stub is shared
class GeminiClientTest {

    @Mock
    AiProps props;
    @Mock HttpClient http;
    @Mock ObjectMapper om;
    @Mock HttpResponse<String> httpResponse;

    GeminiClient client;

    @BeforeEach
    void setUp() {
        when(props.getBaseUrl()).thenReturn("https://generativelanguage.googleapis.com/v1beta");
        when(props.getApiKey()).thenReturn("API_KEY");
        when(props.getDefaultModel()).thenReturn("gemini-1.5-flash");
        when(props.getTimeoutMs()).thenReturn(5_000);
        client = new GeminiClient(props, http, om);
    }

    @Test
    @DisplayName("Success: builds URL/body correctly and returns text from parts[0].text")
    void generate_success_basic() throws Exception {
        ObjectMapper real = new ObjectMapper();
        JsonNode okNode = real.readTree("""
            {"candidates":[{"content":{"parts":[{"text":"Hello, Aidar!"}]}}]}
        """);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String,Object>> bodyCap = ArgumentCaptor.forClass(Map.class);
        when(om.writeValueAsString(bodyCap.capture())).thenReturn("{\"dummy\":\"json\"}");

        ArgumentCaptor<HttpRequest> reqCap = ArgumentCaptor.forClass(HttpRequest.class);
        when(http.send(reqCap.capture(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{}");
        when(om.readTree(anyString())).thenReturn(okNode);

        String out = client.generate("Hi", "You are helpful.", "gemini-1.5-pro");

        assertThat(out).isEqualTo("Hello, Aidar!");

        HttpRequest sent = reqCap.getValue();
        assertThat(sent.method()).isEqualTo("POST");
        assertThat(sent.headers().firstValue("Content-Type")).contains("application/json");
        assertThat(sent.timeout()).contains(Duration.ofMillis(5_000));
        URI uri = sent.uri();
        assertThat(uri.toString())
                .isEqualTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=API_KEY");

        Map<String,Object> root = bodyCap.getValue();
        assertThat(root).containsKeys("contents");
        assertThat(root).containsKey("systemInstruction");

        @SuppressWarnings("unchecked")
        List<Map<String,Object>> contents = (List<Map<String,Object>>) root.get("contents");
        assertThat(contents).hasSize(1);
        assertThat(contents.get(0).get("role")).isEqualTo("user");
    }

    @Test
    @DisplayName("Uses default model when model arg is null or blank")
    void generate_usesDefaultModel() throws Exception {
        ObjectMapper real = new ObjectMapper();
        JsonNode okNode = real.readTree("""
            {"candidates":[{"content":{"parts":[{"text":"OK"}]}}]}
        """);

        when(om.writeValueAsString(any())).thenReturn("{}");
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{}");
        when(om.readTree(anyString())).thenReturn(okNode);

        ArgumentCaptor<HttpRequest> reqCap = ArgumentCaptor.forClass(HttpRequest.class);
        when(http.send(reqCap.capture(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        String out = client.generate("Hi", null, null);
        assertThat(out).isEqualTo("OK");
        assertThat(reqCap.getValue().uri().toString())
                .contains("/models/gemini-1.5-flash:generateContent");
    }

    @Test
    @DisplayName("If baseUrl has trailing slash, it is stripped (no double slashes)")
    void generate_stripsTrailingSlash() throws Exception {
        when(props.getBaseUrl()).thenReturn("https://generativelanguage.googleapis.com/v1beta/");

        ObjectMapper real = new ObjectMapper();
        JsonNode okNode = real.readTree("""
            {"candidates":[{"content":{"parts":[{"text":"OK"}]}}]}
        """);

        when(om.writeValueAsString(any())).thenReturn("{}");
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{}");
        when(om.readTree(anyString())).thenReturn(okNode);

        ArgumentCaptor<HttpRequest> reqCap = ArgumentCaptor.forClass(HttpRequest.class);
        when(http.send(reqCap.capture(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        String out = client.generate("Hi", null, "m");
        assertThat(out).isEqualTo("OK");
        assertThat(reqCap.getValue().uri().toString())
                .isEqualTo("https://generativelanguage.googleapis.com/v1beta/models/m:generateContent?key=API_KEY");
    }

    @Test
    @DisplayName("Non-2xx HTTP status → throws RuntimeException with status and body")
    void generate_non2xx_throws() throws Exception {
        when(om.writeValueAsString(any())).thenReturn("{}");
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(503);
        when(httpResponse.body()).thenReturn("{\"error\":\"unavailable\"}");

        assertThatThrownBy(() -> client.generate("Hi", null, "m"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Gemini HTTP 503")
                .hasMessageContaining("unavailable");
    }

    @Test
    @DisplayName("Blank message → throws IllegalArgumentException")
    void generate_blankMessage_throws() {
        assertThatThrownBy(() -> client.generate("   ", null, "m"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message must not be blank");
        verifyNoInteractions(http, om);
    }

    @Test
    @DisplayName("Parses multi-part response by concatenating parts[*].text")
    void generate_parsesMultiParts() throws Exception {
        ObjectMapper real = new ObjectMapper();
        JsonNode okNode = real.readTree("""
            {"candidates":[{"content":{"parts":[{"text":"Hello, "},{"text":"world!"}]}}]}
        """);

        when(om.writeValueAsString(any())).thenReturn("{}");
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{}");
        when(om.readTree(anyString())).thenReturn(okNode);

        String out = client.generate("Hi", null, "m");
        assertThat(out).isEqualTo("Hello, world!");
    }
}
