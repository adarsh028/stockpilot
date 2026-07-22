CREATE TABLE otp_request_log (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose      VARCHAR(30) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_otp_log_user_purpose_time ON otp_request_log(user_id, purpose, requested_at);
