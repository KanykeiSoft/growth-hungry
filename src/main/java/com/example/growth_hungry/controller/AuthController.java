package com.example.growth_hungry.controller;


import com.example.growth_hungry.dto.LoginDto;
import com.example.growth_hungry.dto.UserRegistrationDto;
import com.example.growth_hungry.security.JwtUtil;
import com.example.growth_hungry.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth") // ← базовый путь для auth
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDto dto) {
        userService.registerUser(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto dto) {
        String accessToken = userService.login(dto.getEmail(), dto.getPassword());
        return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }
}
