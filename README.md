# VerityOps

**Enterprise Audit, Risk & Remediation Management Platform**

VerityOps is a self-hosted application for documenting control findings, assigning remediation ownership, tracking work through independent validation, and preserving an append-only activity history. It is designed for internal audit, security assurance, and compliance teams that need a clear record of issue ownership and disposition.

> Deployment status: development release. The source includes a hardened local Docker Compose baseline and security controls, but a production launch still requires an organization-specific security assessment, external TLS ingress, configured backups and restore drills, scanner freshness monitoring, alerting, and operational ownership. Do not put regulated or sensitive organizational data into an unreviewed deployment.

## Current capabilities

- Spring Boot API with Java 25 LTS, PostgreSQL, Flyway migrations, Redis-backed authentication throttling, and OpenAPI documentation.
- React and TypeScript workspace with sign-in, findings register, status filters, dashboard counts, new finding form, owner assignment, detail view, remediation transitions, risk-acceptance reason, and audit-history display.
- Role-based API access (`ADMIN`, `MANAGER`, `VIEWER`), safe user summaries, database-authoritative roles, bootstrap administrator creation, and viewer-only self-registration when explicitly enabled (disabled by default).
- Short-lived bearer access tokens held in memory; rotating refresh tokens are stored only as hashes in PostgreSQL and delivered through an HttpOnly, SameSite cookie. Logout revokes the current refresh session.
- Finding changes and status transitions are recorded as audit events in the same database transaction; database trigger prevents updating or deleting audit rows.
- OWASP Top 10:2025-oriented security baseline, security headers, strict CORS configuration, request validation, rate limiting, private container networks, non-root runtime images, and secret-based production configuration.
- Evidence upload for PDF/PNG/JPEG with strict size/signature checks, ClamAV scanning (fail closed when unavailable), private generated-name storage, SHA-256 metadata, restricted download, and finding-history events. Finding closure requires clean evidence and an independent reviewer.
- Risk acceptances require a reason and future expiry date; a scheduled job reopens expired acceptances and records a system audit event.
- Optional owner email notifications use a database outbox written in the same transaction as finding changes, with deduplication, bounded exponential retry, and terminal failure state. SMTP remains disabled until explicitly configured.
- Manager-only CSV register export with filter support, spreadsheet-formula injection mitigation, bounded output, and export audit events.
- Health and Prometheus metrics endpoints on the backend. Public frontend proxy blocks actuator access.

## Workflow and roles

1. An administrator bootstraps the first account and provisions team accounts.
2. An administrator or manager creates a finding with severity, source, business unit, due date, and owner.
3. The owner moves the finding into remediation and requests validation.
4. A manager or administrator validates and closes it, sends it back for rework, or records a risk acceptance reason.
5. Finding changes and actor identity are available in the history view.

API authorization is the enforcement boundary; frontend visibility is not treated as authorization. A finding owner can update their remediation response and permitted status, while management-only details remain restricted.

## Technology choices

- **Backend:** Java 25 LTS with Spring Boot 3.5. Chosen for long-term support, mature security and transactional tooling, broad hiring availability, and stable enterprise operations.
- **Frontend:** TypeScript + React. Types catch API/model drift, while React has a large ecosystem and long-lived browser support.
- **Data:** PostgreSQL is the system of record; Redis supports shared rate-limit counters.
- **AI:** AI is intentionally disabled and not in the authorization, evidence, or remediation path. Consider AI only for opt-in summarization after privacy review, with human verification and no automatic disposition.

## Run locally with Docker Compose

Prerequisites: Docker Desktop/Engine with Compose v2, 4 GB RAM, and a modern browser.

1. Copy `.env.example` to `.env`.
2. Replace `DB_PASSWORD`, `REDIS_PASSWORD`, and `JWT_SECRET` with unique secrets. Set the bootstrap admin username, email, and password; use at least 16 characters for that password.
3. Run `docker compose up --build -d`.
4. Open `http://localhost:3000` and sign in using the bootstrap account.
5. Check service status with `docker compose ps` and backend health at `http://localhost:3000/healthz`.

