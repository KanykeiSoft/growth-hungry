package com.example.growth_hungry.service;
import com.example.growth_hungry.dto.ChatMessageDto;
import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import com.example.growth_hungry.dto.ChatSessionDto;
import java.util.List;

public interface ChatService {
    ChatResponse chat(ChatRequest req, String userEmail);
    List<ChatSessionDto> getUserSessions(String userEmail);
    List<ChatMessageDto> getSessionMessages(Long sessionId, String userEmail);
}
