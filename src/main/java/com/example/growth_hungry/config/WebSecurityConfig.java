package com.example.growth_hungry.config;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    @Bean
    public SecurityFilterChain security(HttpSecurity http)throws Exception{
        http
                .csrf(csrf -> csrf.disable()) // отключаем CSRF
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // разрешаем все запросы
                );
        return http.build();
    }
}
