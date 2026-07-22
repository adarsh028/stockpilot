CREATE TABLE sales (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id      UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    channel_id           UUID NOT NULL REFERENCES channels(id),
    sku_id               UUID NOT NULL REFERENCES skus(id),
    quantity             INTEGER       NOT NULL,
    unit_price           NUMERIC(12,2) NOT NULL,
    total_amount         NUMERIC(14,2) NOT NULL,
    sale_date            TIMESTAMPTZ   NOT NULL,
    marketplace_order_id VARCHAR(100),
    source               VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',
    status               VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_sales_org_date ON sales(organization_id, sale_date);
CREATE INDEX idx_sales_org_channel ON sales(organization_id, channel_id);
CREATE INDEX idx_sales_org_sku ON sales(organization_id, sku_id);
CREATE INDEX idx_sales_org_status ON sales(organization_id, status);
