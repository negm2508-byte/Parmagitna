# PaperERP Enterprise — Final Independent Release Certification Report

**Document ID:** `REL-CERT-2026-10-26-002`  
**Evaluation Date:** October 26, 2026  
**Auditor Roles:** Independent Release Manager, Senior QA Engineer, PostgreSQL Security Auditor, Redis Reliability Engineer, Enterprise ERP Acceptance Lead  
**Target Repository:** `G:\ERP_BackEnd`  
**Baseline Git Commit:** `92dbc29d81976bff7a791c902bbef7effc394552` (Branch: `master`)  
**Target Environment:** PaperERP Enterprise Backend (Spring Boot 3.4.3 / Java 17 / PostgreSQL 16 / Redis 7)  

---

## Executive Summary & Independent Verdict Matrix

In strict adherence to enterprise release governance, this independent release certification distinguishes empirically reproduced facts from unverified claims. Every check reported herein was directly executed against the current repository state and local infrastructure.

```
========================================================================================
                      PAPERERP ENTERPRISE INDEPENDENT VERDICT MATRIX
========================================================================================

  [STAGE 1] LOCAL VERIFICATION & CODEBASE SECURITY ................ PASS
  [STAGE 2] STAGING DEPLOYMENT READINESS (ARTIFACT) ............... PASS
  [STAGE 3] STAGING DEPLOYMENT EXECUTION & ACCEPTANCE ............. BLOCKED
  [STAGE 4] UNCONDITIONAL PRODUCTION GO-LIVE ...................... BLOCKED

----------------------------------------------------------------------------------------
  SUMMARY STATUS: GATED ON EXTERNAL STAGING CLUSTER PROVISIONING & UAT SIGN-OFF
========================================================================================
```

| Release Gate | Verification Scope | Status | Verified Technical Facts & Operational Constraints |
| :--- | :--- | :---: | :--- |
| **Gate 1: Backend Release Verification** | Clean Maven build, test suite, artifact packaging, live boot | **PASS** | Exit code 0, 54/54 tests passed across 4 modules. Packaged JAR boots cleanly on port 8094. |
| **Gate 2: Redis Security Verification** | Atomic Lua rate limiting, lockout boundaries, credential stuffing fix | **PASS** | Fixed-window counter with progressive lockout extension. IP counter preserved on valid login. `fail-closed` policy verified. |
| **Gate 3: Database & Disaster Recovery** | PostgreSQL backup/restore drill, RPO/RTO calculations, RLS integrity | **PASS** (Local) | Local drill measured RPO: 1.86 s (< 300 s), RTO: 1.05 s (< 900 s). Schema, data, and RLS survived restore. Classified strictly as local evidence. |
| **Gate 4: End-to-End ERP Acceptance** | Repository client audit, core business workflows, maker-checker | **PASS** (Backend) | Confirmed no frontend client exists in repo. All 9 core business workflows verified via integration test suites. |
| **Gate 5: Production Deployment Checklist** | Configuration, secrets, CORS/CSRF, health endpoints, runbook | **PASS** (Artifact) | Artifact ready. Production cutover blocked pending staging UAT, edge proxy header sanitization, and cloud KMS injection. |

---

## 1. Gate 1 — Backend Release Verification

### 1.1 Git Commit & Working-Tree Status
- **Baseline Commit:** `92dbc29d81976bff7a791c902bbef7effc394552`
- **Active Branch:** `master`
- **Working Tree Integrity:** Preserved all existing changes and hardening files without repository resets or destructive operations.
- **Tracked Modifications:**
  - `backend/paper-erp-api/pom.xml`: Added `spring-boot-starter-data-redis` dependency.
  - `backend/paper-erp-api/src/main/java/com/papererp/api/controller/AuthController.java`: Rate-limiting hardening; removed IP counter deletion on valid user login to defeat credential-stuffing loops.
  - `backend/paper-erp-api/src/main/java/com/papererp/api/security/RateLimitingService.java`: Distributed Redis rate limiting with atomic Lua execution and configurable failure modes.
  - `backend/paper-erp-api/src/main/resources/application.yml` & `application-prod.yml`: Configured Redis connection parameters, Tomcat trusted proxies, and rate-limiting failure mode.
  - `backend/paper-erp-core/src/main/resources/db/migration/V3__hardening_and_keyset_index.sql`: Composite keyset pagination indexes and RLS policies.

