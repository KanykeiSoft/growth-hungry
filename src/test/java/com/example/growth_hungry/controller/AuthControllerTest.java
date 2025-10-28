package com.example.growth_hungry.controller;


import com.example.growth_hungry.dto.LoginDto;
import com.example.growth_hungry.dto.UserRegistrationDto;
import com.example.growth_hungry.api.UsernameAlreadyExistsException;
import com.example.growth_hungry.model.User;
import com.example.growth_hungry.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import com.example.growth_hungry.security.JwtUtil;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @Resource
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService; // контроллер зовёт сервис — его и мокаем

    // ---------- /api/auth/register ----------

    @Test
    @DisplayName("POST /api/auth/register — valid -> 201 CREATED")
    void register_success() throws Exception {
        Mockito.when(userService.registerUser(any(UserRegistrationDto.class)))
                .thenReturn(new User());

        var dto = new UserRegistrationDto();
        dto.setUsername("aidar");
        dto.setPassword("StrongPass123!");
        dto.setEmail("a@ex.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User registered"));
    }

    @Test
    @DisplayName("POST /api/auth/register — empty username -> 400 BAD_REQUEST")
    void register_emptyUsername_returns400() throws Exception {
        var dto = new UserRegistrationDto();
        dto.setUsername("");                // пусто
        dto.setPassword("StrongPass123!");
        dto.setEmail("a@ex.com");


        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("POST /api/auth/register — empty password -> 400 BAD_REQUEST")
    void register_emptyPassword_returns400() throws Exception {
        var dto = new UserRegistrationDto();
        dto.setUsername("aidar");
        dto.setPassword("");                // пусто
        dto.setEmail("a@ex.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/auth/register — invalid email -> 400 BAD_REQUEST")
    void register_invalidEmail_returns400() throws Exception {
        var dto = new UserRegistrationDto();
        dto.setUsername("aidar");
        dto.setPassword("StrongPass123!");
        dto.setEmail("not-an-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/auth/register — whitespace fields -> 400 BAD_REQUEST")
    void register_whitespace_returns400() throws Exception {
        var dto = new UserRegistrationDto();
        dto.setUsername("   ");
        dto.setPassword("   ");
        dto.setEmail("a@ex.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("POST /api/auth/register — missing fields (empty JSON) -> 400 BAD_REQUEST")
    void register_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/auth/register — username taken -> 409 CONFLICT")
    void register_usernameTaken_returns409() throws Exception {
        Mockito.when(userService.registerUser(any(UserRegistrationDto.class)))
                .thenThrow(new UsernameAlreadyExistsException("Username already exists: aidar"));

        var dto = new UserRegistrationDto();
        dto.setUsername("aidar");
        dto.setPassword("StrongPass123!");
        dto.setEmail("a@ex.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // ---------- /api/auth/login ----------

    @Test
    @DisplayName("POST /api/auth/login — valid -> 200 OK")
    void login_success() throws Exception {

        var dto = new LoginDto();
        dto.setUsername("aidan");
        dto.setPassword("StrongPass123!");

        // login возвращает НЕ void → мокаем через thenReturn(...)
        Mockito.when(userService.login(eq("aidan"), eq("StrongPass123!")))
                .thenReturn("ok");            // если у тебя String — поставь "OK", если User — new User()

        Mockito.when(jwtUtil.generate(eq("aidan")))
                .thenReturn("dummy-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("dummy-token"));
    }

    @Test
    @DisplayName("POST /api/auth/login — bad credentials -> 401 UNAUTHORIZED")
    void login_badCredentials_returns401() throws Exception {
        Mockito.when(userService.login(eq("aidar"), eq("BadPass")))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        var dto = new LoginDto();
        dto.setUsername("aidar");
        dto.setPassword("BadPass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
        // если у тебя есть хендлер под BadCredentialsException,
        // можно дополнительно проверить тело ответа.
    }
}
