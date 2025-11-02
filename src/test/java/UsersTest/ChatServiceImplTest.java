package UsersTest;


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

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    AiClient aiClient;

    @InjectMocks
    ChatServiceImpl chatService;

    ChatRequest req;

    @BeforeEach
    void setUp() {
        req = new ChatRequest();
        req.setMessage("Hello AI");
        req.setSystemPrompt("You are helpful.");
        req.setModel("gemini-1.5-flash");
        req.setContextId("ctx-1");
    }

    @Test
    @DisplayName("Null request → returns 'Error: empty request'")
    void chat_nullRequest_returnsError() {
        ChatResponse r = chatService.chat(null);
        assertThat(r).isNotNull();
        assertThat(r.getReply()).isEqualTo("Error: empty request");
        verifyNoInteractions(aiClient);
    }

    @Test
    @DisplayName("Empty message → returns 'Error: message must not be blank'")
    void chat_emptyMessage_returnsError() {
        req.setMessage("   ");
        ChatResponse r = chatService.chat(req);
        assertThat(r.getReply()).isEqualTo("Error: message must not be blank");
        verifyNoInteractions(aiClient);
    }

    @Test
    @DisplayName("Success: calls aiClient.generate(...) and returns a valid reply")
    void chat_success_returnsReply_andPassesParams() throws Exception {
        when(aiClient.generate("Hello AI", "You are helpful.", "gemini-1.5-flash"))
                .thenReturn("Hi, human!");

        ChatResponse r = chatService.chat(req);

        assertThat(r).isNotNull();
        assertThat(r.getReply()).isEqualTo("Hi, human!");
        assertThat(r.getContextId()).isEqualTo("ctx-1");
        assertThat(r.getModel()).isEqualTo("gemini-1.5-flash");

        verify(aiClient).generate("Hello AI", "You are helpful.", "gemini-1.5-flash");
        verifyNoMoreInteractions(aiClient);
    }

    @Test
    @DisplayName("Trims message and sends null for blank systemPrompt/model")
    void chat_trimsMessage_andNullifiesBlanks() throws Exception {
        req.setMessage("   hi   ");
        req.setSystemPrompt("   "); // should become null
        req.setModel("");           // should become null

        when(aiClient.generate("hi", null, null)).thenReturn("ok");

        ChatResponse r = chatService.chat(req);

        assertThat(r.getReply()).isEqualTo("ok");
        assertThat(r.getModel()).isNull();

        verify(aiClient).generate("hi", null, null);
    }

    @Test
    @DisplayName("Empty AI response → should return placeholder '(Empty response)'")
    void chat_emptyClientAnswer_becomesPlaceholder() throws Exception {
        when(aiClient.generate(anyString(), any(), any())).thenReturn("   ");

        ChatResponse r = chatService.chat(req);

        assertThat(r.getReply()).isEqualTo("(Empty response)");
        verify(aiClient).generate(eq("Hello AI"), eq("You are helpful."), eq("gemini-1.5-flash"));
    }

    @Test
    @DisplayName("IllegalArgumentException from AI client → handled gracefully")
    void chat_illegalArgumentFromClient_isHandled() throws Exception {
        when(aiClient.generate(anyString(), any(), any()))
                .thenThrow(new IllegalArgumentException("message must not be blank"));

        ChatResponse r = chatService.chat(req);

        assertThat(r.getReply()).isEqualTo("Error calling AI: message must not be blank");
        verify(aiClient).generate(anyString(), any(), any());
    }

    @Test
    @DisplayName("Generic Exception from AI client → handled gracefully")
    void chat_generalException_isHandled() throws Exception {
        when(aiClient.generate(anyString(), any(), any()))
                .thenThrow(new RuntimeException("upstream boom"));

        ChatResponse r = chatService.chat(req);

        assertThat(r.getReply()).isEqualTo("Error calling AI: upstream boom");
        verify(aiClient).generate(anyString(), any(), any());
    }
}
