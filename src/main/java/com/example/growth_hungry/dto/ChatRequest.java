package com.example.growth_hungry.dto;
import jakarta.validation.constraints.*;


public class ChatRequest {

    @NotBlank
    private String message;

    private Long chatSessionId;
    private String systemPrompt;
    private String model;

    public ChatRequest() {
    }

    // ===== getters =====
    public String getMessage() {
        return message;
    }

    public Long getChatSessionId() {
        return chatSessionId;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getModel() {
        return model;
    }

    // ===== setters =====
    public void setMessage(String message) {
        this.message = message;
    }

    public void setChatSessionId(Long chatSessionId) {
        this.chatSessionId = chatSessionId;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public void setModel(String model) {
        this.model = model;
    }


}

