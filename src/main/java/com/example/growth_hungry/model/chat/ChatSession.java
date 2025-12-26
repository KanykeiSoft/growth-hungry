package com.example.growth_hungry.model.chat;

//@OneToMany(mappedBy = "session") -- Указывает, что у сессии есть список сообщений
//cascade = CascadeType.ALL -- При сохранении/удалении чата делает то же с сообщениями
// orphanRemoval = true - Удаляет сообщения, если их убрать из списка

import com.example.growth_hungry.model.User;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "chat_session")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    // --- Конструкторы ---
    public ChatSession() {}

    public ChatSession(String title, User user) {
        this.title = title;
        this.user = user;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // --- Геттеры и сеттеры ---
    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }



}




