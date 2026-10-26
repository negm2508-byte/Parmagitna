# PaperERP Enterprise — Staging User Acceptance Testing (UAT) Checklist

**Document ID:** `UAT-STAGING-2026-10-26-001`  
**Target Environment:** Staging Acceptance Cluster (`https://staging.papererp.example.com`)  
**Governing Roles:** Enterprise ERP Acceptance Lead, Senior QA Engineer, Security Auditor, Factory Plant Manager  
**Baseline Artifact:** `paper-erp-api-1.0.0-SNAPSHOT.jar` (SHA-256: `4B543B1170F6AF1D6CC8BDE97E8A32711B14817ADEB4097336FACA70123E7821`)  
**Evidence Policy:** Never include passwords, raw JWT tokens, or secret values in this log.

---

## 1. UAT Test Execution & Evidence Log

| TC ID | Test Domain & Workflow Description | Test Account Role | Test Data Identifier | Expected Business Result | Actual Observed Result | Timestamp (UTC) | Verdict |
| :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: |
| **UAT-01** | **Authentication & Role-Based Access:** Verify that a Production Operator cannot access administrative user management endpoints. | `PRODUCTION_OPERATOR` | `operator.uat@mill.com` | Access denied with HTTP 403 `FORBIDDEN`. Error response formatted as `ApiErrorResponse`. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-02** | **Account Brute-Force Lockout:** Send 5 consecutive invalid login attempts for target operator account. | `ANONYMOUS` | `user:target.operator` | 5th attempt fails; 6th attempt rejected with HTTP 429 `ACCOUNT_LOCKED_OUT` (15m window). | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-03** | **Anti-Credential Stuffing Defense:** Verify successful login from an IP with 20 failures does not clear the IP rate-limiting defense. | `PRODUCTION_OPERATOR` | IP: `198.51.100.25` | Account counter cleared; IP remains blocked for remaining TTL window with HTTP 429 `IP_LOCKED_OUT`. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-04** | **Multi-Tenant Isolation (Read):** Tenant A operator queries list of active production machines. | `PRODUCTION_OPERATOR` | Tenant: `Tenant-A` | Only machines belonging to Tenant A are returned. Zero records from Tenant B are visible. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-05** | **Multi-Tenant Isolation (Write):** Attempt to update a roll belonging to Tenant B using Tenant A credentials. | `PRODUCTION_OPERATOR` | Roll: `SR-TENANT-B-001` | Request rejected with HTTP 404 / 403; row remains completely unchanged in database. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-06** | **Jumbo Roll Registration:** Register new paper mill jumbo roll from paper machine. | `PRODUCTION_OPERATOR` | Roll: `JR-UAT-20261026-01` Gross: 3050kg, Tare: 50kg, Net: 3000kg | Roll created with status `AVAILABLE`, barcode assigned, and net weight calculated as 3000.00 kg. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-07** | **Atomic Slitting & Mass Balance:** Execute slitting run producing 3 child rolls (1245kg, 1245kg, 435kg) + 65kg trim scrap. | `PRODUCTION_OPERATOR` | Input: 3000kg Net Outputs: 2925kg + 65kg | Scale variance is -10kg (-0.33%), within 0.5% tolerance. Parent transitions to `FULLY_SLIT`. 3 child slit rolls created. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-08** | **Tolerance Breach Rejection:** Attempt slitting run where output weight + scrap deviates by > 0.5% from parent net weight. | `PRODUCTION_OPERATOR` | Input: 3000kg Outputs: 2700kg + 50kg (-250kg / -8.3% variance) | Slitting rejected with HTTP 400 `MASS_BALANCE_TOLERANCE_EXCEEDED`. Zero child rolls created; parent roll remains `AVAILABLE`. | Awaiting Staging Deployment | **BLOCKED** |
| **UAT-09** | **Maker-Checker QC Gate Violation:** Operator who performed the slitting run attempts to approve QC inspection for child roll. | `PRODUCTION_OPERATOR` (Creator) | Roll: `SR-UAT-CHILD-01` | Inspection submission strictly rejected with HTTP 403 `MAKER_CHECKER_VIOLATION`. Roll remains in `PENDING` quality status. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-10** | **Independent Quality Release:** Dedicated QC Inspector tests moisture, burst strength, and approves roll. | `QC_INSPECTOR` (Independent) | Roll: `SR-UAT-CHILD-01` | Inspection recorded; roll quality disposition transitions to `RELEASED`. Audit log entry created. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-11** | **Warehouse Location Transfer:** Transfer released roll from slitting bay to Warehouse Rack B-04. | `WAREHOUSE_KEEPER` | Roll: `SR-UAT-CHILD-01` Target: `LOC-B-04` | Roll location updated; movement record created in `inventory_ledger` with movement type `TRANSFER`. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-12** | **Dispatch Quality Gate Enforcement:** Attempt to dispatch child roll while quality disposition is `PENDING` or `REJECTED`. | `WAREHOUSE_KEEPER` | Roll: `SR-UAT-CHILD-02` (Pending QC) | Dispatch rejected with HTTP 400 `INELIGIBLE_ROLL_DISPOSITION`. Roll status remains `AVAILABLE`. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-13** | **Customer Dispatch Execution:** Dispatch released child roll against customer purchase order. | `WAREHOUSE_KEEPER` | Roll: `SR-UAT-CHILD-01` Order: `PO-EGY-2026-99` | Roll status transitions to `DISPATCHED`; dispatch record added to `inventory_ledger`. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-14** | **Stock Movement Journal Immutability:** Verify that `inventory_ledger` records cannot be updated or deleted via API. | `ADMIN` | Ledger Entry: `LEDGER-001` | Any update or delete attempt returns HTTP 405 Method Not Allowed or is rejected by database constraints. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-15** | **Idempotency Under Duplicate Retries:** Resend identical execute slitting request with same `Idempotency-Key` header. | `PRODUCTION_OPERATOR` | Key: `IDEMP-UAT-SLIT-001` | Second request returns cached HTTP 200 response immediately without creating duplicate rolls or ledger records. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-16** | **Transactional Outbox Event Publishing:** Verify that production and slitting domain events are recorded in `outbox_events`. | `SYSTEM_WORKER` | Aggregate: `SLITTING_RUN` | Outbox event created in same database transaction; background worker claims lease (`locked_at`) and marks `PUBLISHED`. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-17** | **Redis Fail-Closed Outage Test:** Simulate Redis downtime during authentication attempt. | `ANONYMOUS` | Endpoint: `/api/v1/auth/login` | Service safely rejects requests with HTTP 503 `RATE_LIMITER_UNAVAILABLE`; fails closed to protect against credential stuffing. | Awaiting Staging Deployment | — | **BLOCKED** |
| **UAT-18** | **Application Container Restart Recovery:** Force restart backend pod; verify active sessions and database consistency. | `DEVOPS_LEAD` | Pod: `papererp-backend-*` | Pod restarts cleanly; passes readiness probe within 45s; existing user refresh tokens and database integrity intact. | Awaiting Staging Deployment | — | **BLOCKED** |

---

## 2. UAT Summary & Acceptance Sign-Off

```
========================================================================================
                          PAPERERP ENTERPRISE UAT STATUS
========================================================================================
  TOTAL TEST CASES SPECIFIED: ........................................... 18
  CASES PASSED ON LOCAL VERIFICATION: ................................... 18 (Automated)
  CASES EXECUTED ON LIVE STAGING CLUSTER: ................................ 0
  CURRENT STAGING UAT VERDICT: .......................................... BLOCKED
========================================================================================
```

### Sign-Off Attestation
- **Factory Acceptance Lead:** `Awaiting Staging Cluster Deployment & Frontend Client Access`
- **Senior QA Engineer:** `Test Cases Specified & Ready for Execution`
- **Security Auditor:** `Security & Quality Gates Fully Defined`
