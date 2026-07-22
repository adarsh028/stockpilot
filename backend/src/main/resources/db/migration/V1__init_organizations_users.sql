CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE organizations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(80)  NOT NULL UNIQUE,
    plan        VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'STAFF',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone)
);

CREATE INDEX idx_users_org ON users(organization_id);
