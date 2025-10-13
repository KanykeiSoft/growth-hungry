package com.example.growth_hungry.service;

import com.example.growth_hungry.api.UsernameAlreadyExistsException;
import com.example.growth_hungry.dto.UserRegistrationDto;
import com.example.growth_hungry.model.User;
import com.example.growth_hungry.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** trim + toLowerCase; пустые строки -> null */
    private static String norm(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toLowerCase();
    }

    /** Грубая проверка, что пароль уже BCrypt ($2a/$2b/$2y ...) */
    private static boolean looksHashed(String s) {
        return s != null && s.startsWith("$2");
    }

    @Override
    public User saveUser(User user) {
        // 1) Пароль: если «сырой» — захешировать
        String pwd = user.getPassword();
        if (pwd != null && !looksHashed(pwd)) {
            user.setPassword(passwordEncoder.encode(pwd));
        }

        // 2) Username/Email: нормализуем одинаково
        user.setUsername(norm(user.getUsername()));
        user.setEmail(norm(user.getEmail()));

        // 3) Сохраняем
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        String username = norm(dto.getUsername());
        String email    = norm(dto.getEmail());

        if (username != null && userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException("Username already exists: " + dto.getUsername());
        }
        // Если добавишь проверку email — делай по нормализованному значению:
        // if (email != null && userRepository.existsByEmail(email)) {
        //     throw new EmailAlreadyExistsException("Email already exists: " + dto.getEmail());
        // }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(norm(username)); // ← нормализуем вход
    }

    @Override
    public String login(String username, String rawPassword) {
        var user = userRepository.findByUsername(norm(username))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return "dummy-token"; // здесь позже вернёшь реальный JWT/refresh
    }
}
