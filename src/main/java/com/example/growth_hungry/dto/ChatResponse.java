package com.example.growth_hungry.dto;

public class ChatResponse {
    private String reply;
    private Long chatSessionId;
    private String model;
    private String title;      // NEW
    private Boolean isNew;

    // Пустой конструктор (нужен для сериализации)
    public ChatResponse() {
    }

    // Полный конструктор
    public ChatResponse(String reply, Long chatSessionId, String model) {
        this.reply = reply;
        this.chatSessionId = chatSessionId;
        this.model = model;
    }

    // Конструктор только с ответом (удобно при ошибках или быстрых ответах)
    public ChatResponse(String reply) {
        this.reply = reply;
    }

    // Getters / Setters
    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public Long getChatSessionId() {
        return chatSessionId;
    }

    public void setChatSessionId(Long chatSessionId) {
        this.chatSessionId = chatSessionId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
