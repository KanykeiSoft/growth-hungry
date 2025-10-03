package com.example.growth_hungry.controller;

import com.example.growth_hungry.dto.UserRegistrationDto;
import com.example.growth_hungry.model.User;
import com.example.growth_hungry.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")

public class UserController {
    private final UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationDto dto){
        userService.registerUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered!");
    }

}
