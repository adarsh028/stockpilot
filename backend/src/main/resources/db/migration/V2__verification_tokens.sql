CREATE TABLE verification_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code        VARCHAR(6)  NOT NULL,
    purpose     VARCHAR(30) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_vtoken_user_purpose ON verification_tokens(user_id, purpose);
CREATE INDEX idx_vtoken_expires ON verification_tokens(expires_at);
