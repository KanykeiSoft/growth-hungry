package com.example.growth_hungry.security;

import com.example.growth_hungry.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getServletPath();

        // public endpoints
        if ("OPTIONS".equalsIgnoreCase(method)) return true;
        if (path.startsWith("/api/auth/")) return true;
        if ("/actuator/health".equals(path)) return true;
        if ("/error".equals(path)) return true; // важно!
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String path   = request.getServletPath();
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        log.info("{} {} | Authorization={}", method, path, header);

        // если аутентификация уже есть — не трогаем
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // если нет Bearer токена — просто пропускаем дальше
        if (header == null || !header.startsWith("Bearer ")) {
            log.debug("No Bearer token for {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();

        try {
            if (!jwtUtil.isValid(token)) {
                log.warn("Invalid JWT for {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtUtil.getSubject(token);
            if (email == null || email.isBlank()) {
                log.warn("JWT subject is blank for {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            AuthorityUtils.createAuthorityList("ROLE_USER")
                    );

            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            log.error("JWT processing error: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
