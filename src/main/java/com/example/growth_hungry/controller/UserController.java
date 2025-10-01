package com.example.growth_hungry.controller;

import com.example.growth_hungry.model.User;
import com.example.growth_hungry.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")

public class UserController {
    private final UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }

}
