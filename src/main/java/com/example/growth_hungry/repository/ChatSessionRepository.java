package com.example.growth_hungry.repository;

import com.example.growth_hungry.model.chat.ChatMessage;
import com.example.growth_hungry.model.chat.ChatSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.*;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findByIdAndUser_Id(Long id, Long userId);

    // (на будущее) список сессий пользователя
        List<ChatSession> findAllByUser_IdOrderByUpdatedAtDesc(Long userId);



}