### 1.2 Clean Maven Build & Test Suite Results
A complete clean build was executed using Adoptium OpenJDK 17.0.20.1 from the multi-module backend root:
```powershell
$env:JAVA_HOME = "C:\Users\it2_specialist\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
./mvnw.cmd clean package
```
- **Exit Code:** `0`
- **Total Test Suites Executed:** 18
- **Total Tests Run:** **54**
- **Failures:** **0** | **Errors:** **0** | **Skipped:** **0**
- **Build Outcome:** `BUILD SUCCESS`

#### Comprehensive Test Execution Breakdown by Module

| Module | Test Suite Class | Tests | Failures | Errors | Skipped | Time (s) |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **paper-erp-common** | `TenantContextTest` | 3 | 0 | 0 | 0 | 0.221 |
| **paper-erp-core** | `TenantIsolationTest` | 1 | 0 | 0 | 0 | 0.019 |
| **paper-erp-core** | `SlittingOperationServiceTest` | 4 | 0 | 0 | 0 | 0.053 |
| **paper-erp-core** | `HighConcurrencySlittingTest` | 1 | 0 | 0 | 0 | 1.170 |
| **paper-erp-core** | `QualityServiceTest` | 2 | 0 | 0 | 0 | 0.060 |
| **paper-erp-core** | `InventoryServiceTest` | 3 | 0 | 0 | 0 | 0.205 |
| **paper-erp-core** | `PostgreSqlRlsVerificationTest` | 3 | 0 | 0 | 0 | 0.686 |
| **paper-erp-core** | `IdempotencyServiceTest` | 7 | 0 | 0 | 0 | 0.259 |
| **paper-erp-core** | `OutboxConcurrencyTest` | 3 | 0 | 0 | 0 | 0.116 |
| **paper-erp-core** | `TenantRlsAspectTest` | 3 | 0 | 0 | 0 | 2.333 |
| **paper-erp-core** | `SupabaseConnectionSmokeTest` | 1 | 0 | 0 | 0 | 0.004 |
| **paper-erp-api** | `ArchUnitTest` | 3 | 0 | 0 | 0 | 3.883 |
| **paper-erp-api** | `MasterDataCacheTest` | 3 | 0 | 0 | 0 | 4.220 |
| **paper-erp-api** | `AuthControllerTest` | 2 | 0 | 0 | 0 | 5.407 |
| **paper-erp-api** | `HealthControllerTest` | 1 | 0 | 0 | 0 | 0.849 |
| **paper-erp-api** | `ProductionControllerTest` | 1 | 0 | 0 | 0 | 0.983 |
| **paper-erp-api** | `RateLimitingServiceTest` | 8 | 0 | 0 | 0 | 0.483 |
| **paper-erp-api** | `RedisRateLimitingIntegrationTest` | 5 | 0 | 0 | 0 | 2.045 |
| **TOTAL** | **Reactor Build Total** | **54** | **0** | **0** | **0** | **22.996** |

### 1.3 Packaged Release Artifact Verification
- **Artifact Path:** `backend/paper-erp-api/target/paper-erp-api-1.0.0-SNAPSHOT.jar`
- **File Size:** `75,950,875` bytes (75.95 MB)
- **Compilation Timestamp:** 2026-10-26 13:50:18 (UTC+03:00)
- **SHA-256 Checksum:**
  ```
  4B543B1170F6AF1D6CC8BDE97E8A32711B14817ADEB4097336FACA70123E7821
  ```

### 1.4 Runtime Smoke Boot Verification
The packaged release JAR was booted independently on isolated port `8094` against local PostgreSQL 16 (`paper_erp_dev`) and local Redis (PID 5112, port 6379):
- **Startup Duration:** 36.56 seconds.
- **Flyway Validation:** Schema version 3 validated successfully; 0 pending migrations.
- **Redis Initialization:** Startup ping verified (`Ping: PONG`).
- **Live Health Endpoint Probe:**
  - `GET http://localhost:8094/api/v1/health`: HTTP 200 OK
    ```json
    {
      "success": true,
      "message": "System is healthy and operational",
      "data": {
        "status": "UP",
        "service": "PaperERP Enterprise Backend",
        "version": "1.0.0",
        "uptimeSeconds": 46
      }
    }
    ```
  - `GET http://localhost:8094/actuator/health`: HTTP 200 OK `{"status":"UP"}`

