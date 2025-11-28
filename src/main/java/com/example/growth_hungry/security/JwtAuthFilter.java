package com.example.growth_hungry.security;

import com.example.growth_hungry.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String path   = request.getServletPath();
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        log.info(" {} {} | Authorization={}", method, path, header);

        // 1. Открытые пути
        if ("OPTIONS".equalsIgnoreCase(method)
                || path.startsWith("/api/auth/")
                || "/actuator/health".equals(path)) {
            log.debug("Skipping JwtAuthFilter for open path {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Нет Bearer-токена
        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("⛔ No Bearer token for protected path {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Достаём токен без "Bearer "
        String token = header.substring(7).trim();
        log.debug("📥 Extracted JWT (first 20 chars): {}...",
                token.length() > 20 ? token.substring(0, 20) : token);

        // 4. Если уже есть аутентификация — не трогаем
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.debug("🔁 SecurityContext already has auth: {}",
                    SecurityContextHolder.getContext().getAuthentication());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 5. Валидируем токен
            if (!jwtUtil.isValid(token)) {
                log.warn("❌ jwtUtil.isValid(token) returned FALSE for path {}", path);
            } else {
                String subject = jwtUtil.getSubject(token);
                log.info("🔐 JWT valid, subject={}", subject);

                var auth = new UsernamePasswordAuthenticationToken(
                        subject,
                        null,
                        AuthorityUtils.NO_AUTHORITIES
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("👤 SecurityContext set for user={}", subject);
            }
        } catch (Exception e) {
            log.error("💥 Error while processing JWT: {}", e.getMessage(), e);
        }

        // 6. Продолжаем цепочку фильтров
        filterChain.doFilter(request, response);
    }
}
