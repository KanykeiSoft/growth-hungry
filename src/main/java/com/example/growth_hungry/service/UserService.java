package com.example.growth_hungry.service;

import com.example.growth_hungry.dto.UserRegistrationDto;
import com.example.growth_hungry.model.User;
import java.util.Optional;

public interface UserService {
    User saveUser(User user);
    User registerUser(UserRegistrationDto dto);
    Optional<User> findByEmail(String email);
    String login(String email, String rawPassword);
}

