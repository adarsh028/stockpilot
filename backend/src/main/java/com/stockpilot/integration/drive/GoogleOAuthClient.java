package com.stockpilot.integration.drive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Thin wrapper over Google's OAuth 2.0 endpoints (token exchange, refresh, revoke) and
 * the userinfo endpoint. Follows the same hand-rolled {@link RestClient} style as the
 * Brevo email client rather than pulling in the full Google API SDK.
 */
@Slf4j
@Component
public class GoogleOAuthClient {

    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke";
    private static final String USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo";

    private final GoogleDriveProperties props;
    private final RestClient restClient = RestClient.create();

    public GoogleOAuthClient(GoogleDriveProperties props) {
        this.props = props;
    }

    /** Builds the Google consent-screen URL the user is redirected to. */
    public String buildAuthorizationUrl(String state) {
        String url = UriComponentsBuilder.fromHttpUrl(AUTH_ENDPOINT)
                .queryParam("client_id", props.getClientId())
                .queryParam("redirect_uri", props.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", GoogleDriveProperties.SCOPES)
                // offline + consent guarantees a refresh_token even on re-authorization.
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", state)
                .build()
                .toUriString();
        log.info("Google Drive authorization URL: {}", url);
        return url;
    }

    /** Exchanges an authorization code for access + refresh tokens. */
    public TokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("redirect_uri", props.getRedirectUri());
        form.add("grant_type", "authorization_code");
        return postForm(form);
    }

    /** Mints a fresh access token from a stored refresh token. */
    public TokenResponse refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("refresh_token", refreshToken);
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("grant_type", "refresh_token");
        return postForm(form);
    }

    /** Best-effort token revocation on disconnect. */
    public void revoke(String token) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("token", token);
            restClient.post()
                    .uri(REVOKE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to revoke Google token: {}", ex.getMessage());
        }
    }

    /** Reads the email address of the authorized Google account. */
    public String fetchEmail(String accessToken) {
        try {
            UserInfo info = restClient.get()
                    .uri(USERINFO_ENDPOINT)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(UserInfo.class);
            return info == null ? null : info.email();
        } catch (Exception ex) {
            log.warn("Failed to fetch Google account email: {}", ex.getMessage());
            return null;
        }
    }

    private TokenResponse postForm(MultiValueMap<String, String> form) {
        return restClient.post()
                .uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("scope") String scope) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UserInfo(String email) {
    }
}
