package com.example.growth_hungry.dto;

import com.example.growth_hungry.model.chat.MessageRole;
import java.time.Instant;

public class ChatMessageDto {
    private Long id;
    private MessageRole role;
    private String content;
    private Instant createdAt;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
