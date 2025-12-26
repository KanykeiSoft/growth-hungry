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

        log.info("{} {} | Authorization={}", method, path, header);

        // Skip open endpoints (auth, health, preflight)
        if ("OPTIONS".equalsIgnoreCase(method)
                || path.startsWith("/api/auth/")
                || "/actuator/health".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Reject if Authorization header is missing or invalid
        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("No Bearer token for protected path {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();

        // Do not override existing authentication
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Validate token and extract subject (email)
            if (jwtUtil.isValid(token)) {
                String email = jwtUtil.getSubject(token);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                AuthorityUtils.NO_AUTHORITIES
                        );

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            log.error("Error while processing JWT: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
