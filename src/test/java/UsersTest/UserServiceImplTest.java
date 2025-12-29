package UsersTest;

import com.example.growth_hungry.dto.UserRegistrationDto;
import com.example.growth_hungry.model.User;
import com.example.growth_hungry.repository.UserRepository;
import com.example.growth_hungry.security.JwtUtil;
import com.example.growth_hungry.service.UserServiceImpl;
import com.example.growth_hungry.api.EmailAlreadyExistsException;
import com.example.growth_hungry.api.UsernameAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // ничего не нужно
    }

    @Test
    void registerUser_success_savesNormalizedUsernameEmail_andEncodedPassword() {
        // Arrange
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("  Aidar  ");
        dto.setEmail("  AIDAR@MAIL.COM  ");
        dto.setPassword("rawPass");

        // после norm() будет: username="aidar", email="aidar@mail.com"
        when(userRepository.existsByUsername("aidar")).thenReturn(false);
        when(userRepository.existsByEmail("aidar@mail.com")).thenReturn(false);

        when(passwordEncoder.encode("rawPass")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        User saved = userService.registerUser(dto);

        // Assert: проверим, что в save ушли нормализованные значения и encoded пароль
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User toSave = captor.getValue();

        assertEquals("aidar", toSave.getUsername());
        assertEquals("aidar@mail.com", toSave.getEmail());
        assertEquals("encodedPass", toSave.getPassword());

        // и то, что метод вернул то же самое (мы так настроили mock save)
        assertEquals("aidar", saved.getUsername());
        assertEquals("aidar@mail.com", saved.getEmail());
        assertEquals("encodedPass", saved.getPassword());

        verify(userRepository).existsByUsername("aidar");
        verify(userRepository).existsByEmail("aidar@mail.com");
        verify(passwordEncoder).encode("rawPass");
        verifyNoMoreInteractions(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void registerUser_usernameTaken_throws() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("Aidar");
        dto.setEmail("aidar@mail.com");
        dto.setPassword("rawPass");

        when(userRepository.existsByUsername("aidar")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class, () -> userService.registerUser(dto));

        verify(userRepository).existsByUsername("aidar");
        verifyNoInteractions(passwordEncoder, jwtUtil);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_emailTaken_throws() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("Aidar");
        dto.setEmail("aidar@mail.com");
        dto.setPassword("rawPass");

        when(userRepository.existsByUsername("aidar")).thenReturn(false);
        when(userRepository.existsByEmail("aidar@mail.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.registerUser(dto));

        verify(userRepository).existsByUsername("aidar");
        verify(userRepository).existsByEmail("aidar@mail.com");
        verifyNoInteractions(passwordEncoder, jwtUtil);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findByEmail_found_normalizesInput() {
        User u = new User();
        u.setId(10L);
        u.setUsername("aidar");
        u.setEmail("aidar@mail.com");
        u.setPassword("encodedPass");

        when(userRepository.findByEmail("aidar@mail.com")).thenReturn(Optional.of(u));

        Optional<User> result = userService.findByEmail("  AIDAR@MAIL.COM ");

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());
        assertEquals("aidar@mail.com", result.get().getEmail());

        verify(userRepository).findByEmail("aidar@mail.com");
        verifyNoMoreInteractions(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void findByEmail_notFound() {
        when(userRepository.findByEmail("unknown@mail.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("unknown@mail.com");

        assertTrue(result.isEmpty());
        verify(userRepository).findByEmail("unknown@mail.com");
        verifyNoMoreInteractions(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void login_success_returnsJwtToken() {
        User u = new User();
        u.setEmail("aidar@mail.com");
        u.setPassword("encodedPass");

        when(userRepository.findByEmail("aidar@mail.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("rawPass", "encodedPass")).thenReturn(true);
        when(jwtUtil.generateToken("aidar@mail.com")).thenReturn("JWT_TOKEN");

        String token = userService.login("  AIDAR@MAIL.COM  ", "rawPass");

        assertEquals("JWT_TOKEN", token);

        verify(userRepository).findByEmail("aidar@mail.com");
        verify(passwordEncoder).matches("rawPass", "encodedPass");
        verify(jwtUtil).generateToken("aidar@mail.com");
        verifyNoMoreInteractions(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void login_userNotFound_throwsBadCredentials() {
        when(userRepository.findByEmail("aidar@mail.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> userService.login("aidar@mail.com", "rawPass"));

        verify(userRepository).findByEmail("aidar@mail.com");
        verifyNoMoreInteractions(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void login_passwordMismatch_throwsBadCredentials() {
        User u = new User();
        u.setEmail("aidar@mail.com");
        u.setPassword("encodedPass");

        when(userRepository.findByEmail("aidar@mail.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong", "encodedPass")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> userService.login("aidar@mail.com", "wrong"));

        verify(userRepository).findByEmail("aidar@mail.com");
        verify(passwordEncoder).matches("wrong", "encodedPass");
        verifyNoMoreInteractions(userRepository, passwordEncoder, jwtUtil);
    }
}


