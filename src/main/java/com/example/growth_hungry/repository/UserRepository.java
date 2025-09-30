package com.example.growth_hungry.repository;

import com.example.growth_hungry.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
