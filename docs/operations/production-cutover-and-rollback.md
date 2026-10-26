# PaperERP Enterprise — Production Cutover & Rollback Runbook

**Document ID:** `OPS-PROD-CUTOVER-001`  
**Governing Roles:** Release Manager, Lead Database Architect, Application Security Engineer, Enterprise ERP Acceptance Lead  
**Target Application:** PaperERP Enterprise Backend (Spring Boot 3.4.3 / Java 17 / PostgreSQL 16 / Redis 7)  
**Artifact Baseline:** `paper-erp-api-1.0.0-SNAPSHOT.jar` (SHA-256: `4B543B1170F6AF1D6CC8BDE97E8A32711B14817ADEB4097336FACA70123E7821`)  

---

## 1. Architectural Pre-Requisites & Zero-Trust Boundary

### 1.1 Reverse Proxy & Untrusted Header Defense
- **The Threat:** Clients connecting to public endpoints can forge the `X-Forwarded-For` header (e.g. `X-Forwarded-For: 203.0.113.195`). If an application blindly accepts this header, attackers can bypass IP-level rate limiting by altering the header on every login attempt.
- **Enforced Topology:**
  1. The edge reverse proxy (Cloudflare, AWS ALB, or Ingress NGINX) **MUST** be configured to strip any incoming client `X-Forwarded-For` and overwrite it with the true client socket `$remote_addr`.
  2. Direct network access to application backend ports (e.g. port 8080) bypassing the ingress proxy must be strictly blocked via Kubernetes NetworkPolicies or Cloud VPC Security Groups.
  3. `application-prod.yml` enforces Tomcat's `RemoteIpValve` with `server.tomcat.internal-proxies` restricting trusted upstream proxies to internal subnets.

### 1.2 Database Least-Privilege & Connection Security
- The production runtime user **MUST** be `paper_erp_app_role`.
- `paper_erp_app_role` must **NOT** possess `SUPERUSER`, `CREATEDB`, `CREATEROLE`, or `BYPASSRLS`.
- `FORCE ROW LEVEL SECURITY` must be active on all business tables so that even table owners cannot bypass tenant isolation policies.
- Connection string must enforce TLS: `sslmode=require` or `sslmode=verify-full`.
- HikariCP pool is tuned for transaction pooling (PgBouncer mode): `minimum-idle: 10`, `maximum-pool-size: 50`.

### 1.3 Redis Reliability & Failure Policy
- Redis connection must enforce TLS (`REDIS_SSL_ENABLED=true`).
- `RATE_LIMITING_REDIS_FAILURE_MODE` must be configured strictly as `fail-closed` in production.
- If Redis becomes unreachable, authentication requests return HTTP 503 `RATE_LIMITER_UNAVAILABLE` to prevent brute-force attacks during infrastructure outages.

---

## 2. Production Cutover Procedure

### Step 1: Pre-Cutover Freeze Window (T-30 Minutes)
1. Announce a scheduled 30-minute maintenance window to mill operators and plant managers.
2. Terminate active user sessions via token revocation or ingress maintenance page.
3. Verify that the transactional outbox queue is fully drained:
   ```sql
   SELECT count(*) FROM outbox_events WHERE status IN ('PENDING', 'PROCESSING');
   -- Must return 0
   ```

### Step 2: Pre-Deployment Database Snapshot (T-15 Minutes)
Execute a consistent binary backup snapshot using `pg_dump`:
```bash
pg_dump -U postgres -h $PROD_DB_HOST -p 5432 -F c -b -v \
  -f "/secure/backups/paper_erp_pre_cutover_$(date +%Y%m%d_%H%M%S).dump" \
  $PROD_DB_NAME
```
- Verify the dump file exists, has a non-zero length, and its SHA-256 checksum is recorded.
- **Usage Boundary:** This snapshot is strictly intended for recovery if Flyway migration fails *before* traffic cutover.

### Step 3: Rolling Application Deployment (T-0)
1. Deploy the new backend container image across the cluster using Kubernetes rolling update strategy:
   ```yaml
   strategy:
     type: RollingUpdate
     rollingUpdate:
       maxSurge: 1
       maxUnavailable: 0
   ```
2. The first starting pod executes Flyway migrations automatically on startup (`flyway.enabled=true`, `hibernate.ddl-auto=validate`).
3. Verify Flyway migration logs:
   - `Successfully validated 3 migrations`
   - `Current version of schema "public": 3`

