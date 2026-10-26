# PaperERP Enterprise — Staging Acceptance & Deployment Verification Checklist

**Document ID:** `CHK-STAGING-2026-10-26-001`  
**Target Environment:** Staging Acceptance Cluster (Isolated Kubernetes / VM Infrastructure)  
**Evaluator Roles:** Independent Release Manager, Senior QA Engineer, PostgreSQL Security Auditor, Enterprise ERP Acceptance Lead  
**Target Artifact:** `paper-erp-api-1.0.0-SNAPSHOT.jar` (SHA-256: `4B543B1170F6AF1D6CC8BDE97E8A32711B14817ADEB4097336FACA70123E7821`)  
**Baseline Commit:** `92dbc29d81976bff7a791c902bbef7effc394552`  

---

## 1. Governance & Execution Status Summary

| Gate / Phase | Scope | Status | Blocker / Justification |
| :--- | :--- | :---: | :--- |
| **Phase 1: Build & Artifact Readiness** | Local Maven clean build, test suite (54/54), JAR hash | **PASS** | Successfully verified on OpenJDK 17.0.20.1. |
| **Phase 2: Staging Secrets & Infrastructure** | Inject staging DB, Redis, JWT secrets, TLS validation | **BLOCKED** | Staging credentials and cloud access are not present in this workspace. |
| **Phase 3: Automated Staging Deployment** | Containerization, deployment pipeline execution | **BLOCKED** | Deployment pipeline / Docker orchestration tools absent in repo. |
| **Phase 4: Staging Database Migration** | Flyway V1 $\rightarrow$ V3 schema verification on staging DB | **BLOCKED** | Requires connectivity to isolated staging PostgreSQL instance. |
| **Phase 5: Staging Smoke & API Probes** | Health endpoints (`/actuator/health`), Redis ping, JWT auth | **BLOCKED** | Gated on Phase 3 deployment completion. |
| **Phase 6: End-to-End Business Acceptance** | Web frontend pilot workflows (Jumbo $\rightarrow$ Slit $\rightarrow$ QC $\rightarrow$ Dispatch) | **BLOCKED** | Gated on deployment and external frontend client connectivity. |

---

## 2. Phase 1: Artifact & Configuration Verification (Completed Locally)

- [x] **Compiler & JDK:** Eclipse Adoptium OpenJDK 17.0.20.1 verified.
- [x] **Maven Build:** `mvnw.cmd clean package` executed with exit code `0`.
- [x] **Test Results:** 54 passed, 0 failures, 0 errors, 0 skipped across all 4 modules.
- [x] **Artifact Packaging:** Spring Boot executable JAR verified (`75,950,875` bytes).
- [x] **Artifact Checksum:** `4B543B1170F6AF1D6CC8BDE97E8A32711B14817ADEB4097336FACA70123E7821`.
- [x] **Local Boot Smoke Test:** Tested against local PostgreSQL 16 and Redis; HTTP 200 on `/api/v1/health`.

---

## 3. Phase 2: Staging Infrastructure & Secret Provisioning (Blocked — Operator Checklist)

The designated Release Operations Engineer must inject the following environment variables into the staging runtime environment. **No secrets must ever be committed to source control or logged:**

```bash
# Staging PostgreSQL / Supabase Connection
export SUPABASE_DB_URL="jdbc:postgresql://<staging-db-host>:5432/<staging-db-name>?sslmode=require"
export SUPABASE_DB_USERNAME="paper_erp_app_role"
export SUPABASE_DB_PASSWORD="<SECURE_STAGING_PASSWORD>"

# Staging Redis Cluster / Instance
export REDIS_HOST="<staging-redis-host>"
export REDIS_PORT="6379"
export REDIS_PASSWORD="<SECURE_REDIS_PASSWORD>"
export REDIS_SSL_ENABLED="true"

# Security & Governance
export APP_JWT_SECRET="<SECURE_256BIT_STAGING_SECRET>"
export SPRING_PROFILES_ACTIVE="prod"
export RATE_LIMITING_REDIS_FAILURE_MODE="fail-closed"
export TRUSTED_PROXY_REGEX="10\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.(1[6-9]|2[0-9]|3[0-1])\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}"
```

- [ ] Verify that `SUPABASE_DB_USERNAME` uses `paper_erp_app_role` (non-superuser, no `BYPASSRLS`).
- [ ] Verify that Redis connection enforces TLS (`REDIS_SSL_ENABLED=true`).
- [ ] Confirm edge ingress proxy (e.g. NGINX Ingress, Cloudflare, AWS ALB) strictly overwrites `X-Forwarded-For` with `$remote_addr` before forwarding traffic to the application.

---

## 4. Phase 3: Staging Deployment Protocol

1. **Build Container Image:**
   ```bash
   docker build -t papererp-backend:1.0.0-staging -f backend/Dockerfile .
   ```
