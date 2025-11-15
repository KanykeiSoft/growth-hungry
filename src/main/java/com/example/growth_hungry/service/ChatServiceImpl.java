package com.example.growth_hungry.service;

import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import com.example.growth_hungry.model.User;
import com.example.growth_hungry.model.chat.ChatMessage;
import com.example.growth_hungry.model.chat.ChatSession;
import com.example.growth_hungry.model.chat.MessageRole;
import com.example.growth_hungry.repository.ChatMessageRepository;
import com.example.growth_hungry.repository.ChatSessionRepository;
import com.example.growth_hungry.repository.UserRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

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

//    @Value("${ai.default-model:gemini-2.5-flash}")
//    private String defaultModel;

    @Transactional
    @Override
    public ChatResponse chat(ChatRequest req) {
        // 1. Валидация входных данных
        if (req == null) {
            return new ChatResponse("Error: empty request");
        }
        if (req.getMessage() == null || req.getMessage().isBlank()) {
            return new ChatResponse("Error: message must not be blank");
        }

        final String message = req.getMessage().trim();

        final String systemPrompt =
                (req.getSystemPrompt() == null || req.getSystemPrompt().isBlank())
                        ? null
                        : req.getSystemPrompt().trim(); // можно ещё и trim

        // если модель не указана — подставляем дефолтную (пока null)
        final String requestedModel =
                (req.getModel() == null || req.getModel().isBlank())
                        ? null
                        : req.getModel().trim();

        final Long requestedSessionId = req.getChatSessionId();

        try {
            // 2. Определяем текущего пользователя (из токена / SecurityContext)
            User user = getCurrentUser();

            // 3. Находим или создаём ChatSession
            ChatSession session;
            if (requestedSessionId == null) {
                // новая сессия
                session = new ChatSession();
                session.setUser(user);
                session.setTitle(buildDefaultTitle(message));
                session.setCreatedAt(Instant.now());
                session.setUpdatedAt(Instant.now());

                session = sessionRepo.save(session);
                log.info("Created new ChatSession id={} for user id={}", session.getId(), user.getId());
            } else {
                // существующая сессия
                session = sessionRepo.findByIdAndUserId(requestedSessionId, user.getId())
                        .orElseThrow(() -> new IllegalArgumentException("ChatSession not found: " + requestedSessionId));

                // проверяем, что сессия принадлежит текущему пользователю
                if (session.getUser() == null || !session.getUser().getId().equals(user.getId())) {
                    throw new SecurityException("Access denied to ChatSession: " + requestedSessionId);
                }

                session.setUpdatedAt(Instant.now());
            }

            // 4. Сохраняем сообщение пользователя
            ChatMessage userMessage = new ChatMessage();
            userMessage.setSession(session);
            userMessage.setUser(user);
            userMessage.setRole(MessageRole.USER);
            userMessage.setContent(message);
            userMessage.setCreatedAt(Instant.now());
            messageRepo.save(userMessage);

            // 5. Вызываем AI-клиент — БЕЗ ВНУТРЕННЕГО try/catch
            String answer = aiClient.generate(message, systemPrompt, requestedModel);

            // 6. Обрабатываем пустой ответ
            if (answer == null || answer.isBlank()) {
                answer = "(Empty response)";
            }

            // 7. Сохраняем ответ ассистента
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setSession(session);
            aiMessage.setUser(null);                       // ассистент, не пользователь
            aiMessage.setRole(MessageRole.ASSISTANT);
            aiMessage.setContent(answer);
            aiMessage.setCreatedAt(Instant.now());
            messageRepo.save(aiMessage);

            // 8. Обновляем updatedAt у сессии
            session.setUpdatedAt(Instant.now());
            sessionRepo.save(session);

            // 9. Формируем ChatResponse
            ChatResponse resp = new ChatResponse(answer);
            resp.setContextId(session.getId());    // фронт будет слать как chatSessionId
            resp.setModel(requestedModel);
            return resp;

        } catch (IllegalArgumentException badInput) {
            // сюда попадёт IllegalArgumentException из aiClient.generate(...)
            log.warn("AI request error: {}", badInput.getMessage());
            return new ChatResponse("Error calling AI: " + badInput.getMessage());
        } catch (Exception ex) {
            // сюда попадёт RuntimeException("upstream boom") и любые другие
            log.error("AI invocation error", ex);
            return new ChatResponse("Error calling AI: " + ex.getMessage());
        }
    }

    // -------- вспомогательные методы --------

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        Object principal = auth.getPrincipal();

        // если Spring подставил анонимного пользователя
        if (principal instanceof String str && "anonymousUser".equals(str)) {
            throw new RuntimeException("User is not authenticated");
        }

        String username;

        if (principal instanceof User) {
            return (User) principal;
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof String str) {
            username = str;
        } else {
            throw new RuntimeException("Unsupported principal type: " + principal.getClass());
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }


    private String buildDefaultTitle(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "New chat";
        }
        String trimmed = userMessage.trim();
        return trimmed.length() > 30 ? trimmed.substring(0, 30) + "…" : trimmed;
    }
}