### Step 4: Post-Deployment Smoke Probes (T+5 Minutes)
1. Verify pod readiness probes succeed on `/actuator/health`.
2. Inspect log stream for startup errors:
   - `Redis rate-limiting backend verified at startup. Ping: PONG`
   - Zero `CRITICAL_SECURITY_EVENT` or `DomainException` entries.
3. Test break-glass administrator login via single authenticated request.
4. Switch ingress traffic to new pods and disable maintenance mode.

---

## 3. Database Migration & Rollback Architecture

### 3.1 The Catastrophic Risk of Full Snapshot Restore Post-Cutover
> [!CAUTION]
> **Severe Data Loss Warning:** If the new release is deployed, traffic is restored, and production operations commence (e.g. jumbo rolls are weighed, slitting runs executed, and dispatch movements written to the ledger), **RESTORING A PRE-CUTOVER BACKUP WILL CATASTROPHICALLY WIPE OUT ALL SUBSEQUENT PRODUCTION TRANSACTIONS.**
> 
> In a manufacturing ERP, restoring a pre-cutover backup orphans physical rolls, invalidates printed barcode labels, and creates irreconcilable inventory discrepancies with the plant floor.

### 3.2 Enforced Strategy: Expand / Contract (Backward-Compatible) Migrations
To ensure safe rollback without data destruction, PaperERP enforces the **Expand/Contract Pattern**:

1. **All Migrations Must Be Strictly Backward-Compatible:**
   - Columns added to existing tables must be `NULL` or provide safe `DEFAULT` values.
   - Constraints, indexes, and new tables (e.g. `V3__hardening_and_keyset_index.sql`) must not break the prior version of the backend application.
   - Columns or tables are never dropped in the same release that introduces their deprecation.
2. **Application Rollback (Zero Data Loss):**
   - If a software defect or unexpected latency spike is detected post-cutover, execute an **Application-First Rollback**:
     ```bash
     kubectl rollout undo deployment/paper-erp-backend
     ```
   - The prior version of the application immediately resumes processing traffic against the updated database schema without errors, because all schema modifications in V1, V2, and V3 are strictly additive and backward-compatible.
   - **Zero business data is lost.**

### 3.3 Emergency Disaster Recovery Rollback Runbook (Pre-Traffic Only)
Full database restoration is **only** permitted if Flyway migration encounters an unrecoverable failure *during Step 3, before customer traffic is enabled*:

1. Immediately halt deployment:
   ```bash
   kubectl rollout pause deployment/paper-erp-backend
   ```
2. Restore database from pre-cutover snapshot:
   ```bash
   pg_restore -U postgres -h $PROD_DB_HOST -p 5432 --clean --if-exists --no-owner --no-privileges \
     -d $PROD_DB_NAME "/secure/backups/paper_erp_pre_cutover_<timestamp>.dump"
   ```
3. Re-verify table counts, schema integrity, and RLS policies.
4. Roll back application image to previous stable release tag.
5. Abort maintenance window and notify plant management.

---

## 4. Production Release Governance Matrix

```
+-----------------------------------------------------------------------------------+
|                        PRODUCTION GO-LIVE READINESS GATES                         |
+-----------------------------------------------------------------------------------+
| 1. Codebase & Security Verification ................................... PASS      |
| 2. Automated Test Suite (54/54 Tests) ................................. PASS      |
| 3. Local PostgreSQL RLS & DR Drill Verification ....................... PASS      |
| 4. Distributed Redis Rate Limiting (Atomic Lua) ....................... PASS      |
| 5. Staging Environment Acceptance Sign-Off ............................ BLOCKED   |
| 6. Production Ingress Zero-Trust Header Sanitization .................. BLOCKED   |
| 7. Production KMS Secret Injection .................................... BLOCKED   |
+-----------------------------------------------------------------------------------+
| FINAL PRODUCTION RELEASE STATUS: PARTIAL / BLOCKED ON EXTERNAL STAGING GATES      |
+-----------------------------------------------------------------------------------+
```

### Accountable Production Sign-Offs Required
- [ ] **Release Manager:** Staging UAT Completion Sign-Off
- [ ] **Lead Database Administrator:** Production Snapshot & Migration Approval
- [ ] **Application Security Lead:** Zero-Trust Header Sanitization & Secret Injection Verification
- [ ] **Enterprise ERP Acceptance Lead:** Factory Acceptance & Operations Authorization
