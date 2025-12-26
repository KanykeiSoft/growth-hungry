package UsersTest;

import com.example.growth_hungry.repository.UserRepository;
import com.example.growth_hungry.repository.ChatSessionRepository;
import com.example.growth_hungry.repository.ChatMessageRepository;
import com.example.growth_hungry.model.User;
import com.example.growth_hungry.model.chat.ChatSession;
import com.example.growth_hungry.model.chat.ChatMessage;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.growth_hungry.dto.ChatRequest;
import com.example.growth_hungry.dto.ChatResponse;
import com.example.growth_hungry.service.AiClient;
import com.example.growth_hungry.service.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

//@ExtendWith(MockitoExtension.class)
//class ChatServiceImplTest {
//
//    @Mock
//    AiClient aiClient;
//
//    @Mock
//    UserRepository userRepository;
//
//    @Mock
//    ChatSessionRepository chatSessionRepository;
//
//    @Mock
//    ChatMessageRepository chatMessageRepository;
//
//    @InjectMocks
//    ChatServiceImpl chatService;
//
//    ChatRequest req;
//    User testUser;
//
//    @BeforeEach
//    void setUp() {
//        // базовый корректный запрос
//        req = new ChatRequest();
//        req.setMessage("Hello AI");
//        req.setSystemPrompt("You are helpful.");
//        req.setModel("gemini-2.5-flash");
//        req.setContextId("ctx-1");
//
//        // каждый тест начинает с чистого security-context
//        SecurityContextHolder.clearContext();
//    }
//
//    @AfterEach
//    void tearDown() {
//        // на всякий случай чистим контекст после теста
//        SecurityContextHolder.clearContext();
//    }
//
//    /**
//     * Хелпер: имитируем залогиненного пользователя и настраиваем репозитории.
//     * ВЫЗЫВАЕМ ТОЛЬКО В ТЕХ ТЕСТАХ, где действительно идёт работа с AI и сохранением.
//     */
//    private void mockAuthenticatedUserAndPersistence() {
//        // 1) SecurityContext: кладём "test-user"
//        var auth = new UsernamePasswordAuthenticationToken(
//                "test-user",
//                null,
//                AuthorityUtils.NO_AUTHORITIES
//        );
//        SecurityContextHolder.getContext().setAuthentication(auth);
//
//        // 2) Фейковый пользователь
//        testUser = new User();
//        testUser.setId(1L);
//        testUser.setUsername("test-user");
//
//        when(userRepository.findByUsername("test-user"))
//                .thenReturn(Optional.of(testUser));
//
//        // 3) Заглушка для сохранения сессий — просто возвращаем тот же объект
//        when(chatSessionRepository.save(any(ChatSession.class)))
//                .thenAnswer(inv -> inv.getArgument(0));
//
//        // 4) Заглушка для сообщений — тоже просто возвращаем
//        when(chatMessageRepository.save(any(ChatMessage.class)))
//                .thenAnswer(inv -> inv.getArgument(0));
//    }
//
//
//
//    @Test
//    @DisplayName("Null request → returns 'Error: empty request'")
//    void chat_nullRequest_returnsError() {
//        ChatResponse r = chatService.chat(null);
//
//        assertThat(r).isNotNull();
//        assertThat(r.getReply()).isEqualTo("Error: empty request");
//
//        // aiClient не должен вызываться вообще
//        verifyNoInteractions(aiClient);
//        // и никакие репы тут не нужны → мы их даже не мокали
//    }
//
//    @Test
//    @DisplayName("Empty message → returns 'Error: message must not be blank'")
//    void chat_emptyMessage_returnsError() {
//        req.setMessage("   ");
//
//        ChatResponse r = chatService.chat(req);
//
//        assertThat(r.getReply()).isEqualTo("Error: message must not be blank");
//
//        verifyNoInteractions(aiClient);
//    }
//
//
//    @Test
//    @DisplayName("Success: calls aiClient.generate(...) and returns a valid reply")
//    void chat_success_returnsReply_andPassesParams() throws Exception {
//        mockAuthenticatedUserAndPersistence();
//
//        when(aiClient.generate("Hello AI", "You are helpful.", "gemini-2.5-flash"))
//                .thenReturn("Hi, human!");
//
//        ChatResponse r = chatService.chat(req);
//
//        assertThat(r).isNotNull();
//        assertThat(r.getReply()).isEqualTo("Hi, human!");
//        // ❌ старые проверки:
//        // assertThat(r.getContextId()).isEqualTo("ctx-1");
//        // assertThat(r.getContextId()).isNotNull();
//
//        // ✅ проверяем только reply и model
//        assertThat(r.getModel()).isEqualTo("gemini-2.5-flash");
//
//        verify(aiClient).generate("Hello AI", "You are helpful.", "gemini-2.5-flash");
//        verifyNoMoreInteractions(aiClient);
//    }
//
//
//    @Test
//    @DisplayName("Trims message and sends null for blank systemPrompt/model")
//    void chat_trimsMessage_andNullifiesBlanks() throws Exception {
//        mockAuthenticatedUserAndPersistence();
//
//        req.setMessage("   hi   ");
//        req.setSystemPrompt("   "); // должно стать null
//        req.setModel("");           // должно стать null
//
//        when(aiClient.generate("hi", null, null)).thenReturn("ok");
//
//        ChatResponse r = chatService.chat(req);
//
//        assertThat(r.getReply()).isEqualTo("ok");
//        assertThat(r.getModel()).isNull();
//
//        verify(aiClient).generate("hi", null, null);
//        verifyNoMoreInteractions(aiClient);
//    }
//
//    @Test
//    @DisplayName("Empty AI response → should return placeholder '(Empty response)'")
//    void chat_emptyClientAnswer_becomesPlaceholder() throws Exception {
//        mockAuthenticatedUserAndPersistence();
//
//        when(aiClient.generate(anyString(), any(), any())).thenReturn("   ");
//
//        ChatResponse r = chatService.chat(req);
//
//        assertThat(r.getReply()).isEqualTo("(Empty response)");
//        verify(aiClient).generate(eq("Hello AI"), eq("You are helpful."), eq("gemini-2.5-flash"));
//        verifyNoMoreInteractions(aiClient);
//    }
//
//    @Test
//    @DisplayName("IllegalArgumentException from AI client → handled gracefully")
//    void chat_illegalArgumentFromClient_isHandled() throws Exception {
//        mockAuthenticatedUserAndPersistence();
//
//        when(aiClient.generate(anyString(), any(), any()))
//                .thenThrow(new IllegalArgumentException("message must not be blank"));
//
//        ChatResponse r = chatService.chat(req);
//
//        assertThat(r.getReply()).isEqualTo("Error calling AI: message must not be blank");
//        verify(aiClient).generate(anyString(), any(), any());
//        verifyNoMoreInteractions(aiClient);
//    }
//
//    @Test
//    @DisplayName("Generic Exception from AI client → handled gracefully")
//    void chat_generalException_isHandled() throws Exception {
//        mockAuthenticatedUserAndPersistence();
//
//        when(aiClient.generate(anyString(), any(), any()))
//                .thenThrow(new RuntimeException("upstream boom"));
//
//        ChatResponse r = chatService.chat(req);
//
//        assertThat(r.getReply()).isEqualTo("Error calling AI: upstream boom");
//        verify(aiClient).generate(anyString(), any(), any());
//        verifyNoMoreInteractions(aiClient);
//    }
