package com.example.growth_hungry.service;
import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;

public interface ChatService {
    ChatResponse chat(ChatRequest req);
}
