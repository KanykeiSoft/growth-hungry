package com.example.growth_hungry.controller;

import com.example.growth_hungry.config.WebSecurityConfig;
import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import com.example.growth_hungry.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ChatController.class,
        excludeFilters = {
                // 1) исключаем реальную секьюрити-конфигурацию
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebSecurityConfig.class),
                // 2) исключаем ВЕСЬ пакет security (JwtAuthFilter, JwtUtil, Json401EntryPoint и т.д.)
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.example\\.growth_hungry\\.security\\..*")
        }
)
@AutoConfigureMockMvc(addFilters = true)                  // прогоняем наш тестовый фильтр
@Import({ TestSecurityConfig.class, ChatControllerTest.MockBeans.class })
@TestPropertySource(properties = { "spring.test.context.failure.threshold=99" })
class ChatControllerTest {

    @TestConfiguration
    static class MockBeans {
        @Bean ChatService chatService() { return Mockito.mock(ChatService.class); }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ChatService chatService;

    @Test
    void chat_Unauthorized_WhenNoToken_Then401() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setMessage("hello");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(chatService);
    }

    @Test
    void chat_AuthorizedAndValidInput_Then200() throws Exception {
        ChatResponse mocked = Mockito.mock(ChatResponse.class);
        when(chatService.chat(any(ChatRequest.class))).thenReturn(mocked);

        ChatRequest req = new ChatRequest();
        req.setMessage("hello from test");

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer MOCK_TOKEN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(chatService, times(1)).chat(any(ChatRequest.class));
    }

    @Test
    void chat_AuthorizedButInvalidInput_Then400() throws Exception {
        ChatRequest bad = new ChatRequest();
        bad.setMessage(""); // @NotBlank -> invalid

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer MOCK_TOKEN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatService);
    }
}
