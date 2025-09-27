package com.example.growth_hungry.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String root() {
        return "Welcome to Growth Hungry!";
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello Growth Hungry!";
    }
}
