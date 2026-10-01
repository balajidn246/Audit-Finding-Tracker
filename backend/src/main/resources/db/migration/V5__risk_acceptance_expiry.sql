ALTER TABLE audit_findings ADD COLUMN risk_acceptance_expires_on date;
CREATE INDEX idx_findings_risk_acceptance_expiry ON audit_findings(status, risk_acceptance_expires_on)
    WHERE status = 'RISK_ACCEPTED' AND deleted = FALSE;
