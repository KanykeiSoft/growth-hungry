package com.example.growth_hungry.controller;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/chat", "/api/chat/**").authenticated()
                        .anyRequest().permitAll())
                // ✅ Принудительно возвращаем 401 и для "нет аутентификации", и для "доступ запрещён"
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) // вместо 403
                        .accessDeniedHandler((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                )
                .addFilterBefore(new TestJwtAuthByHeaderFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    static class TestJwtAuthByHeaderFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain) throws IOException, ServletException {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ") && auth.substring(7).equals("MOCK_TOKEN")) {
                var principal = User.withUsername("test-user").password("N/A").roles("USER").build();
                var authn = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authn);
            }
            chain.doFilter(request, response);
        }
    }
}
