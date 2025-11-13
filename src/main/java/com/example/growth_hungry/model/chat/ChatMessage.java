package com.example.growth_hungry.model.chat;


import com.example.growth_hungry.model.User;
import jakarta.persistence.*;

import java.time.Instant;
//optional = false - Сообщение не может существовать без чат
// fetch = FetchType.LAZY - Не подгружает ChatSession, пока ты не вызовешь getSession()
//@JoinColumn(name = "session_id") -- Создаёт колонку в БД, которая связывает сообщение с сессией
//@ManyToOne - Указывает, что сообщение принадлежит сессии
//@Id -- Primary Key
//@GeneratedValue(strategy = GenerationType.IDENTITY)-- база данных создаёт его автоматически ID
// Instant -- точный момент времени (timestamp) в формате UTC.

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    // 👇 НЕОБЯЗАТЕЛЬНО, но удобно: кто автор сообщения
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public ChatMessage() {
    }

    public ChatMessage(String content, MessageRole role) {
        this.content = content;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        // если вручную не задали createdAt, установим сейчас
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // ======= GETTERS =======
    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public MessageRole getRole() {
        return role;
    }

    public ChatSession getSession() {
        return session;
    }

    public User getUser() {
        return user;
    }

    // ======= SETTERS =======
    public void setContent(String content) {
        this.content = content;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public void setUser(User user) {
        this.user = user;
    }
}

