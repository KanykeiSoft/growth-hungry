package UsersTest;

import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import com.example.growth_hungry.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Connects Mockito with JUnit 5
class AiServiceTest {

    // --- Mocks (test doubles) ---
    @Mock HttpClient http;               // The main mock: intercepts HTTP calls
    @Mock HttpResponse<String> httpResp; // Fake response from the "server"

    // Real ObjectMapper, but wrapped as a spy (we can verify its calls)
    ObjectMapper om;

    // The service we are testing
    AiService service;

    @BeforeEach
    void setUp() throws Exception {
        // Spy = a real object, but we can monitor its method calls
        om = spy(new ObjectMapper());
        service = new AiService(om);

        // In tests, @Value fields are not injected by Spring, so we set them manually
        setPrivate(service, "baseUrl", "https://generativelanguage.googleapis.com/v1beta");
        setPrivate(service, "defaultModel", "gemini-1.5-flash");
        setPrivate(service, "apiKey", "TEST_KEY");
        setPrivate(service, "timeoutMs", 15000L);

        // Inject the mock HttpClient into the private field (otherwise a real one will be created)
        setPrivate(service, "http", http);
    }

    // Helper to set private fields using reflection
    static void setPrivate(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    @DisplayName("200 OK → parses response text, URL and headers are correct")
    void chat_success200() throws Exception {
        // Prepare fake JSON returned from the external API
        String upstream = """
          { "candidates":[{ "content":{"parts":[{"text":"Hello Aidar!"}]}}] }
        """;
        when(httpResp.statusCode()).thenReturn(200);
        when(httpResp.body()).thenReturn(upstream);

        // Capture the HttpRequest sent to check URL and headers
        ArgumentCaptor<HttpRequest> reqCap = ArgumentCaptor.forClass(HttpRequest.class);
        when(http.<String>send(reqCap.capture(), any())).thenReturn(httpResp);

        // Call the service
        ChatRequest req = new ChatRequest();
        req.setMessage("Hi!");
        ChatResponse out = service.chat(req);

        // Check result
        assertEquals("Hello Aidar!", out.getReply());
        assertEquals("gemini-1.5-flash", out.getModel());

        // Check the built HTTP request
        HttpRequest sent = reqCap.getValue();
        assertEquals("POST", sent.method());
        assertEquals("application/json", sent.headers().firstValue("Content-Type").orElse(""));
        URI uri = sent.uri();
        assertTrue(uri.toString().startsWith(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"));
        assertTrue(uri.getQuery().contains("key=TEST_KEY"));

        // Verify systemPrompt was NOT added to the body
        ArgumentCaptor<Object> bodyArg = ArgumentCaptor.forClass(Object.class);
        verify(om, atLeastOnce()).writeValueAsString(bodyArg.capture());
        @SuppressWarnings("unchecked")
        var rootMap = (java.util.Map<String, Object>) bodyArg.getValue();
        assertFalse(rootMap.containsKey("systemInstruction"));
    }

    @Test
    @DisplayName("req.model overrides defaultModel and appears in the URL")
    void chat_customModel() throws Exception {
        when(httpResp.statusCode()).thenReturn(200);
        when(httpResp.body()).thenReturn("""
          { "candidates":[{ "content":{"parts":[{"text":"ok"}]}}] }
        """);
        ArgumentCaptor<HttpRequest> reqCap = ArgumentCaptor.forClass(HttpRequest.class);
        when(http.<String>send(reqCap.capture(), any())).thenReturn(httpResp);

        ChatRequest req = new ChatRequest();
        req.setMessage("m");
        req.setModel("gemini-2.0-flash");

        ChatResponse out = service.chat(req);
        assertEquals("gemini-2.0-flash", out.getModel());
        assertTrue(reqCap.getValue().uri().toString()
                .contains("/models/gemini-2.0-flash:generateContent"));
    }

    @Test
    @DisplayName("systemPrompt is added to the request body")
    void chat_systemPrompt_present() throws Exception {
        when(httpResp.statusCode()).thenReturn(200);
        when(httpResp.body()).thenReturn("""
          { "candidates":[{ "content":{"parts":[{"text":"x"}]}}] }
        """);
        when(http.<String>send(any(), any())).thenReturn(httpResp);

        ChatRequest req = new ChatRequest();
        req.setMessage("M");
        req.setSystemPrompt("You are helpful");

        ChatResponse out = service.chat(req);
        assertEquals("x", out.getReply());

        // Verify systemPrompt was serialized into the JSON body
        ArgumentCaptor<Object> bodyArg = ArgumentCaptor.forClass(Object.class);
        verify(om, atLeastOnce()).writeValueAsString(bodyArg.capture());
        @SuppressWarnings("unchecked")
        var rootMap = (java.util.Map<String, Object>) bodyArg.getValue();
        assertTrue(rootMap.containsKey("systemInstruction"));
    }

    @Test
    @DisplayName("non-2xx → returns ChatResponse with an error message")
    void chat_non2xx() throws Exception {
        when(httpResp.statusCode()).thenReturn(400);
        when(http.<String>send(any(), any())).thenReturn(httpResp);

        ChatResponse out = service.chat(new ChatRequest("hi"));
        assertTrue(out.getReply().contains("Ошибка вызова Gemini:"));
        assertTrue(out.getReply().contains("Gemini HTTP 400"));
    }

    @Test
    @DisplayName("timeout → returns special timeout message")
    void chat_timeout() throws Exception {
        when(http.send(any(), any())).thenThrow(new java.net.http.HttpTimeoutException("timeout"));

        ChatResponse out = service.chat(new ChatRequest("hi"));
        assertTrue(out.getReply().contains("таймаут запроса"));
    }

    @Test
    @DisplayName("IOException → returns ChatResponse with exception message")
    void chat_io_error() throws Exception {
        when(http.send(any(), any())).thenThrow(new java.io.IOException("boom"));

        ChatResponse out = service.chat(new ChatRequest("hi"));
        assertTrue(out.getReply().contains("Ошибка вызова Gemini:"));
        assertTrue(out.getReply().contains("boom"));
    }

    @Test
    @DisplayName("Empty model reply → '(empty model response)' placeholder")
    void chat_empty_text_placeholder() throws Exception {
        when(httpResp.statusCode()).thenReturn(200);
        when(httpResp.body()).thenReturn("""
          { "candidates":[{ "content":{"parts":[{"text":""}]}}] }
        """);
        when(http.<String>send(any(), any())).thenReturn(httpResp);

        ChatResponse out = service.chat(new ChatRequest("hi"));
        assertEquals("(пустой ответ модели)", out.getReply());
    }
}


