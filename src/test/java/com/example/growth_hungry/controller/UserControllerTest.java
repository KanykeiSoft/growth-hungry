package com.example.growth_hungry.controller;


import com.example.growth_hungry.dto.UserRegistrationDto;
import com.example.growth_hungry.model.User;
import com.example.growth_hungry.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------- /users/register ----------

    @Test
    @DisplayName("POST /users/register — успешная регистрация -> 201")
    void register_success() throws Exception {
        Mockito.when(userService.registerUser(any(UserRegistrationDto.class)))
                .thenReturn(new User());

        var dto = new UserRegistrationDto();
        dto.setUsername("aidar");
        dto.setPassword("StrongPass123!");

        mockMvc.perform(
                        post("/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(content().string("User registered!"));

        Mockito.verify(userService).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    @DisplayName("POST /users/register — невалидный ввод -> 400")
    void register_invalidInput() throws Exception {
        var dto = new UserRegistrationDto();
        dto.setUsername(""); // нарушает @NotBlank
        dto.setPassword("pass");

        mockMvc.perform(
                        post("/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isBadRequest());
    }

    // ---------- /users/login ----------
    // Эти тесты будут работать, если ты добавишь метод /users/login в UserController

    @Test
    @DisplayName("POST /users/login — успешный логин -> 200")
    void login_success() throws Exception {
        var user = new User();
        user.setUsername("aidar");
        user.setPassword("$2a$10$hash");

        Mockito.when(userService.findByUsername(eq("aidar")))
                .thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches(eq("StrongPass123!"), eq(user.getPassword())))
                .thenReturn(true);

        var dto = new UserRegistrationDto();
        dto.setUsername("aidar");
        dto.setPassword("StrongPass123!");

        mockMvc.perform(
                        post("/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Login successful"));

        Mockito.verify(userService).findByUsername("aidar");
        Mockito.verify(passwordEncoder).matches("StrongPass123!", user.getPassword());
    }

    @Test
    @DisplayName("POST /users/login — неверный пароль -> 401")
    void login_wrongPassword() throws Exception {
        var user = new User();
        user.setUsername("aidar");
        user.setPassword("$2a$10$hash");

        Mockito.when(userService.findByUsername(eq("aidar")))
                .thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches(eq("BadPass"), eq(user.getPassword())))
                .thenReturn(false);

        var dto = new UserRegistrationDto();
        dto.setUsername("aidar");
        dto.setPassword("BadPass");

        mockMvc.perform(
                        post("/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));
    }

    @Test
    @DisplayName("POST /users/login — пользователь не найден -> 404")
    void login_userNotFound() throws Exception {
        Mockito.when(userService.findByUsername(eq("ghost")))
                .thenReturn(Optional.empty());

        var dto = new UserRegistrationDto();
        dto.setUsername("ghost");
        dto.setPassword("whatever");

        mockMvc.perform(
                        post("/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }
}
