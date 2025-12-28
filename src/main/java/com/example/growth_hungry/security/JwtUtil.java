package com.example.growth_hungry.security;

import io.jsonwebtoken.*;
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
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final long CLOCK_SKEW_SECONDS = 60;

    private final Key key;
    private final long ttlMinutes;
    private final String issuer;
    private final String audience;

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

    public String generateToken(String subject) {
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

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);

            if (!issuer.equals(claims.getIssuer())) return false;
            if (!audience.equals(claims.getAudience())) return false;

            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
            return false;
        } catch (JwtException ex) {
            log.warn("JWT invalid: {}", ex.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .setAllowedClockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
