package com.example.growth_hungry.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    private final Key key;                 // секретный ключ для подписи JWT
    private final long ttlMinutes;         // срок жизни токена
    private final String issuer;           // кто выдал токен
    private final String audience;         // для кого токен

    private static final long CLOCK_SKEW_SECONDS = 60; // допуск по времени

    public JwtUtil(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.ttl-minutes:30}") long ttlMinutes,
            @Value("${jwt.issuer:growth-hungry}") String issuer,
            @Value("${jwt.audience:gh-users}") String audience
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.ttlMinutes = ttlMinutes;
        this.issuer = issuer;
        this.audience = audience;
    }

    /** Создаём access-token */
    public String generate(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttlMinutes * 60)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Достаём пользователя из sub */
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /** True, если подпись, время и issuer/audience корректны */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /** Парсим токен и одновременно валидируем его */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .setAllowedClockSkewSeconds(CLOCK_SKEW_SECONDS)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


}
