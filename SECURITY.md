# Security policy

## Reporting

Do not report vulnerabilities in public issues. Contact the repository owner privately with a concise reproduction, affected commit/version, impact, and suggested mitigation. Do not include real audit records, evidence files, secrets, or personal data. Coordinate disclosure and remediation timing with the maintainer.

## Security posture

The security architecture, OWASP Top 10:2025 mapping, and ATT&CK-informed threat scenarios are documented in [`docs/security/SECURITY-BASELINE.md`](docs/security/SECURITY-BASELINE.md). Operational backup and recovery expectations are in [`docs/operations/RECOVERY.md`](docs/operations/RECOVERY.md).

The application is a development release and has not been independently penetration-tested or certified. Production operators must configure TLS, secret management, encrypted off-host backups, monitoring and alerting, dependency/image scanning, log retention, malware-signature freshness, and a tested recovery process. See the product gaps and release gate in the README and security baseline before storing organizational data.