2. **Deploy to Staging Namespace:**
   Apply staging deployment manifest with 2 application replicas to verify distributed Redis rate limiting and shared cache behavior.
3. **Validate Pod Startup Logs:**
   - Confirm log line: `Redis rate-limiting backend verified at startup. Ping: PONG`.
   - Confirm Flyway schema validation: `Current version of schema "public": 3`.
   - Confirm Tomcat started on port 8080 with context path `/`.

---

## 5. Phase 4: Staging Database & Tenant Isolation Verification

Connect to the staging PostgreSQL database via `psql` using `paper_erp_app_role`:

```sql
-- 1. Fail-closed test (Must return 0 rows)
SELECT count(*) FROM plants;

-- 2. Tenant A Isolation test
BEGIN;
SELECT set_config('app.current_tenant_id', '<staging-tenant-a-uuid>', true);
SELECT code, name_ar FROM plants;
COMMIT;

-- 3. Cross-Tenant update defense test (Must update 0 rows)
BEGIN;
SELECT set_config('app.current_tenant_id', '<staging-tenant-a-uuid>', true);
UPDATE plants SET name_ar = 'Hacked' WHERE tenant_id = '<staging-tenant-b-uuid>';
COMMIT;
```
- [ ] Confirm `SELECT count(*)` without tenant context returns 0 rows.
- [ ] Confirm cross-tenant update updates 0 rows.
- [ ] Confirm cross-tenant insert is rejected with `ERROR: new row violates row-level security policy`.

---

## 6. Phase 5: Distributed Redis Rate Limiting Verification on Staging

Execute automated load tests against staging `/api/v1/auth/login` across the 2 backend replicas:

1. **Account Lockout Threshold:** Send 5 consecutive failed login attempts for `user:test_staging_operator`.
   - Verify 6th attempt returns HTTP 429 `ACCOUNT_LOCKED_OUT`.
2. **IP Lockout Threshold:** Send 20 failed login attempts from a test IP.
   - Verify 21st attempt returns HTTP 429 `IP_LOCKED_OUT`.
3. **Anti-Credential Stuffing Test:** Authenticate with a valid credential from the locked test IP.
   - Confirm user login succeeds for the valid account.
   - Confirm subsequent requests from the locked IP *still* return HTTP 429 `IP_LOCKED_OUT`.
4. **Redis Outage Simulation:** Temporarily isolate Redis or point to a non-existent port.
   - Verify authentication requests immediately return HTTP 503 `RATE_LIMITER_UNAVAILABLE` under `fail-closed` policy.
   - Confirm the system does NOT fall back to open access.

---

## 7. Phase 6: End-to-End Business Acceptance Workflows

Execute the following end-to-end manufacturing workflows via the staging frontend client or automated REST suite:

- [ ] **Workflow 1: Authentication & Role Enforcement:** Log in as Operator, QC Inspector, Warehouse Keeper, and Admin; verify HTTP 403 on restricted endpoints.
- [ ] **Workflow 2: Master Data Caching:** Query paper grades; verify cache hit on subsequent calls; update paper grade and verify cache eviction.
- [ ] **Workflow 3: Jumbo Roll Registration:** Register jumbo roll with barcode, net weight, gross weight, core tare; verify status `AVAILABLE`.
- [ ] **Workflow 4: Atomic Slitting & Mass Balance:** Execute slitting run with 3 child cuts and trim waste; verify parent jumbo transitions to `FULLY_SLIT`; verify scale variance is within configured tolerance ($\le 0.5\%$).
- [ ] **Workflow 5: Maker-Checker QC Gate:** Attempt to approve QC inspection of child roll as the operator who executed the slitting run; verify HTTP 403 `MAKER_CHECKER_VIOLATION`. Approve as dedicated QC Inspector; verify status transitions to `RELEASED`.
- [ ] **Workflow 6: Dispatch & Stock Movement Journal:** Attempt to dispatch unreleased roll (verify rejection). Dispatch released roll; verify status `DISPATCHED` and inspect `inventory_ledger` movement records.
- [ ] **Workflow 7: Idempotency:** Resend identical slitting request with same `Idempotency-Key`; verify HTTP 200 cached response returned without duplicate roll creation.

---

## 8. Staging Sign-Off Attestation

| Role | Name / Title | Sign-Off Date | Staging Verdict |
| :--- | :--- | :---: | :---: |
| **Release Manager** | Enterprise Release Manager | Pending UAT | `BLOCKED (Awaiting Staging Cluster)` |
| **Lead QA Engineer** | Senior QA Engineer | 2026-10-26 | `PASS (Artifact & Tests Verified)` |
| **Security Auditor** | PostgreSQL Security Auditor | 2026-10-26 | `PASS (RLS & Role Verified Locally)` |
| **ERP Acceptance Lead** | Enterprise ERP Acceptance Lead | Pending UAT | `BLOCKED (Awaiting Frontend UAT)` |
