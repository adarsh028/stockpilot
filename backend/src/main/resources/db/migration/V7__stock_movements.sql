CREATE TABLE stock_movements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    sku_id          UUID NOT NULL REFERENCES skus(id),
    channel_id      UUID REFERENCES channels(id),
    type            VARCHAR(20) NOT NULL,
    quantity_delta  INTEGER     NOT NULL,
    reason          VARCHAR(255),
    reference_id    UUID,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_movements_org_sku ON stock_movements(organization_id, sku_id);
CREATE INDEX idx_movements_org_created ON stock_movements(organization_id, created_at);
