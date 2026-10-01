-- Add remediation workflow fields without discarding the original findings data.
ALTER TABLE audit_findings
    ADD COLUMN root_cause TEXT,
    ADD COLUMN recommendation TEXT,
    ADD COLUMN management_response TEXT,
    ADD COLUMN source_audit VARCHAR(200),
    ADD COLUMN business_unit VARCHAR(160),
    ADD COLUMN owner_id BIGINT,
    ADD COLUMN due_date DATE,
    ADD COLUMN closed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE audit_findings SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE audit_findings
SET severity = CASE UPPER(COALESCE(severity, 'MEDIUM'))
    WHEN 'LOW' THEN 'LOW' WHEN 'MEDIUM' THEN 'MEDIUM'
    WHEN 'HIGH' THEN 'HIGH' WHEN 'CRITICAL' THEN 'CRITICAL'
    ELSE 'MEDIUM' END;
UPDATE audit_findings
SET status = CASE UPPER(COALESCE(status, 'OPEN'))
    WHEN 'OPEN' THEN 'OPEN' WHEN 'IN_PROGRESS' THEN 'IN_PROGRESS'
    WHEN 'PENDING_VALIDATION' THEN 'PENDING_VALIDATION'
    WHEN 'REWORK_REQUIRED' THEN 'REWORK_REQUIRED'
    WHEN 'CLOSED' THEN 'CLOSED' WHEN 'RISK_ACCEPTED' THEN 'RISK_ACCEPTED'
    ELSE 'OPEN' END;
UPDATE audit_findings SET deleted = FALSE WHERE deleted IS NULL;

ALTER TABLE audit_findings ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE audit_findings ALTER COLUMN severity SET DEFAULT 'MEDIUM';
ALTER TABLE audit_findings ALTER COLUMN severity SET NOT NULL;
ALTER TABLE audit_findings ALTER COLUMN status SET DEFAULT 'OPEN';
ALTER TABLE audit_findings ALTER COLUMN status SET NOT NULL;
ALTER TABLE audit_findings ALTER COLUMN deleted SET DEFAULT FALSE;
ALTER TABLE audit_findings ALTER COLUMN deleted SET NOT NULL;
ALTER TABLE audit_findings ADD CONSTRAINT uq_audit_findings_uuid UNIQUE (uuid);
ALTER TABLE audit_findings ADD CONSTRAINT fk_findings_owner FOREIGN KEY (owner_id) REFERENCES users(id);

CREATE INDEX idx_findings_owner_status ON audit_findings(owner_id, status);
CREATE INDEX idx_findings_status_updated ON audit_findings(status, updated_at);
CREATE INDEX idx_findings_due_date ON audit_findings(due_date);

ALTER TABLE audit_logs
    ADD COLUMN event_id UUID DEFAULT gen_random_uuid(),
    ADD COLUMN entity_type VARCHAR(50) DEFAULT 'FINDING',
    ADD COLUMN source_ip VARCHAR(64);

UPDATE audit_logs SET event_id = gen_random_uuid() WHERE event_id IS NULL;
UPDATE audit_logs SET entity_type = 'FINDING' WHERE entity_type IS NULL;
UPDATE audit_logs SET action = 'LEGACY_EVENT' WHERE action IS NULL;
UPDATE audit_logs SET created_at = now() WHERE created_at IS NULL;
ALTER TABLE audit_logs ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN entity_type SET NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN action SET NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE audit_logs ADD CONSTRAINT uq_audit_logs_event_id UNIQUE (event_id);
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_finding FOREIGN KEY (finding_id) REFERENCES audit_findings(id);
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users(id);
CREATE INDEX idx_audit_logs_finding_created ON audit_logs(finding_id, created_at);
CREATE INDEX idx_audit_logs_actor_created ON audit_logs(actor_id, created_at);

-- Application audit history is append-only. Corrections are recorded as new events.
CREATE FUNCTION reject_audit_log_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit log entries are append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_no_update_or_delete
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION reject_audit_log_mutation();
