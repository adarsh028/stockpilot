package com.stockpilot.integration.drive;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * App-level Google Drive OAuth configuration. A single OAuth client (id/secret) is
 * shared across all tenants; each tenant authorizes it against their own Drive account.
 */
@Getter
@Component
public class GoogleDriveProperties {

    /** OAuth scopes: least-privilege — only files this app creates, plus the account email. */
    public static final String SCOPES = "openid email https://www.googleapis.com/auth/drive.file";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String postConnectRedirect;
    private final String folderName;

    public GoogleDriveProperties(
            @Value("${app.integrations.google-drive.client-id:}") String clientId,
            @Value("${app.integrations.google-drive.client-secret:}") String clientSecret,
            @Value("${app.integrations.google-drive.redirect-uri}") String redirectUri,
            @Value("${app.integrations.google-drive.post-connect-redirect}") String postConnectRedirect,
            @Value("${app.integrations.google-drive.folder-name}") String folderName) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.postConnectRedirect = postConnectRedirect;
        this.folderName = folderName;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
