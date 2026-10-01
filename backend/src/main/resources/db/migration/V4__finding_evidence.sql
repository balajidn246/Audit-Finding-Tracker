CREATE TABLE finding_evidence (
    id uuid PRIMARY KEY,
    finding_id bigint NOT NULL REFERENCES audit_findings(id),
    uploaded_by_id bigint NOT NULL REFERENCES users(id),
    original_name varchar(255) NOT NULL,
    media_type varchar(100) NOT NULL,
    size_bytes bigint NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    sha256 varchar(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_evidence_finding_created ON finding_evidence(finding_id, created_at);
