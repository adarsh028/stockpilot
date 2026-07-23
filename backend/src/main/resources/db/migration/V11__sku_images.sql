CREATE TABLE sku_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    sku_id          UUID NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    url             VARCHAR(500) NOT NULL,
    is_primary      BOOLEAN      NOT NULL DEFAULT false,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_sku_images_org ON sku_images(organization_id);
CREATE INDEX idx_sku_images_sku ON sku_images(sku_id);

-- At most one primary image per SKU.
CREATE UNIQUE INDEX uq_sku_images_primary ON sku_images(sku_id) WHERE is_primary;
