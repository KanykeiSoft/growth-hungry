package com.example.growth_hungry.controller;

import com.example.growth_hungry.config.WebSecurityConfig;
import com.example.growth_hungry.dto.ChatMessageDto;
import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import com.example.growth_hungry.dto.ChatSessionDto;
import com.example.growth_hungry.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ChatController.class,
        excludeFilters = {
                // 1) исключаем реальную секьюрити-конфигурацию
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebSecurityConfig.class),
                // 2) исключаем весь пакет security (JwtAuthFilter, JwtUtil, Json401EntryPoint и т.д.)
                @ComponentScan.Filter(type = FilterType.REGEX,
                        pattern = "com\\.example\\.growth_hungry\\.security\\..*")
        }
)
@AutoConfigureMockMvc(addFilters = true)   // прогоняем наш ТЕСТОВЫЙ фильтр из TestSecurityConfig
@Import({ TestSecurityConfig.class, ChatControllerTest.MockBeans.class })
@TestPropertySource(properties = { "spring.test.context.failure.threshold=99" })
class ChatControllerTest {

    // даём Spring'у мок ChatService как бин
    @TestConfiguration
    static class MockBeans {
        @Bean
        ChatService chatService() {
            return Mockito.mock(ChatService.class);
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ChatService chatService;

    // ===================== POST /api/chat =====================

    @Test
    @DisplayName("POST /api/chat без токена → 401 и ChatService.chat не вызывается")
    void chat_Unauthorized_WhenNoToken_Then401() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setMessage("hello");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // важно: проверяем, что метод chat() ни разу не вызывался
        verify(chatService, never()).chat(any(ChatRequest.class));
    }

    @Test
    @DisplayName("POST /api/chat с токеном и валидным сообщением → 200 и вызывает ChatService.chat()")
    void chat_AuthorizedAndValidInput_Then200() throws Exception {
        ChatResponse mocked = new ChatResponse();
        mocked.setReply("Hi from mocked service");

        when(chatService.chat(any(ChatRequest.class))).thenReturn(mocked);

        ChatRequest req = new ChatRequest();
        req.setMessage("hello from test");

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer MOCK_TOKEN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hi from mocked service"));

        verify(chatService, times(1)).chat(any(ChatRequest.class));
    }

    @Test
    @DisplayName("POST /api/chat с токеном, но пустым сообщением → 400 и ChatService.chat не вызывается")
    void chat_AuthorizedButInvalidInput_Then400() throws Exception {
        ChatRequest bad = new ChatRequest();
        bad.setMessage(""); // @NotBlank -> невалидно

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer MOCK_TOKEN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).chat(any(ChatRequest.class));
    }

    // ===================== GET /api/chat/sessions =====================

    @Test
    @DisplayName("GET /api/chat/sessions → возвращает список сессий пользователя")
    void getUserSessions_returnsList() throws Exception {
        ChatSessionDto s1 = new ChatSessionDto();
        s1.setId(1L);
        s1.setTitle("First session");
        s1.setUpdatedAt(Instant.parse("2025-01-01T10:00:00Z"));

        ChatSessionDto s2 = new ChatSessionDto();
        s2.setId(2L);
        s2.setTitle("Second session");
        s2.setUpdatedAt(Instant.parse("2025-01-02T11:00:00Z"));

        List<ChatSessionDto> sessions = List.of(s1, s2);
        given(chatService.getUserSessions()).willReturn(sessions);

        mockMvc.perform(get("/api/chat/sessions")
                        .header("Authorization", "Bearer MOCK_TOKEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("First session"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].title").value("Second session"));
    }

    // ===================== GET /api/chat/sessions/{id} =====================

    @Test
    @DisplayName("GET /api/chat/sessions/{id} → возвращает сообщения сессии")
    void getSessionMessages_returnsList() throws Exception {
        ChatMessageDto m1 = new ChatMessageDto();
        m1.setId(10L);
        m1.setRole("USER");
        m1.setContent("Hello");
        m1.setCreatedAt(Instant.parse("2025-01-01T10:00:00Z"));

        ChatMessageDto m2 = new ChatMessageDto();
        m2.setId(11L);
        m2.setRole("ASSISTANT");
        m2.setContent("Hi, how can I help?");
        m2.setCreatedAt(Instant.parse("2025-01-01T10:00:01Z"));

        List<ChatMessageDto> messages = List.of(m1, m2);
        Long sessionId = 1L;

        given(chatService.getSessionMessages(sessionId)).willReturn(messages);

        mockMvc.perform(get("/api/chat/sessions/{sessionId}", sessionId)
                        .header("Authorization", "Bearer MOCK_TOKEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].content").value("Hello"))
                .andExpect(jsonPath("$[1].id").value(11L))
                .andExpect(jsonPath("$[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$[1].content").value("Hi, how can I help?"));
    }
}