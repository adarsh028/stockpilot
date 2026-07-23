package com.stockpilot.integration.drive;

import com.stockpilot.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A single organization's Google Drive connection. The OAuth tokens are stored
 * encrypted at rest (see {@link com.stockpilot.common.crypto.TokenCipher}); nothing
 * in this entity is ever exposed to clients directly.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "drive_connections")
public class DriveConnection extends TenantEntity {

    @Column(name = "google_account_email")
    private String googleAccountEmail;

    /** Encrypted refresh token — the long-lived credential used to mint access tokens. */
    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    /** Encrypted, short-lived access token. Refreshed on demand when expired. */
    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    /** Id of the StockPilot folder created in the tenant's Drive. */
    @Column(name = "folder_id")
    private String folderId;

    @Column(name = "connected_by_user_id")
    private UUID connectedByUserId;
}
