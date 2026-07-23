package com.stockpilot.integration.drive;

import com.stockpilot.common.crypto.TokenCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Resolves a usable (plaintext) access token for a tenant's Drive connection,
 * transparently refreshing it via the stored refresh token when it has expired or is
 * about to. The refreshed token is re-encrypted and persisted so subsequent requests
 * reuse it until it expires.
 */
@Service
@RequiredArgsConstructor
public class DriveTokenService {

    /** Refresh a little early so a token doesn't expire mid-request. */
    private static final long EXPIRY_SKEW_SECONDS = 60;

    private final GoogleOAuthClient oAuthClient;
    private final TokenCipher tokenCipher;

    /**
     * Returns a valid access token for the connection, refreshing and persisting a new
     * one if needed. Must run in a transaction so the entity update is flushed.
     */
    @Transactional
    public String validAccessToken(DriveConnection connection) {
        if (isFresh(connection)) {
            return tokenCipher.decrypt(connection.getAccessToken());
        }
        String refreshToken = tokenCipher.decrypt(connection.getRefreshToken());
        GoogleOAuthClient.TokenResponse token = oAuthClient.refreshAccessToken(refreshToken);
        applyToken(connection, token);
        return token.accessToken();
    }

    private boolean isFresh(DriveConnection connection) {
        return connection.getAccessToken() != null
                && connection.getAccessTokenExpiresAt() != null
                && connection.getAccessTokenExpiresAt()
                        .isAfter(Instant.now().plusSeconds(EXPIRY_SKEW_SECONDS));
    }

    /**
     * Stores the access token (and refresh token, if Google returned a new one) on the
     * connection, encrypting both. Shared by the initial exchange and later refreshes.
     */
    void applyToken(DriveConnection connection, GoogleOAuthClient.TokenResponse token) {
        connection.setAccessToken(tokenCipher.encrypt(token.accessToken()));
        long expiresIn = token.expiresIn() != null ? token.expiresIn() : 3600L;
        connection.setAccessTokenExpiresAt(Instant.now().plus(expiresIn, ChronoUnit.SECONDS));
        // Google only returns a refresh token on the first authorization; keep the old one otherwise.
        if (token.refreshToken() != null && !token.refreshToken().isBlank()) {
            connection.setRefreshToken(tokenCipher.encrypt(token.refreshToken()));
        }
    }
}
