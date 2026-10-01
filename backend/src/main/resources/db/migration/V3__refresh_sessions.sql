CREATE TABLE refresh_sessions (
    token_hash varchar(64) PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    revoked_at timestamptz,
    version bigint NOT NULL DEFAULT 0
);
CREATE INDEX idx_refresh_sessions_user ON refresh_sessions(user_id);
CREATE INDEX idx_refresh_sessions_expiry ON refresh_sessions(expires_at);
CREATE INDEX idx_refresh_sessions_active ON refresh_sessions(user_id) WHERE revoked_at IS NULL;
