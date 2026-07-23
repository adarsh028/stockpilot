package com.stockpilot.integration.drive;

import com.stockpilot.common.crypto.TokenCipher;
import com.stockpilot.common.exception.ValidationException;
import com.stockpilot.integration.drive.dto.DriveStatusResponse;
import com.stockpilot.security.SignedTokenService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the per-tenant Google Drive OAuth lifecycle: building the consent URL,
 * handling the redirect callback, reporting status, and disconnecting. The OAuth
 * {@code state} is a signed token carrying the tenant + user, so the callback (which
 * arrives without a JWT) can be attributed to the right organization securely.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriveConnectionService {

    private static final String STATE_PURPOSE = "gdrive_connect";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final String DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file";

    private final DriveConnectionRepository connectionRepository;
    private final GoogleOAuthClient oAuthClient;
    private final DriveTokenService tokenService;
    private final SignedTokenService signedTokenService;
    private final GoogleDriveProperties properties;
    private final TokenCipher tokenCipher;

    @Transactional(readOnly = true)
    public DriveStatusResponse status(UUID orgId) {
        return connectionRepository.findByOrganizationId(orgId)
                .map(c -> new DriveStatusResponse(true, properties.isConfigured(),
                        c.getGoogleAccountEmail(), c.getCreatedAt()))
                .orElseGet(() -> new DriveStatusResponse(false, properties.isConfigured(), null, null));
    }

    /** Builds the Google consent URL with a signed state binding it to this tenant/user. */
    public String buildAuthorizeUrl(UUID orgId, UUID userId) {
        if (!properties.isConfigured()) {
            throw new ValidationException(
                    "Google Drive is not configured on the server. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.");
        }
        String state = signedTokenService.issue(STATE_PURPOSE,
                Map.of("orgId", orgId.toString(), "userId", userId.toString()), STATE_TTL);
        return oAuthClient.buildAuthorizationUrl(state);
    }

    /**
     * Completes the OAuth flow: verifies state, exchanges the code, and upserts the
     * connection with encrypted tokens. Returns the frontend URL to redirect the browser
     * to, with a {@code drive} status flag so the settings page can show the result.
     */
    @Transactional
    public String handleCallback(String code, String state, String error) {
        if (error != null && !error.isBlank()) {
            log.info("Google Drive consent was denied/failed: {}", error);
            return redirectUrl("error");
        }
        Claims claims = signedTokenService.verify(state, STATE_PURPOSE);
        UUID orgId = UUID.fromString(claims.get("orgId", String.class));
        UUID userId = UUID.fromString(claims.get("userId", String.class));

        GoogleOAuthClient.TokenResponse token = oAuthClient.exchangeCode(code);
        log.info("Google token exchange for org {} returned scope: {}",
                orgId, token == null ? null : token.scope());
        if (token == null || token.refreshToken() == null || token.refreshToken().isBlank()) {
            // No refresh token means we can't maintain long-lived access — treat as failure.
            log.warn("Google token exchange returned no refresh token for org {}", orgId);
            return redirectUrl("error");
        }
        if (!grantedRequiredScope(token.scope())) {
            // Google silently drops scopes not registered on the OAuth consent screen, so a
            // successful exchange can still lack drive.file — fail fast instead of a later 403.
            log.warn("Google token exchange for org {} did not grant drive.file (scope: {})",
                    orgId, token.scope());
            return redirectUrl("error");
        }
        String email = oAuthClient.fetchEmail(token.accessToken());

        DriveConnection connection = connectionRepository.findByOrganizationId(orgId)
                .orElseGet(() -> {
                    DriveConnection c = new DriveConnection();
                    c.setOrganizationId(orgId);
                    return c;
                });
        connection.setConnectedByUserId(userId);
        connection.setGoogleAccountEmail(email);
        // Re-authorizing starts a fresh folder so we always encrypt the new refresh token.
        connection.setRefreshToken(tokenCipher.encrypt(token.refreshToken()));
        tokenService.applyToken(connection, token);
        connectionRepository.save(connection);

        log.info("Google Drive connected for org {} ({})", orgId, email);
        return redirectUrl("connected");
    }

    @Transactional
    public void disconnect(UUID orgId) {
        connectionRepository.findByOrganizationId(orgId).ifPresent(connection -> {
            oAuthClient.revoke(tokenCipher.decrypt(connection.getRefreshToken()));
            connectionRepository.deleteByOrganizationId(orgId);
        });
    }

    private String redirectUrl(String result) {
        return UriComponentsBuilder.fromHttpUrl(properties.getPostConnectRedirect())
                .queryParam("drive", result)
                .build()
                .toUriString();
    }

    /** Google returns the space-separated set of scopes actually granted, which can be
     * narrower than requested if drive.file isn't registered on the OAuth consent screen. */
    private static boolean grantedRequiredScope(String scope) {
        if (scope == null) {
            return false;
        }
        return Arrays.asList(scope.split(" ")).contains(DRIVE_FILE_SCOPE);
    }
}
