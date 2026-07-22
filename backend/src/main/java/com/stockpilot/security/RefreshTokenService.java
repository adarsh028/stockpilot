package com.stockpilot.security;

import com.stockpilot.common.exception.UnauthorizedException;
import com.stockpilot.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.jwt.refresh-ttl-days}")
    private long refreshTtlDays;

    /** Issue a brand-new opaque refresh token (returns the raw value; only the hash is stored). */
    @Transactional
    public String issue(UUID userId) {
        String raw = generateRawToken();
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS));
        repository.save(token);
        return raw;
    }

    /**
     * Rotate a presented refresh token: validate it, revoke it, issue a replacement.
     * Reuse of an already-revoked token triggers a full revoke of the user's tokens.
     */
    @Transactional
    public Rotation rotate(String rawToken) {
        RefreshToken existing = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (existing.getRevokedAt() != null) {
            log.warn("Refresh token reuse detected for user {} — revoking all tokens", existing.getUserId());
            repository.revokeAllForUser(existing.getUserId(), Instant.now());
            throw new UnauthorizedException("Refresh token has been revoked");
        }
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        String newRaw = generateRawToken();
        RefreshToken replacement = new RefreshToken();
        replacement.setUserId(existing.getUserId());
        replacement.setTokenHash(hash(newRaw));
        replacement.setExpiresAt(Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS));
        repository.save(replacement);

        existing.setRevokedAt(Instant.now());
        existing.setReplacedById(replacement.getId());
        repository.save(existing);

        return new Rotation(existing.getUserId(), newRaw);
    }

    @Transactional
    public void revokeAll(UUID userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record Rotation(UUID userId, String newRefreshToken) {
    }
}