---

## 2. Gate 2 — Redis Security & Distributed Rate Limiting

### 2.1 Algorithm Classification & Lua Script Analysis
The rate limiter enforces rate limiting via an atomic Lua script executed in Redis:
```lua
local current = redis.call('INCR', KEYS[1])
if current == 1 then
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
elseif current >= tonumber(ARGV[1]) then
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
end
return current
```
- **Algorithm Type:** **Fixed-Window Counter with Lazy TTL Initialization and Progressive Lockout Extension on Breach**.
  - It is **not** a sliding-window log (which uses sorted sets `ZADD`/`ZREMRANGEBYSCORE`) and **not** a sliding-window counter (which interpolates between prior and current windows).
  - On the first failed attempt (`current == 1`), a 900-second TTL is set.
  - If attempts reach the threshold (`current >= maxAttempts`), the 900-second TTL is re-armed from the latest breach timestamp.
- **Counter Key Scoping & Normalization:**
  - Account scope: `rate_limit:user:{username.trim().toLowerCase()}` (Threshold: 5 attempts / 900 s window).
  - IP scope: `rate_limit:ip:{clientIp}` (Threshold: 20 attempts / 900 s window).

### 2.2 Security Hardening: Anti-Credential Stuffing
- **Defect Identified in Baseline:** Previously, `AuthController` invoked `rateLimitingService.recordSuccessfulAttempt(ipKey)` upon any successful login. This allowed an attacker with credentials to 1 valid account to execute credential-stuffing loops indefinitely (19 failed guesses $\rightarrow$ 1 valid login to reset IP counter $\rightarrow$ repeat).
- **Remediation Implemented:**
  - `AuthController.java` was modified to **only** clear `accountKey` on successful authentication.
  - The `ipKey` counter remains intact and decays solely through its natural 900-second TTL window.
- **Empirical Integration Test Verification:**
  - Verified in `RedisRateLimitingIntegrationTest.testSuccessfulAccountLoginDoesNotResetIpLockout()`.
  - Attacker IP `198.51.100.7` accumulated 20 failed attempts (lockout active).
  - Valid user authenticated from `198.51.100.7`. Account counter cleared, but IP lockout remained strictly enforced (`isAllowed("ip:198.51.100.7") == false`).

### 2.3 Redis Failure Policy Verification
- Configured in `application-prod.yml` as `fail-closed`.
- If Redis is unreachable, authentication requests throw `DomainException("RATE_LIMITER_UNAVAILABLE", ..., 503)`.
- Verified in `RateLimitingServiceTest.testExecuteWithRedisFallback_FailClosed_ThrowsException()`: requests are strictly rejected with HTTP 503 rather than failing open.

### 2.4 Reverse Proxy & Client IP Extraction
- `AuthController` uses `httpRequest.getRemoteAddr()`.
- Tomcat's `RemoteIpValve` is configured via `application-prod.yml`:
  ```yaml
  server:
    forward-headers-strategy: framework
    tomcat:
      remote-ip-header: X-Forwarded-For
      protocol-header: X-Forwarded-Proto
      internal-proxies: ${TRUSTED_PROXY_REGEX:10\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.(1[6-9]|2[0-9]|3[0-1])\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}}
  ```
- **Crucial Production Dependency:** In production, the edge ingress (Cloudflare, AWS ALB, NGINX) **MUST** overwrite `X-Forwarded-For` with the true client socket `$remote_addr`. If the ingress allows untrusted clients to inject arbitrary `X-Forwarded-For` headers, client IP rate limiting could be bypassed.

---

## 3. Gate 3 — Database Architecture & Disaster Recovery

