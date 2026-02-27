package UsersTest;

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
import com.example.growth_hungry.service.AiClient;
import com.example.growth_hungry.service.ChatServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    ChatSessionRepository sessionRepo;
    @Mock
    ChatMessageRepository messageRepo;
    @Mock
    UserRepository userRepository;
    @Mock
    AiClient aiClient;
    @Mock
    SectionRepository sectionRepository;

    @InjectMocks
    ChatServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- helpers ---
    private static void setAuthEmail(String email) {
        var auth = new UsernamePasswordAuthenticationToken(email, "n/a", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static User user(long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    // =========================
    // chat()
    // =========================

    @Test
    void chat_createsNewSession_savesUserAndAiMessages_returnsResponse() {
        // given
        setAuthEmail("TEST@Email.com");

        ChatRequest req = new ChatRequest();
        req.setMessage("  Hello AI  ");   // trims
        req.setChatSessionId(null);
        req.setSystemPrompt("  You are helpful ");
        req.setModel(" gemini-2.5-flash ");

        User u = user(10L, "test@email.com");
        when(userRepository.findByEmail(eq("test@email.com"))).thenReturn(Optional.of(u));

        // sessionRepo.save(new ChatSession()) -> return with id
        when(sessionRepo.save(any(ChatSession.class))).thenAnswer(inv -> {
            ChatSession s = inv.getArgument(0);
            if (s.getId() == null) s.setId(100L);
            return s;
        });

        when(aiClient.generate(eq("Hello AI"), eq("You are helpful"), eq("gemini-2.5-flash")))
                .thenReturn("Hi! How can I help?");

        // when
        ChatResponse resp = service.chat(req, "TEST@Email.com");

        // then
        assertNotNull(resp);
        assertEquals("Hi! How can I help?", resp.getReply());
        assertEquals(100L, resp.getChatSessionId());
        assertEquals("gemini-2.5-flash", resp.getModel());

        // verify session created
        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        verify(sessionRepo, atLeastOnce()).save(sessionCaptor.capture());
        ChatSession created = sessionCaptor.getValue();
        assertEquals(u, created.getUser());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());
        assertTrue(created.getTitle() != null && !created.getTitle().isBlank());

        // verify message saved twice (USER + ASSISTANT)
        ArgumentCaptor<ChatMessage> msgCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepo, times(2)).save(msgCaptor.capture());
        List<ChatMessage> saved = msgCaptor.getAllValues();

        assertEquals(MessageRole.USER, saved.get(0).getRole());
        assertEquals("Hello AI", saved.get(0).getContent());
        assertEquals(100L, saved.get(0).getSession().getId());

        assertEquals(MessageRole.ASSISTANT, saved.get(1).getRole());
        assertEquals("Hi! How can I help?", saved.get(1).getContent());
        assertEquals(100L, saved.get(1).getSession().getId());

        verify(aiClient).generate("Hello AI", "You are helpful", "gemini-2.5-flash");
    }

    @Test
    void chat_withExistingSession_checksOwnership_updatesSession_savesMessages() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Question?");
        req.setChatSessionId(555L);
        req.setSystemPrompt(null);
        req.setModel(null); // -> DEFAULT_MODEL inside service

        User u = user(7L, "user@mail.com");
        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(u));

        ChatSession existing = new ChatSession();
        existing.setId(555L);
        existing.setUser(u);
        existing.setCreatedAt(Instant.now().minusSeconds(1000));
        existing.setUpdatedAt(Instant.now().minusSeconds(1000));

        when(sessionRepo.findByIdAndUser_Id(555L, 7L)).thenReturn(Optional.of(existing));
        when(aiClient.generate(eq("Question?"), isNull(), eq("gemini-2.5-flash"))).thenReturn("Answer!");

        when(sessionRepo.save(any(ChatSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        ChatResponse resp = service.chat(req, "user@mail.com");

        // then
        assertEquals("Answer!", resp.getReply());
        assertEquals(555L, resp.getChatSessionId());
        assertEquals("gemini-2.5-flash", resp.getModel());

        verify(sessionRepo).findByIdAndUser_Id(555L, 7L);
        verify(messageRepo, times(2)).save(any(ChatMessage.class));
        verify(sessionRepo, atLeastOnce()).save(existing);
    }

    @Test
    void chat_blankMessage_throwsIllegalArgumentException() {
        setAuthEmail("a@b.com");
        ChatRequest req = new ChatRequest();
        req.setMessage("   ");

        assertThrows(IllegalArgumentException.class, () -> service.chat(req, "x"));
        verifyNoInteractions(userRepository, sessionRepo, messageRepo, aiClient);
    }

    @Test
    void chat_noAuthentication_throwsAccessDenied() {
        ChatRequest req = new ChatRequest();
        req.setMessage("hi");

        assertThrows(AccessDeniedException.class, () -> service.chat(req, "   "));
        verifyNoInteractions(userRepository, sessionRepo, messageRepo, aiClient);
    }

    @Test
    void chat_aiReturnsBlank_savesEmptyResponseFallback() {
        ChatRequest req = new ChatRequest();
        req.setMessage("hi");

        User u = user(1L, "u@u.com");
        when(userRepository.findByEmail("u@u.com")).thenReturn(Optional.of(u));
        when(sessionRepo.save(any(ChatSession.class))).thenAnswer(inv -> {
            ChatSession s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        when(aiClient.generate(eq("hi"), isNull(), eq("gemini-2.5-flash"))).thenReturn("   ");

        ChatResponse resp = service.chat(req, "u@u.com");
        assertEquals("(Empty response)", resp.getReply());

        ArgumentCaptor<ChatMessage> msgCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepo, times(2)).save(msgCaptor.capture());
        ChatMessage aiMsg = msgCaptor.getAllValues().get(1);
        assertEquals("(Empty response)", aiMsg.getContent());
    }

    // =========================
    // getUserSessions()
    // =========================

    @Test
    void getUserSessions_mapsEntitiesToDtos_handlesNullUpdatedAt() {
        User u = user(5L, "x@y.com");
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.of(u));

        ChatSession s1 = new ChatSession();
        s1.setId(1L);
        s1.setTitle("A");
        s1.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        s1.setUpdatedAt(null); // should fallback to createdAt

        ChatSession s2 = new ChatSession();
        s2.setId(2L);
        s2.setTitle("B");
        s2.setCreatedAt(Instant.parse("2025-01-02T00:00:00Z"));
        s2.setUpdatedAt(Instant.parse("2025-01-03T00:00:00Z"));

        when(sessionRepo.findAllByUser_IdOrderByUpdatedAtDesc(5L)).thenReturn(List.of(s2, s1));

        List<ChatSessionDto> res = service.getUserSessions("  X@Y.COM  ");

        assertEquals(2, res.size());
        assertEquals(2L, res.get(0).getId());
        assertEquals("B", res.get(0).getTitle());
        assertEquals(Instant.parse("2025-01-03T00:00:00Z"), res.get(0).getUpdatedAt());

        assertEquals(1L, res.get(1).getId());
        assertEquals("A", res.get(1).getTitle());
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), res.get(1).getUpdatedAt()); // fallback
    }

    @Test
    void getUserSessions_blankEmail_throws401() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getUserSessions("   "));
        assertEquals(401, ex.getStatusCode().value());
        verifyNoInteractions(userRepository, sessionRepo);
    }

    // =========================
    // getSessionMessages()
    // =========================

    @Test
    void getSessionMessages_returnsDtos_inAscendingOrder() {
        User u = user(9L, "a@a.com");
        when(userRepository.findByEmail("a@a.com")).thenReturn(Optional.of(u));

        ChatSession session = new ChatSession();
        session.setId(77L);
        session.setUser(u);

        when(sessionRepo.findByIdAndUser_Id(77L, 9L)).thenReturn(Optional.of(session));

        ChatMessage m1 = new ChatMessage();
        m1.setId(1L);
        m1.setRole(MessageRole.USER);
        m1.setContent("hi");
        m1.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));

        ChatMessage m2 = new ChatMessage();
        m2.setId(2L);
        m2.setRole(MessageRole.ASSISTANT);
        m2.setContent("hello");
        m2.setCreatedAt(Instant.parse("2025-01-01T00:00:01Z"));

        when(messageRepo.findBySession_IdOrderByCreatedAtAsc(77L)).thenReturn(List.of(m1, m2));

        List<ChatMessageDto> res = service.getSessionMessages(77L, " A@A.COM ");

        assertEquals(2, res.size());
        assertEquals(1L, res.get(0).getId());
        assertEquals(MessageRole.USER, res.get(0).getRole());
        assertEquals("hi", res.get(0).getContent());

        assertEquals(2L, res.get(1).getId());
        assertEquals(MessageRole.ASSISTANT, res.get(1).getRole());
        assertEquals("hello", res.get(1).getContent());
    }

    @Test
    void getSessionMessages_nullSessionId_throws400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getSessionMessages(null, "x@y.com"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void getSessionMessages_sessionNotBelongToUser_throws404() {
        User u = user(9L, "a@a.com");
        when(userRepository.findByEmail("a@a.com")).thenReturn(Optional.of(u));
        when(sessionRepo.findByIdAndUser_Id(77L, 9L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getSessionMessages(77L, "a@a.com"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void chatInSection_sectionIdNull_shouldThrow() {
        ChatRequest req = new ChatRequest();

        // Проверяем: если sectionId = null → должно выбросить исключение
        assertThrows(IllegalArgumentException.class,
                () -> service.chatInSection(null, req, "a@test.com"));
    }

    @Test
    void chatInSection_reqNull_shouldThrow() {
        // Проверяем: если req = null → IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> service.chatInSection(1L, null, "a@test.com"));
    }

    @Test
    void chatInSection_emailBlank_shouldThrow() {
        ChatRequest req = new ChatRequest();

        // Проверяем: если email пустой → IllegalStateException
        assertThrows(IllegalStateException.class,
                () -> service.chatInSection(1L, req, "   "));
    }

    @Test
    void chatInSection_happyPath_shouldReturnNotImplementedResponse() {
        ChatRequest req = new ChatRequest();
        req.setMessage("hello");
        User user = new User();
        user.setId(1L);
        user.setEmail("a@test.com");

        Section section = new Section();
        section.setId(10L);
        section.setContent("content");

        ChatSession session = new ChatSession();
        session.setId(99L);

        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(sessionRepo.findByUser_IdAndSectionId(1L, 10L)).thenReturn(Optional.of(session));
        when(aiClient.generate(anyString(), anyString(), isNull())).thenReturn("AI answer");

        ChatResponse resp = service.chatInSection(10L, req, "a@test.com");

        assertNotNull(resp);
        assertEquals(99L, resp.getChatSessionId());
        assertEquals("AI answer", resp.getReply());
    }

    // -------------------- getSectionChat --------------------

    @Test
    void getSectionChat_sectionIdNull_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getSectionChat(null, "a@test.com"));
    }

    @Test
    void getSectionChat_emailBlank_shouldThrow() {
        assertThrows(IllegalStateException.class,
                () -> service.getSectionChat(1L, " "));
    }

    @Test
    void getSectionChat_userNotFound_shouldThrow401() {
        // Говорим мок-репозиторию: если ищем "a@test.com" → вернуть Optional.empty()
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.empty());

        // Вызываем метод и ожидаем ResponseStatusException
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getSectionChat(1L, "A@Test.com"));

        // Проверяем, что это именно 401 Unauthorized
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void getSectionChat_sessionNotFound_shouldReturnEmpty() {
        // Подготовили пользователя
        User user = new User();
        user.setId(99L);

        // user найден
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));

        // но сессия не найдена (Optional.empty())
        when(sessionRepo.findByUser_IdAndSectionId(99L, 7L)).thenReturn(Optional.empty());

        ChatResponse resp = service.getSectionChat(7L, "  A@TEST.com  ");

        // ожидаем: если чата нет → chatSessionId = null и messages пустые
        assertNull(resp.getChatSessionId());
        assertNotNull(resp.getMessages());
        assertTrue(resp.getMessages().isEmpty());

        // доп. проверка: email нормализовался в lowercase+trim
        verify(userRepository).findByEmail("a@test.com");
    }

    @Test
    void getSectionChat_sessionFound_messagesMappedCorrectly() {
        // user
        User user = new User();
        user.setId(1L);

        // session
        ChatSession session = new ChatSession();
        session.setId(555L);

        // message 1
        ChatMessage m1 = new ChatMessage();
        m1.setId(10L);
        m1.setRole(m1.getRole().USER);
        m1.setContent("hi");
        m1.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));

        // message 2
        ChatMessage m2 = new ChatMessage();
        m2.setId(11L);
        m2.setRole(m2.getRole().ASSISTANT);
        m2.setContent("hello");
        m2.setCreatedAt(Instant.parse("2026-01-01T10:00:01Z"));

        // Настраиваем поведение моков
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(sessionRepo.findByUser_IdAndSectionId(1L, 3L)).thenReturn(Optional.of(session));
        when(messageRepo.findBySession_IdOrderByCreatedAtAsc(555L))
                .thenReturn(List.of(m1, m2));

        // Действие
        ChatResponse resp = service.getSectionChat(3L, "A@test.com");

        // Проверки
        assertEquals(555L, resp.getChatSessionId());
        assertEquals(2, resp.getMessages().size());

        ChatMessageDto d1 = resp.getMessages().get(0);
        assertEquals(10L, d1.getId());
        assertEquals(d1.getRole().USER, d1.getRole());
        assertEquals("hi", d1.getContent());
        assertEquals(m1.getCreatedAt(), d1.getCreatedAt());
    }


}
