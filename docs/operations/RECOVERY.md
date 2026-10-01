# VerityOps backup and recovery runbook

This runbook is a baseline for a single-host Compose installation. Production operators must adapt it to their database platform, retention obligations, RPO/RTO, encryption keys, and incident process. Named Docker volumes are persistence; they are not backups.

## Recovery objectives

Set and approve a recovery point objective (RPO), recovery time objective (RTO), backup retention, and evidence retention before onboarding records. Store database and evidence backups encrypted in a separate account or storage failure domain. Protect encryption keys separately from the backup objects.

## Backup

1. Put the application in a maintenance window or coordinate a consistent database snapshot with the object/evidence store. Record the backup timestamp and release identifier.
2. Create a PostgreSQL custom-format dump using a credential from the runtime secret store, not a command-line password:

   ```sh
   docker compose exec -T postgres pg_dump -U "$DB_USERNAME" -d "$DB_NAME" -Fc > verityops-$(date -u +%Y%m%dT%H%M%SZ).dump
   ```

3. Back up `/var/verityops/evidence` from the backend evidence volume. Preserve file identifiers and permissions. Back up PostgreSQL and evidence from a coordinated point so evidence metadata matches files.
4. Encrypt and upload both artifacts to the approved off-host backup location. Record checksums, encryption key reference, owner, timestamp, and restore-test date.
5. Confirm the backup job, transfer, and retention policy succeeded. Alert on missed or stale backups.

For a managed PostgreSQL deployment, use provider-managed encrypted backups and point-in-time recovery, plus separately protected evidence backups.

## Restore

1. Declare the recovery incident and identify the last known good database/evidence pair. Do not overwrite the source system while investigating.
2. Provision an isolated recovery environment with the same or compatible application release. Restore secrets through the approved secret manager; do not reuse development secrets.
3. Restore PostgreSQL into an empty database:

   ```sh
   docker compose exec -T postgres pg_restore --clean --if-exists -U "$DB_USERNAME" -d "$DB_NAME" < verityops-backup.dump
   ```

4. Restore evidence files to the configured private evidence path and verify permissions, count, and checksums against the backup manifest.
5. Start the application without public ingress. Confirm Flyway state, backend health, record counts, evidence references, login, finding history, evidence downloads, and metrics.
6. Have a second operator verify the recovery and record the actual RPO/RTO. Only then switch ingress or DNS according to the incident plan.
7. Preserve the affected system and relevant logs for investigation. Rotate credentials and signing keys if compromise is suspected; rotating a JWT key invalidates active tokens.

## Recovery exercise and operational controls

- Perform a restore exercise at least quarterly and after major storage or database changes.
- Keep a dated record of artifacts, checksum results, duration, failures, and remediation owner.
- Monitor PostgreSQL disk and connection capacity, evidence-volume capacity, Redis health, ClamAV definition age, and backup freshness.
- Monitor notification outbox backlog and terminal (`FAILED`) message count; SMTP delivery is at-least-once, so a timeout after provider acceptance can result in a duplicate notification.
- Keep logs and Prometheus metrics off-host with access controls, retention, and alerts. Do not log credentials, bearer tokens, or evidence contents.
- The Compose profile is a development/single-host baseline. Use managed high availability, encrypted storage, TLS ingress, external secret management, and an independently tested disaster-recovery design for production.
