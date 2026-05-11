-- V1 initial schema: users and audit tables

CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(200) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  enabled BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT NOT NULL,
  role VARCHAR(100) NOT NULL,
  CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_findings (
  id BIGSERIAL PRIMARY KEY,
  uuid UUID NOT NULL DEFAULT gen_random_uuid(),
  title VARCHAR(1000) NOT NULL,
  description TEXT,
  severity VARCHAR(50),
  status VARCHAR(50),
  tags TEXT,
  created_by BIGINT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_audit_findings_uuid ON audit_findings(uuid);
CREATE INDEX IF NOT EXISTS idx_audit_findings_status ON audit_findings(status);
CREATE INDEX IF NOT EXISTS idx_audit_findings_severity ON audit_findings(severity);

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGSERIAL PRIMARY KEY,
  finding_id BIGINT,
  actor_id BIGINT,
  action VARCHAR(100),
  old_json TEXT,
  new_json TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);
