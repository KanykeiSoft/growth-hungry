package UsersTest;

import com.example.growth_hungry.dto.UserRegistrationDto;
import com.example.growth_hungry.model.User;
import com.example.growth_hungry.repository.UserRepository;
import com.example.growth_hungry.service.UserServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;   // ✅ any(), eq(), …

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerUser_success() {
        // Arrange
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("aidar");
        dto.setPassword("rawPass");

        // имя свободно
        when(userRepository.existsByUsername("aidar")).thenReturn(false);
        when(passwordEncoder.encode("rawPass")).thenReturn("encodedPass");

        doAnswer(inv -> {
            User u = inv.getArgument(0, User.class);
            try {
                u.setId(1L);
            } catch (Exception ignored) {
            }
            return u;
        }).when(userRepository).save(any(User.class));

        // Act
        userService.registerUser(dto);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertNotNull(saved);
        assertEquals("aidar", saved.getUsername());
        assertEquals("encodedPass", saved.getPassword());

        verify(userRepository).existsByUsername("aidar");
        verify(passwordEncoder).encode("rawPass");
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }
    @Test
    void findByUsername_found(){
        User u = new User();
        u.setId(10L);
        u.setUsername("aidar");
        u.setPassword("encodedPass");

        when(userRepository.findByUsername("aidar")).thenReturn(Optional.of(u));
        Optional<User> result = userService.findByUsername("aidar");

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());
        assertEquals("aidar", result.get().getUsername());
        verify(userRepository).findByUsername("aidar");
        verifyNoMoreInteractions(userRepository, passwordEncoder);

        @Test



    }
}
