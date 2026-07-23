CREATE TABLE categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_categories_org_name UNIQUE (organization_id, name)
);

CREATE INDEX idx_categories_org ON categories(organization_id);

ALTER TABLE products DROP COLUMN category;
ALTER TABLE products ADD COLUMN category_id UUID REFERENCES categories(id);

CREATE INDEX idx_products_category ON products(category_id);
