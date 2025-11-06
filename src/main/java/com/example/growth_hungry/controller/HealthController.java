package com.example.growth_hungry.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @GetMapping("/health/db")
    String db() {
        return "DB OK: " + jdbc.queryForObject("SELECT 1", Integer.class);
    }

}
