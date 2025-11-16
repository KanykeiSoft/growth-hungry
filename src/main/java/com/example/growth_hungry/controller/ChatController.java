package com.example.growth_hungry.controller;

import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import com.example.growth_hungry.service.ChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest req, Principal principal) {

        if (req == null || req.getMessage() == null || req.getMessage().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Error: message must not be blank"));
        }

        String username = principal.getName();
        ChatResponse resp = chatService.chat(req);
        return ResponseEntity.ok(resp);
    }


}