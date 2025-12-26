package com.example.growth_hungry.service;

import com.example.growth_hungry.api.EmailAlreadyExistsException;
import com.example.growth_hungry.api.UsernameAlreadyExistsException;
import com.example.growth_hungry.dto.UserRegistrationDto;
import com.example.growth_hungry.model.User;
import com.example.growth_hungry.repository.UserRepository;
import com.example.growth_hungry.security.JwtUtil;
import java.util.regex.Pattern;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");



    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /** trim + toLowerCase; пустые строки -> null */
    private static String norm(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toLowerCase();
    }


    private static boolean looksHashed(String value) {
        return value != null && BCRYPT_PATTERN.matcher(value).matches();
    }
    @Override
    public User saveUser(User user) {
        // 1) if raw password then encoding

        String pwd = user.getPassword();
        if (pwd != null && !looksHashed(pwd)) {
            user.setPassword(passwordEncoder.encode(pwd));
        }

        user.setUsername(norm(user.getUsername()));
        user.setEmail(norm(user.getEmail()));
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

         if (email != null && userRepository.existsByEmail(email)) {
             throw new EmailAlreadyExistsException("Email already exists: " + dto.getEmail());
         }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(norm(email));
    }

    @Override
    @Transactional(readOnly = true)
    public String login(String email, String rawPassword) {
        String normalizedEmail = norm(email);
        var user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return jwtUtil.generateToken(user.getEmail());
    }
}
