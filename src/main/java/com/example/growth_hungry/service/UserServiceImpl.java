package com.example.growth_hungry.service;

import com.example.growth_hungry.dto.UserRegistrationDto;
import com.example.growth_hungry.model.User;
import com.example.growth_hungry.repository.UserRepository;
import com.example.growth_hungry.service.UserService;
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

    @Override
    public User saveUser(User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User registerUser(UserRegistrationDto userData) {
        // 1) проверка занятости (была пропущена закрывающая скобка ')')
        if (userRepository.existsByUsername(userData.getUsername())) {
            throw new IllegalArgumentException(
                    "Username already exists: " + userData.getUsername()
            );
        }

        // 2) DTO -> Entity (всё ДОЛЖНО быть внутри метода)
        User user = new User();
        user.setUsername(userData.getUsername());
        user.setPassword(passwordEncoder.encode(userData.getPassword()));


        // 3) сохранение
        return userRepository.save(user);
    } // <-- тут метод реально закрывается

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
