# SECURITY.md

Threat model
- Users: ADMIN, MANAGER, VIEWER
- Assets: Audit findings, attachments, user credentials, AI documents, reports
- Adversaries: Malicious users, external attackers, supply-chain attacks

Top vulnerabilities and mitigations
- Injection (SQL/OS/LDAP): Use parameterized queries, JPA with parameters, input validation, ORM
- Broken Authentication: JWT with secure signing, short-lived access tokens, refresh tokens, rotate secrets
- Sensitive Data Exposure: Encrypt secrets at rest, use TLS, do not store tokens in localStorage in production
- XSS: Escape output, set Content-Security-Policy headers, use frameworks that auto-escape
- CSRF: APIs are stateless and use Authorization header for JWT; avoid storing tokens in cookies
- Insecure Deserialization: Do not trust serialized inputs, validate types, use safe parsers
- Using Components with Known Vulnerabilities: Keep dependencies updated
- Insufficient Logging & Monitoring: Audit logs, alerts for suspicious activity

OWASP Mapping
- A1 Injection -> SQL prepared statements, JPA
- A2 Broken Authentication -> JWT best practices
- A3 Sensitive Data Exposure -> TLS, encryption
- A7 XSS -> CSP, escaping
- A9 Using Components with Known Vulnerabilities -> Dependency management

Testing checklist
- Automated scans (Snyk, Dependabot) enabled
- Unit/integration tests for auth and RBAC
- Fuzzing inputs to AI endpoints
- Manual pen-test on login, file upload, and AI endpoints

