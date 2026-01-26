package com.example.growth_hungry;

import com.example.growth_hungry.model.User;
import com.example.growth_hungry.model.chat.ChatMessage;
import com.example.growth_hungry.model.chat.ChatSession;
import com.example.growth_hungry.model.chat.MessageRole;
import com.example.growth_hungry.repository.ChatMessageRepository;
import com.example.growth_hungry.repository.ChatSessionRepository;
import com.example.growth_hungry.repository.UserRepository;
import com.example.growth_hungry.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
public class ChatServiceImplIT {
    @Autowired
    ChatService chatService;

    @Autowired
    UserRepository userRepository;
    @Autowired
    ChatSessionRepository sessionRepo;
    @Autowired
    ChatMessageRepository messageRepo;

    @Test
    void deleteSession_shouldDeleteSessionAndMessages() {
        // ARRANGE: создаём пользователя
        User user = new User();
        user.setEmail("test@mail.com");
        user.setPassword("123"); // <-- поменяй на правильное поле, если нужно
        user = userRepository.save(user);

        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setTitle("hello");
        session.setModel("gemini-2.5-flash"); // если поле обязательное
        session = sessionRepo.save(session);

        ChatMessage msg = new ChatMessage();
        msg.setSession(session);
        msg.setUser(user); // если у тебя есть поле user в ChatMessage
        msg.setRole(MessageRole.USER);
        msg.setContent("hi");
        messageRepo.save(msg);

        Long sessionId = session.getId();
        chatService.deleteSession(sessionId, user.getEmail());
        assertTrue(sessionRepo.findById(sessionId).isEmpty());
        assertTrue(messageRepo.findBySession_IdOrderByCreatedAtAsc(sessionId).isEmpty());

    }
}