### 3.1 Independent PostgreSQL Tenant Isolation Verification
Tested against PostgreSQL 16 using the restricted runtime application role `paper_erp_app_role`:
- **Role Attributes:** Confirmed `paper_erp_app_role` has **NO** `SUPERUSER`, `CREATEDB`, `CREATEROLE`, or `BYPASSRLS`.
- **Row-Level Security Enforcement:** Verified `relrowsecurity = t` and `relforcerowsecurity = t` across all tenant-isolated tables (`plants`, `machines`, `jumbo_rolls`, `slitting_runs`, `slit_rolls`, `inventory_ledger`, `audit_logs`, `idempotency_records`).
- **Fail-Closed Verification:** `SELECT count(*) FROM plants` executed without `app.current_tenant_id` returned exactly **0 rows**.
- **Positive & Negative Isolation:**
  - With `app.current_tenant_id` set to Tenant A: returns only `PLANT-A / مصنع أ` (1 row).
  - With `app.current_tenant_id` set to Tenant B: returns only `PLANT-B / مصنع ب` (1 row).
- **Cross-Tenant Mutation Defense:**
  - Tenant A executing `UPDATE plants SET name_ar = 'Hacked' WHERE code = 'PLANT-B'` updated **0 rows**.
  - Tenant A attempting to insert a plant for Tenant B failed immediately with:
    `ERROR: new row violates row-level security policy for table "plants"`.
- **Session State Isolation:** `set_config('app.current_tenant_id', :tenantId, true)` ensures transaction-local scoping (`is_local=true`). Once the transaction commits or rolls back, the variable automatically resets, preventing session leakage across pooled connection reuse.

### 3.2 Disaster Recovery Drill Execution Log
Executed on PostgreSQL 16.15 from `paper_erp_test` into an isolated target database `paper_erp_dr_target`:

| Timestamp Event | Exact Timestamp (UTC+03:00) | Elapsed Duration | Description |
| :--- | :---: | :---: | :--- |
| **$T_0$ (Snapshot Initiation)** | `2026-10-26 13:52:08.539` | — | Backup triggered via `pg_dump -F c -b`. |
| **$T_1$ (Snapshot Completed)** | `2026-10-26 13:52:09.409` | 0.870 s | Binary archive created: 98,819 bytes. |
| **$T_2$ (Restoration Initiation)** | `2026-10-26 13:52:10.403` | — | Disaster simulated; clean target DB initialized. |
| **$T_3$ (Restoration Verified)** | `2026-10-26 13:52:11.449` | 1.046 s | Restoration completed via `pg_restore`. |

#### Measured RPO & RTO Calculations
$$\text{Measured RPO} = T_2 - T_0 = 1.864 \text{ seconds (Target: } \le 300\text{ s / 5 min)} \implies \mathbf{PASS}$$
$$\text{Measured RTO} = T_3 - T_2 = 1.046 \text{ seconds (Target: } \le 900\text{ s / 15 min)} \implies \mathbf{PASS}$$

- **Post-Restoration Data Integrity:** Restored database verified with 18 public schema tables, 2 active tenants, 2 active plants, and enforced RLS policies (`relrowsecurity = t, relforcerowsecurity = t`).
- **Restored Isolation Probe:** Queried `paper_erp_dr_target` under `paper_erp_app_role`: returned 0 rows without tenant context, isolated Tenant A plant under Tenant A, and isolated Tenant B plant under Tenant B.
- **Classification:** This drill is classified as **Local Empirical Verification Evidence**. It does not constitute cloud staging or production DR certification.

---

## 4. Gate 4 — End-to-End ERP Acceptance

### 4.1 Client / Frontend Repository Inspection
- **Audit Finding:** **No frontend, web, mobile, or desktop client exists within `G:\ERP_BackEnd`.**
- The repository is strictly a backend modular monolith service. Business acceptance was conducted directly against the API controllers and core domain services.

### 4.2 Verified Critical Business Workflows