The Postgres and Redis ports are not published to the host. Their data persists in named volumes. The first administrator is created only when the database contains no users; changing bootstrap environment values later does not alter existing accounts. The supplied `.env.example` credentials are local-development values and must never be used for a shared deployment.

### Frontend development mode

Run the backend and its dependencies, then in `frontend/` run `npm ci` followed by `npm run dev`. Vite proxies `/api` to `http://localhost:8080`. The normal build command runs TypeScript checks and produces static assets.

## Automated verification

GitHub Actions runs `mvn verify` with Java 25, checks the frontend with Node 22, `npm audit`, TypeScript, and a production Vite build, and validates the Docker Compose model on pushes and pull requests to `main`. Run the same commands locally from `backend/` and `frontend/` before release.

## Configuration

| Variable | Purpose |
| --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Shared login/register/refresh throttling |
| `JWT_SECRET` | Signing secret; at least 32 bytes of unpredictable material |
| `INITIAL_ADMIN_USERNAME`, `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD` | First account only, on an empty database |
| `REFRESH_COOKIE_SECURE` | Set `true` behind HTTPS; keep `false` only for local HTTP development |
| `SECURITY_REGISTRATION_ENABLED` | Public viewer registration; disabled unless explicitly enabled |
| `CORS_ALLOWED_ORIGINS` | Exact allowed browser origins; no wildcard origins |
| `DB_POOL_MAX`, `DB_POOL_MIN` | Hikari connection pool bounds |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP delivery for owner notifications when enabled |
| `FILES_UPLOAD_DIR` | Private evidence-storage path (uploads are not exposed through a public static route) |
| `NOTIFICATIONS_ENABLED` | Set `true` after configuring authenticated SMTP to enable owner notification delivery |
| `NOTIFICATIONS_FROM`, `APP_PUBLIC_URL` | Verified sender address and canonical app URL used in notifications |

## API overview

- `POST /api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`
- `GET /api/findings`, `GET /api/findings/{uuid}`, `POST /api/findings`, `PUT /api/findings/{uuid}`
- `PATCH /api/findings/{uuid}/status`, `GET /api/findings/{uuid}/history`, `GET /api/findings/stats`
- `GET /api/findings/report.csv` (manager/admin export), `/api/findings/{uuid}/evidence` (authorized upload/list/download)
- `GET/POST /api/admin/users`
- `GET /api/admin/notifications` and `POST /api/admin/notifications/{id}/retry` (delivery health and failed-message retry)
- OpenAPI UI at `/swagger-ui.html` and API spec at `/api-docs` on the private backend service; do not expose them publicly without an authenticated gateway

## Security and threat model

See [`docs/security/SECURITY-BASELINE.md`](docs/security/SECURITY-BASELINE.md) for the OWASP Top 10:2025 mapping, ASVS/WSTG verification references, and an ATT&CK-informed threat model. OWASP categories guide web-app controls; MITRE ATT&CK describes adversary behavior and is used to reason about realistic threats, not as a compliance certification.

Report suspected vulnerabilities using the private process described in [`SECURITY.md`](SECURITY.md). Never include real customer evidence or credentials in issue reports.

## Operations and recovery

Before a production deployment, configure infrastructure TLS, external secret management, durable encrypted backups, point-in-time recovery, restore drills, log retention, Prometheus scraping and alert rules, dependency/container scanning, and an incident owner. Keep Postgres and Redis private. Restrict and encrypt the evidence volume. Rotate secrets through a documented change procedure.

The Compose volumes are persistence, not backups. See [`docs/operations/RECOVERY.md`](docs/operations/RECOVERY.md) for the backup/restore runbook and the pre-production checks. A restore must be tested on an isolated environment before production use.

## Product gaps to close before general availability

- Scheduled due-date reminders, a full notification management screen, and SSO/MFA are planned integrations, not current capabilities.
- Define customer/tenant isolation, configurable retention and legal hold, and administrator role-change audit events before multi-team or multi-customer deployment.
- Configure deployment-specific metrics dashboards/alerts, structured log collection, SBOM generation, automated dependency/container scanning, and a restore exercise.
- Complete independent penetration testing and an ASVS verification pass against the target hosting environment.

## License

No open-source license is declared. Obtain authorization from the repository owner before redistributing or offering the source as a hosted service.
