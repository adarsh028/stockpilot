CREATE TABLE import_batches (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    channel_id       UUID REFERENCES channels(id),
    kind             VARCHAR(20)  NOT NULL,
    file_name        VARCHAR(255) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    rows_total       INTEGER      NOT NULL DEFAULT 0,
    rows_success     INTEGER      NOT NULL DEFAULT 0,
    rows_failed      INTEGER      NOT NULL DEFAULT 0,
    error_report_url VARCHAR(500),
    uploaded_by      UUID REFERENCES users(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_import_org ON import_batches(organization_id);
CREATE INDEX idx_import_org_kind ON import_batches(organization_id, kind);