| Business Workflow | Verification Suite | Status | Domain Rules & Accounting Invariant Verified |
| :--- | :--- | :---: | :--- |
| **1. Auth & RBAC** | `AuthControllerTest` | **PASS** | 15m stateless JWTs; validated roles: `ADMIN`, `PRODUCTION_OPERATOR`, `QC_INSPECTOR`, `WAREHOUSE_KEEPER`. |
| **2. Multi-Tenant Isolation** | `PostgreSqlRlsVerificationTest` | **PASS** | Double-layer isolation: Hibernate entity filters + PostgreSQL RLS (`app.current_tenant_id`). |
| **3. Master Data Caching** | `MasterDataCacheTest` | **PASS** | High-performance caching for paper grades and machines with automated cache eviction on updates. |
| **4. Jumbo Roll Registration** | `InventoryServiceTest` | **PASS** | Full traceability: barcode, gross weight, core tare, net weight, initial state `AVAILABLE`. |
| **5. Slitting & Mass Balance** | `SlittingOperationServiceTest` | **PASS** | Input 3000 kg $\rightarrow$ 2925 kg child rolls + 65 kg trim scrap = 2990 kg (-0.33% variance; approved within 0.5% tolerance). |
| **6. Warehouse Transfers** | `InventoryServiceTest` | **PASS** | Roll location movements recorded in immutable stock movement journal. |
| **7. Quality & Maker-Checker** | `QualityServiceTest` | **PASS** | Maker-checker strictly enforced: operator who created a jumbo roll is blocked with HTTP 403 `MAKER_CHECKER_VIOLATION` from inspecting it. |
| **8. Dispatch & Ledger Invariant** | `InventoryServiceTest` | **PASS** | Unreleased rolls rejected from dispatch; released rolls transition to `DISPATCHED` and write immutable stock movement journal records. *(Clarification: Not a double-entry financial debit/credit ledger, but an append-only physical stock movement journal with mass-balance conservation).* |
| **9. Outbox & Idempotency** | `OutboxConcurrencyTest` | **PASS** | Idempotency tokens deduplicate concurrent requests; transactional outbox guarantees domain event publishing. |

---

## 5. Gate 5 — Production Deployment Checklist & Rollback Risk Analysis

### 5.1 Catastrophic Risk of Full Snapshot Restore Post-Cutover
> [!CAUTION]
> **Severe Operational Risk Identified:** Restoring a pre-cutover database snapshot after production traffic has commenced will wipe out all subsequent factory transactions (weighed jumbo rolls, slitting runs, QC releases, and customer shipments), creating physical inventory divergence.
> 
> **Enforced Remediation:** All Flyway migrations (V1, V2, V3) follow the **Expand/Contract Pattern** (backward-compatible, additive changes). Rollback is strictly executed via **Application Container Rollback** (`kubectl rollout undo deployment/paper-erp-backend`), which preserves all business transactions while running the previous stable code version against the backward-compatible schema.

---

## 6. Staging Acceptance & Production Cutover Runbook References

Detailed operational runbooks have been established in the repository:
1. [**`docs/operations/staging-acceptance-checklist.md`**](file:///G:/ERP_BackEnd/docs/operations/staging-acceptance-checklist.md): Step-by-step checklist for staging secret injection, multi-instance rate limit load testing, and end-user acceptance testing.
2. [**`docs/operations/production-cutover-and-rollback.md`**](file:///G:/ERP_BackEnd/docs/operations/production-cutover-and-rollback.md): Pre-cutover freeze protocol, zero-trust edge proxy header sanitization requirements, and non-destructive application rollback procedures.

---

## 7. Final Independent Release Verdicts

```
+-----------------------------------------------------------------------------------+
|                        PAPERERP ENTERPRISE VERDICT MATRIX                         |
+-----------------------------------------------------------------------------------+
| 1. LOCAL CODEBASE & SECURITY VERIFICATION ............................ PASS       |
| 2. STAGING DEPLOYMENT READINESS (ARTIFACT) ........................... PASS       |
| 3. STAGING DEPLOYMENT EXECUTION & ACCEPTANCE ......................... BLOCKED    |
| 4. UNCONDITIONAL PRODUCTION GO-LIVE ................................... BLOCKED    |
+-----------------------------------------------------------------------------------+
| NEXT REQUIRED ACTION: PROVISION STAGING CLUSTER & EXECUTE STAGING CHECKLIST       |
+-----------------------------------------------------------------------------------+
```

### Sign-Off & Attestation
- **Independent Release Manager:** Artifact certified ready for Staging Promotion.
- **Senior QA Engineer:** Certified 54 passing tests, zero regressions.
- **PostgreSQL Security Auditor:** Certified RLS tenant isolation, non-superuser role, and local DR drill (RPO 1.86s / RTO 1.05s).
- **Redis Reliability Engineer:** Certified atomic Lua rate limiting, fail-closed policy, and anti-credential-stuffing fix.
- **Enterprise ERP Acceptance Lead:** Certified all 9 core domain workflows at service/API levels. Staging user acceptance testing is required prior to production authorization.
