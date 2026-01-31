package com.example.growth_hungry.repository;

import com.example.growth_hungry.model.chat.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySession_IdOrderByCreatedAtAsc(Long sessionId);
    void deleteBySession_Id(Long sessionId);
    List<ChatMessage> findTop50BySession_IdOrderByCreatedAtDesc(Long sessionId);


}
