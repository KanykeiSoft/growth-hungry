package com.example.growth_hungry.service;

import com.example.growth_hungry.dto.ChatMessageDto;
import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import com.example.growth_hungry.dto.ChatSessionDto;
import com.example.growth_hungry.model.Section;
import com.example.growth_hungry.model.User;
import com.example.growth_hungry.model.chat.ChatMessage;
import com.example.growth_hungry.model.chat.ChatSession;
import com.example.growth_hungry.model.chat.MessageRole;
import com.example.growth_hungry.repository.ChatMessageRepository;
import com.example.growth_hungry.repository.ChatSessionRepository;
import com.example.growth_hungry.repository.SectionRepository;
import com.example.growth_hungry.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private final SectionRepository sectionRepository;

    public ChatServiceImpl(ChatSessionRepository sessionRepo,
                           ChatMessageRepository messageRepo,
                           UserRepository userRepository,
                           AiClient aiClient, SectionRepository sectionRepository) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.userRepository = userRepository;
        this.aiClient = aiClient;
        this.sectionRepository = sectionRepository;
    }

    @Override
    public ChatResponse chatInSection(Long sectionId, ChatRequest req, String userEmail) {

        if (userEmail == null || userEmail.isBlank())
            throw new IllegalStateException("User email is required");

        if (sectionId == null)
            throw new IllegalArgumentException("Section id is required");

        if (req == null)
            throw new IllegalArgumentException("Message is required");

        String email = userEmail.trim().toLowerCase(Locale.ROOT);

        //Find user
        User user =  userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
 // по секции и пользователя ищем чат если нет создаем новый
        //find section
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

        //find session
        ChatSession session = sessionRepo.findByUser_IdAndSectionId(user.getId(), sectionId)
                .orElse(null);
        String model = "gemini-2.5-flash";
        if (session == null){
            session = new ChatSession();
            session.setUser(user);
            session.setSectionId(sectionId);
            session.setModel(model);
            session = sessionRepo.save(session);

        }

        String userMessage = req.getMessage().trim();
            ChatMessage message = new ChatMessage();
            message.setSession(session);
            message.setRole(MessageRole.USER);
            message.setContent(userMessage);
            message.setCreatedAt(Instant.now());
            messageRepo.save(message);

            String sectionContent = section.getContent();
            String prompt = sectionContent + "\n\nUser question: " + userMessage;
            String assistantAnswer = aiClient.generate(prompt,
                    "You are a helpful course assistant. Answer based on the section content.",
                    null
                    );
            ChatMessage assistantMsg = new ChatMessage();
            assistantMsg.setSession(session);
            assistantMsg.setRole(MessageRole.ASSISTANT);
            assistantMsg.setContent(assistantAnswer);
            assistantMsg.setCreatedAt(Instant.now());
            messageRepo.save(assistantMsg);

        ChatResponse resp = new ChatResponse();
        resp.setChatSessionId(session.getId());
        resp.setReply(assistantAnswer);
        return resp;


    }



    @Override
    @Transactional(readOnly = true)
    public ChatResponse getSectionChat(Long sectionId, String userEmail) {

        // 1️⃣ Validate input (consistent with chatInSection)
        if (userEmail == null || userEmail.isBlank())
            throw new IllegalStateException("User email is required");

        if (sectionId == null)
            throw new IllegalArgumentException("Section id is required");

        // 2️⃣ Resolve user
        String email = userEmail.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // 3️⃣ Find chat session (Option 1: one session per section)
        ChatSession session = sessionRepo
                .findByUser_IdAndSectionId(user.getId(), sectionId)
                .orElse(null);

        ChatResponse resp = new ChatResponse();

        // 4️⃣ No chat yet → return empty response
        if (session == null) {
            resp.setChatSessionId(null);
            resp.setMessages(List.of());
            return resp;
        }

        // 5️⃣ Load LIMITED history (last 50 messages)
        List<ChatMessage> msgs = messageRepo
                .findBySession_IdOrderByCreatedAtAsc(session.getId());

// optional: limit last 50 (если надо)
        if (msgs.size() > 50) {
            msgs = msgs.subList(msgs.size() - 50, msgs.size());
        }




        // 6️⃣ Map to DTO
        List<ChatMessageDto> dto = msgs.stream().map(m -> {
            ChatMessageDto d = new ChatMessageDto();
            d.setId(m.getId());
            d.setRole(m.getRole());        // USER / ASSISTANT
            d.setContent(m.getContent());
            d.setCreatedAt(m.getCreatedAt());
            return d;
        }).toList();

        // 7️⃣ Build response
        resp.setChatSessionId(session.getId());
        resp.setMessages(dto);
        return resp;
    }




}