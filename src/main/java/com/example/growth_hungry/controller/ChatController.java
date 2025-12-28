package com.example.growth_hungry.controller;

import com.example.growth_hungry.dto.ChatMessageDto;
import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import com.example.growth_hungry.dto.ChatSessionDto;
import com.example.growth_hungry.service.ChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest req,
                                             Authentication auth) {

        if (req == null || req.getMessage() == null || req.getMessage().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Error: message must not be blank"));
        }

        // auth гарантирован SecurityConfig'ом (иначе до контроллера не дойдёт)
        return ResponseEntity.ok(chatService.chat(req, auth.getName()));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionDto>> getUserSessions(Authentication auth) {
        return ResponseEntity.ok(chatService.getUserSessions(auth.getName()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getSessionMessages(@PathVariable Long sessionId,
                                                                   Authentication auth) {
        return ResponseEntity.ok(chatService.getSessionMessages(sessionId, auth.getName()));
    }
}
