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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GeneralChatServiceImpl implements GeneralChatService{
//    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
   private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final UserRepository userRepository;
    private final AiClient aiClient;

    public GeneralChatServiceImpl(ChatSessionRepository sessionRepo, ChatMessageRepository messageRepo, UserRepository userRepository, AiClient aiClient) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.userRepository = userRepository;
        this.aiClient = aiClient;
    }

    @Override
    @Transactional
    public ChatResponse chat(ChatRequest req, String userEmail) {
        if (req == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request body is required"
            );
        }

        String message = req.getMessage() == null ? null : req.getMessage().trim();
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message is required"
            );
        }

        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User email is required"
            );
        }

        final String email = userEmail.trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));

        Instant now = Instant.now();

        String systemPrompt = (req.getSystemPrompt() == null || req.getSystemPrompt().isBlank())
                ? null
                : req.getSystemPrompt().trim();

        String model = (req.getModel() == null || req.getModel().isBlank())
                ? DEFAULT_MODEL
                : req.getModel().trim();

        Long requestedSessionId = req.getChatSessionId();

        // 1) load/create session
        ChatSession session;
        if (requestedSessionId == null) {
            session = new ChatSession();
            session.setUser(user);
            session.setModel(model);
            session.setTitle(buildDefaultTitle(message));
            session.setCreatedAt(now);
            session.setUpdatedAt(now);

            session = sessionRepo.save(session);
        } else {
            session = sessionRepo.findByIdAndUser_Id(requestedSessionId, user.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Session not found"
                    ));

            // keep existing session model
            model = session.getModel();
        }

        // 2) save USER message
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setUser(user);
        userMsg.setRole(MessageRole.USER);
        userMsg.setContent(message);
        userMsg.setCreatedAt(now);
        messageRepo.save(userMsg);

        // 3) call AI
        String answer;
        try {
            answer = aiClient.generate(message, systemPrompt, model);
            if (answer == null || answer.isBlank()) {
                answer = "(Empty response)";
            }
        } catch (Exception e) {
            answer = "Sorry, I couldn't generate a response right now.";
        }

        // 4) save AI message
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSession(session);
        aiMsg.setUser(user);
        aiMsg.setRole(MessageRole.ASSISTANT);
        aiMsg.setContent(answer);
        aiMsg.setCreatedAt(Instant.now());
        messageRepo.save(aiMsg);

        // 5) update session timestamp
        session.setUpdatedAt(Instant.now());
        sessionRepo.save(session);

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

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        return sessionRepo.findAllByUser_IdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(s -> {
                    ChatSessionDto dto = new ChatSessionDto();
                    dto.setId(s.getId());
                    dto.setTitle(s.getTitle());
                    dto.setModel(s.getModel());
                    dto.setCreatedAt(s.getCreatedAt());
                    dto.setUpdatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getCreatedAt());
                    return dto;
                })
                .toList();
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

    @Override
    @Transactional
    public void deleteSession(Long sessionId, String userEmail) {
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session id is required");
        }

        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User email is required");
        }

        String email = userEmail.trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        ChatSession session = sessionRepo.findByIdAndUser_Id(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        sessionRepo.delete(session);
    }
}
