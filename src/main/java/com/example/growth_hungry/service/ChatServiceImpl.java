package com.example.growth_hungry.service;

import com.example.growth_hungry.dto.ChatMessageDto;
import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import com.example.growth_hungry.dto.ChatSessionDto;
import com.example.growth_hungry.model.User;
import com.example.growth_hungry.model.chat.ChatMessage;
import com.example.growth_hungry.model.chat.ChatSession;
import com.example.growth_hungry.model.chat.MessageRole;
import com.example.growth_hungry.repository.ChatMessageRepository;
import com.example.growth_hungry.repository.ChatSessionRepository;
import com.example.growth_hungry.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;


import javax.swing.*;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final UserRepository userRepository;
    private final AiClient aiClient;

    public ChatServiceImpl(ChatSessionRepository sessionRepo,
                           ChatMessageRepository messageRepo,
                           UserRepository userRepository,
                           AiClient aiClient) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.userRepository = userRepository;
        this.aiClient = aiClient;
    }

    @Override
    @Transactional
    public ChatResponse chat(ChatRequest req, String userEmail) {
        if (req == null) throw new IllegalArgumentException("Request must not be null");

        String message = req.getMessage() == null ? null : req.getMessage().trim();
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be blank");
        }

        // 1) current user from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String emailRaw = auth.getName();
        if (emailRaw == null || emailRaw.isBlank() || "anonymousUser".equalsIgnoreCase(emailRaw.trim())) {
            throw new AccessDeniedException("User is not authenticated");
        }

        final String email = emailRaw.trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found: " + email));

        // 2) normalize optional params
        Instant now = Instant.now();

        String systemPrompt = (req.getSystemPrompt() == null || req.getSystemPrompt().isBlank())
                ? null
                : req.getSystemPrompt().trim();

        String model = (req.getModel() == null || req.getModel().isBlank())
                ? DEFAULT_MODEL
                : req.getModel().trim();

        Long requestedSessionId = req.getChatSessionId();

        // 3) load/create session (must belong to this user)
        ChatSession session;
        if (requestedSessionId == null) {
            session = new ChatSession();
            session.setUser(user);
            session.setTitle(buildDefaultTitle(message));
            session.setCreatedAt(now);
            session.setUpdatedAt(now);
            session = sessionRepo.save(session);
        } else {
            session = sessionRepo.findByIdAndUser_Id(requestedSessionId, user.getId())
                    .orElseThrow(() -> new AccessDeniedException(
                            "Session not found or access denied: " + requestedSessionId));
            session.setUpdatedAt(now);

        }

        // 4) save USER message
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setRole(MessageRole.USER);
        userMsg.setContent(message);
        userMsg.setCreatedAt(now);
        messageRepo.save(userMsg);

        // 5) call AI
        String answer = aiClient.generate(message, systemPrompt, model);
        if (answer == null || answer.isBlank()) {
            answer = "(Empty response)";
        }

        // 6) save AI message
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSession(session);
        aiMsg.setRole(MessageRole.ASSISTANT);
        aiMsg.setContent(answer);
        aiMsg.setCreatedAt(Instant.now());
        messageRepo.save(aiMsg);

        // 7) update session updatedAt
        session.setUpdatedAt(Instant.now());
        sessionRepo.save(session);

        // 8) response
        ChatResponse resp = new ChatResponse(answer);
        resp.setChatSessionId(session.getId());
        resp.setModel(model);
        return resp;
    }
    private String buildDefaultTitle(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return "New chat";
        String t = userMessage.trim();
        return t.length() > 30 ? t.substring(0, 30) + "…" : t;
    }


    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionDto> getUserSessions(String userEmail) {

        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User email is required");
        }

        String email = userEmail.trim().toLowerCase();

        // 1) user must exist
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // 2) load sessions
        List<ChatSession> sessions = sessionRepo.findAllByUser_IdOrderByUpdatedAtDesc(user.getId());

        // 3) map Entity -> DTO
        List<ChatSessionDto> result = new ArrayList<>(sessions.size());
        for (ChatSession s : sessions) {
            ChatSessionDto dto = new ChatSessionDto();
            dto.setId(s.getId());
            dto.setTitle(s.getTitle());

            // 💡 на случай если updatedAt вдруг null (чтобы фронт не падал/не сортировал криво)
            dto.setUpdatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getCreatedAt());

            result.add(dto);
        }

        return result;
    }


    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getSessionMessages(Long sessionId, String userEmail) {

        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User email is required");
        }
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session id is required");
        }

        String email = userEmail.trim().toLowerCase();

        // 1) user must exist
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // 2) session must belong to this user
        ChatSession session = sessionRepo.findByIdAndUser_Id(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        // 3) load messages
        List<ChatMessage> messages = messageRepo.findBySession_IdOrderByCreatedAtAsc(session.getId());

        // 4) map to DTO
        List<ChatMessageDto> result = new ArrayList<>(messages.size());
        for (ChatMessage m : messages) {
            ChatMessageDto dto = new ChatMessageDto();
            dto.setId(m.getId());
            dto.setRole(m.getRole());
            dto.setContent(m.getContent());
            dto.setCreatedAt(m.getCreatedAt());
            result.add(dto);
        }

        return result;
    }
}