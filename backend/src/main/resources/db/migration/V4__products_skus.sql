CREATE TABLE products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    category        VARCHAR(100),
    brand_name      VARCHAR(100),
    description     TEXT,
    image_url       VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_org ON products(organization_id);
CREATE INDEX idx_products_org_name ON products(organization_id, name);

CREATE TABLE skus (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    product_id      UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku             VARCHAR(100) NOT NULL,
    attributes      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    cost_price      NUMERIC(12,2) NOT NULL DEFAULT 0,
    selling_price   NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_skus_org_sku UNIQUE (organization_id, sku)
);

CREATE INDEX idx_skus_org ON skus(organization_id);
CREATE INDEX idx_skus_product ON skus(product_id);
