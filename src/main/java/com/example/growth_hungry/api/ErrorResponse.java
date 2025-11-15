package com.example.growth_hungry.api;

import java.util.List;
import java.util.Map;


public record ErrorResponse(
        String error,
        String message,
        List<Map<String, String>> details
) {
    public static ErrorResponse of(String code, String msg) {
        return new ErrorResponse(code, msg, List.of());
    }
}


