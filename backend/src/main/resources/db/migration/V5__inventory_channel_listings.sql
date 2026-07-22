CREATE TABLE inventory_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    sku_id            UUID NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    quantity_on_hand  INTEGER NOT NULL DEFAULT 0,
    reorder_level     INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_org_sku UNIQUE (organization_id, sku_id)
);

CREATE INDEX idx_inventory_org ON inventory_items(organization_id);
CREATE INDEX idx_inventory_low_stock ON inventory_items(organization_id)
    WHERE quantity_on_hand <= reorder_level;

CREATE TABLE channel_listings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    channel_id          UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    sku_id              UUID NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    channel_sku         VARCHAR(100),
    allocated_quantity  INTEGER NOT NULL DEFAULT 0,
    channel_price       NUMERIC(12,2),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_listing_org_channel_sku UNIQUE (organization_id, channel_id, sku_id)
);

CREATE INDEX idx_listings_org ON channel_listings(organization_id);
CREATE INDEX idx_listings_sku ON channel_listings(sku_id);
CREATE INDEX idx_listings_channel ON channel_listings(channel_id);
