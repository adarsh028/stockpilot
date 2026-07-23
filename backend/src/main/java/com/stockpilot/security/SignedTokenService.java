package com.stockpilot.security;

import com.stockpilot.common.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Issues and verifies short-lived, HMAC-signed tokens for purposes outside the main
 * auth flow — the Google OAuth {@code state} parameter and the signed image-proxy URLs.
 * Each token carries a {@code purpose} claim so a token minted for one use can never be
 * replayed against another. Signed with the same secret as the auth JWTs.
 */
@Service
public class SignedTokenService {

    private static final String PURPOSE_CLAIM = "purpose";

    private final String secret;
    private SecretKey key;

    public SignedTokenService(@Value("${app.jwt.secret}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(String purpose, Map<String, String> claims, Duration ttl) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .claim(PURPOSE_CLAIM, purpose)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key);
        claims.forEach(builder::claim);
        return builder.compact();
    }

    /**
     * Verifies signature, expiry, and the {@code purpose} claim. Throws
     * {@link UnauthorizedException} on any mismatch so callers can surface a 401.
     */
    public Claims verify(String token, String expectedPurpose) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedPurpose.equals(claims.get(PURPOSE_CLAIM, String.class))) {
                throw new UnauthorizedException("Token has the wrong purpose");
            }
            return claims;
        } catch (JwtException ex) {
            throw new UnauthorizedException("Invalid or expired token");
        }
    }
}
