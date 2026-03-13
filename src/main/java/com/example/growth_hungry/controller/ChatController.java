package com.example.growth_hungry.controller;

import com.example.growth_hungry.dto.ChatMessageDto;
import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import com.example.growth_hungry.dto.ChatSessionDto;
import com.example.growth_hungry.service.ChatService;
import com.example.growth_hungry.service.GeneralChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private  final GeneralChatService generalChatService;

    public ChatController(ChatService chatService, GeneralChatService generalChatService) {
        this.chatService = chatService;
        this.generalChatService = generalChatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest req,
                                             Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(generalChatService.chat(req, auth.getName()));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionDto>> getUserSessions(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(generalChatService.getUserSessions(auth.getName()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getSessionMessages(@PathVariable Long sessionId,
                                                                   Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(generalChatService.getSessionMessages(sessionId, auth.getName()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId,
                                              Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        generalChatService.deleteSession(sessionId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sections/{sectionId}")
    public ResponseEntity<ChatResponse> getSectionChat(@PathVariable Long sectionId,
                                                       Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(chatService.getSectionChat(sectionId, auth.getName()));
    }

    @PostMapping("/sections/{sectionId}/messages")
    public ResponseEntity<ChatResponse> chatInSection(@PathVariable Long sectionId,
                                                      @Valid @RequestBody ChatRequest req,
                                                      Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(chatService.chatInSection(sectionId, req, auth.getName()));
    }


}
