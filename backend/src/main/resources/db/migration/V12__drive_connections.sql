-- Per-tenant Google Drive connection. One row per organization; product images
-- are stored in the connected account's Drive and streamed back through the API.
CREATE TABLE drive_connections (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id         UUID NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    google_account_email    VARCHAR(320),
    -- OAuth tokens are stored encrypted (AES-GCM), never in plaintext.
    refresh_token           TEXT         NOT NULL,
    access_token            TEXT,
    access_token_expires_at TIMESTAMPTZ,
    -- Id of the folder created in the tenant's Drive that holds product images.
    folder_id               VARCHAR(255),
    connected_by_user_id    UUID,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_drive_connections_org ON drive_connections(organization_id);
