# VerityOps Security Baseline

## Scope and interpretation

VerityOps stores internal audit findings, remediation plans, user identities, and evidence files. A confidentiality breach can expose sensitive control gaps; an integrity breach can falsify remediation status or audit history; and an availability failure can interrupt audit deadlines.

OWASP Top 10:2025 is the risk-awareness baseline. OWASP ASVS is the control-requirements reference, and the versioned OWASP Web Security Testing Guide is the security-assessment methodology. These references complement one another; the Top 10 alone is not a certification or a complete security checklist.

MITRE ATT&CK Enterprise is used to describe adversary behavior and defensive scenarios relevant to the application and its deployment. ATT&CK is not a secure-coding standard or a replacement for web-application testing.

## OWASP Top 10:2025 control map

| Category | VerityOps risks | Required engineering controls |
| --- | --- | --- |
| A01 Broken Access Control | A user reads another team's finding, assigns themselves elevated work, or closes their own finding | Authorize every object operation server-side; enforce least privilege and separation of duties; scope records to the organization; deny by default; log access-control changes |
| A02 Security Misconfiguration | Development secrets, debug endpoints, exposed database ports, permissive CORS, or unsafe proxy headers | Fail startup on missing production secrets; environment-specific configuration; secure headers; no public database/cache ports; explicit CORS and trusted-proxy configuration; hardened non-root containers |
| A03 Software Supply Chain Failures | Vulnerable or compromised dependencies, build tools, base images, or release artifacts | Pin dependencies and image digests for releases; automated dependency and image scanning; SBOM generation; provenance and protected release workflow; patch ownership and response SLAs |
| A04 Cryptographic Failures | Weak JWT keys, leaked tokens, unencrypted evidence, or unsafe key lifecycle | TLS at ingress; modern password hashing; high-entropy secrets in a secret manager; managed key rotation; encryption at rest for database and evidence storage; never log credentials or tokens |
| A05 Injection | Unsafe search filters, SQL, file names, report templates, or AI prompts | Parameterized persistence; strict DTO validation; output encoding; safe file handling; no shell interpolation; adversarial prompt content treated as untrusted data |
| A06 Insecure Design | Owner self-approval, evidence-free closure, indefinite risk acceptance, or unbounded uploads | Threat model and misuse cases; explicit state machine; owner/reviewer separation; closure evidence gate; time-bound risk acceptance; limits and abuse controls |
| A07 Authentication Failures | Privilege escalation at signup, credential stuffing, stolen refresh tokens, or stale permissions | Self-registration receives least privilege; MFA/SSO for organizations; login throttling and lockout policy; short access tokens; refresh-token rotation and revocation; load current account status and roles |
| A08 Software or Data Integrity Failures | Altered audit events, forged uploads, or untrusted releases | Append-only audit events; optimistic concurrency; verified content hashes; malware quarantine and scanning; signed/controlled build artifacts; validate integration webhook signatures |
| A09 Logging & Alerting Failures | No signal on repeated login failures, export spikes, role grants, or audit-log access | Structured security events; alerting thresholds; protected retention; log access and exports; correlation IDs; redact secrets and sensitive evidence content |
| A10 Mishandling of Exceptional Conditions | Stack traces or secrets in errors, partial workflow updates, or unsafe fail-open behavior | Stable Problem Details responses; transactional finding+audit writes; safe dependency timeouts; explicit fail-closed behavior for authorization; health/readiness checks that reveal no credentials |

## MITRE ATT&CK Enterprise scenarios

| ATT&CK behavior | VerityOps scenario | Defensive response to build and operate |
| --- | --- | --- |
| [T1190 Exploit Public-Facing Application](https://attack.mitre.org/techniques/T1190/) | Crafted requests target the API, reverse proxy, upload handler, or outdated dependency | Minimize exposed services; patch and scan; isolate containers; validate requests; correlate unusual request patterns with application errors and outbound connections |
| [T1110.001 Password Guessing](https://attack.mitre.org/techniques/T1110/001/) | Repeated login attempts target staff or administrator accounts | Rate-limit login by trusted client identity; monitor failures and success-after-failure; MFA; alert on distributed password spraying; avoid account-enumeration responses |
| [T1078 Valid Accounts](https://attack.mitre.org/techniques/T1078/) | A stolen user or administrator credential is used to access findings or exports | Least privilege; MFA/SSO; session and refresh revocation; anomaly alerts for new devices, unusual exports, and privilege changes; periodic access review |
| [T1567.002 Exfiltration to Cloud Storage](https://attack.mitre.org/techniques/T1567/002/) | An attacker with access exports or copies sensitive evidence to an external storage service | Fine-grained export permissions; audit and alert on bulk downloads; short-lived evidence URLs; outbound egress policy; customer-controlled retention and export review |

These scenarios are design and detection inputs. They do not claim ATT&CK coverage or protection against every technique. Revisit mappings when the hosting model, integrations, or ATT&CK Enterprise content changes.

## Release gate

Do not represent a release as production-ready until the deployed configuration, access boundaries, evidence flow, backups, alerting, and recovery procedure have been reviewed for the target environment. Keep a versioned security review record, unresolved-risk register, dependency inventory/SBOM, incident contacts, and restore evidence with each release.

## References

- [OWASP Top 10:2025](https://top10.owasp.org/2025/)
- [OWASP Application Security Verification Standard](https://owasp.org/www-project-application-security-verification-standard/)
- [OWASP Web Security Testing Guide v4.2](https://wstg.owasp.org/v4.2/)
- [OWASP Software Assurance Maturity Model](https://owasp.org/projects/samm)
- [MITRE ATT&CK Enterprise Matrix](https://attack.mitre.org/matrices/enterprise/)
