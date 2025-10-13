package com.example.growth_hungry.repository;

import com.example.growth_hungry.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // Найти пользователя по username
    Optional<User> findByUsername(String username);

    // Проверить, существует ли пользователь с таким username
    boolean existsByUsername(String username);

    boolean existsByEmail(String email); // если проверяешь email
}
