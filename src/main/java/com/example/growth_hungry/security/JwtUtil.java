package com.example.growth_hungry.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    private final Key key;                 // секретный ключ для подписи JWT
    private final long ttlMinutes;         // срок жизни токена
    private final String issuer;           // кто выдал токен
    private final String audience;         // для кого токен

    private static final long CLOCK_SKEW_SECONDS = 60;
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);// допуск по времени

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
    public String generateToken(String subject) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttlMinutes * 60)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.debug("JWT generated for subject={}", subject);
        return token;
    }

    /** Достаём пользователя из sub */
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /** True, если подпись, время и issuer/audience корректны */
    public boolean isValid(String token) {
        try {
            Claims c = parseClaims(token);
            log.info("✅ JWT OK: sub={}, iss={}, aud={}, exp={}",
                    c.getSubject(),
                    c.getIssuer(),
                    c.getAudience(),
                    c.getExpiration());
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("⏰ JWT expired: {}", ex.getMessage());
            return false;
        } catch (io.jsonwebtoken.SignatureException ex) {
            log.warn("🔐 JWT signature invalid: {}", ex.getMessage());
            return false;
        } catch (Exception ex) {
            log.warn("⚠️ JWT invalid: {} ({})", ex.getMessage(), ex.getClass().getSimpleName());
            return false;
        }
    }

    /** Парсим токен и одновременно валидируем его */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .setAllowedClockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
