package com.stockpilot.security;

import com.stockpilot.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final String secret;
    private final long accessTtlMinutes;
    private SecretKey key;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-ttl-minutes}") long accessTtlMinutes) {
        this.secret = secret;
        this.accessTtlMinutes = accessTtlMinutes;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 256 bits (32 bytes)");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTtlMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("orgId", user.getOrganizationId().toString())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public StockPilotPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new StockPilotPrincipal(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get("orgId", String.class)),
                com.stockpilot.user.UserRole.valueOf(claims.get("role", String.class)),
                claims.get("email", String.class)
        );
    }
}
