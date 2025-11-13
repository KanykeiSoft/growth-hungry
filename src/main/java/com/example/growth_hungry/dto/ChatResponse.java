package com.example.growth_hungry.dto;

public class ChatResponse {
    private String reply;
    private Long contextId;
    private String model;

    // Пустой конструктор (нужен для сериализации)
    public ChatResponse() {
    }

    // Полный конструктор
    public ChatResponse(String reply, Long contextId, String model) {
        this.reply = reply;
        this.contextId = contextId;
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

    public Long getContextId() {
        return contextId;
    }

    public void setContextId(Long contextId) {
        this.contextId = contextId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
