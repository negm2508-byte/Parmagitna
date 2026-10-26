# PaperERP Enterprise — Threat Model & Security Posture

## 1. Threat Profile & Boundaries

PaperERP Enterprise operates in an industrial manufacturing environment with factory-floor terminals, mobile QC tablets, and management offices. The threat model accounts for hostile network environments, unauthorized cross-tenant data access, session hijacking, malicious parameter tampering, and concurrent race conditions.

| Threat Category | Potential Attack Vector | Applied Mitigation Strategy |
|---|---|---|
| **Cross-Tenant Breach** | An attacker injects or alters `tenant_id` in HTTP headers, URL params, or JSON payloads to query or mutate another company's records. | 1. `tenant_id` is derived exclusively from server-validated cryptographic tokens.<br>2. DB Composite Foreign Keys `(tenant_id, id)`.<br>3. PostgreSQL Row-Level Security (RLS) fails closed without valid session tenant.<br>4. ThreadLocal TenantContext cleanup on every request cycle. |
| **Cross-Plant Contamination** | An operator in Plant X issues operations or transfers targeting machinery or stock in Plant Y without cross-plant privileges. | Strict plant scope authorization checks in domain services and composite unique constraints matching tenant and plant scopes. |
| **Credential & Secret Exposure** | Production credentials or database passwords leaked in git, stack traces, or logs. | Zero hardcoded secrets, `.gitignore` enforcement, sanitized JSON error responses with non-disclosing error codes, masked log formatters. |
| **CSRF & Session Hijacking** | Malicious scripts on adjacent browser tabs trigger state-changing ERP requests. | HttpOnly, SameSite=Lax/Strict, Secure cookies combined with Spring Security double-submit CSRF cookie token verification (`X-XSRF-TOKEN`). |
| **Double-Slitting Race Conditions** | Competing concurrent requests simultaneously slit the same Jumbo Roll, multiplying inventory. | Strict Pessimistic Write Lock (`PESSIMISTIC_WRITE`) on parent roll in `SlittingOperationService` + unique idempotency operation keys. |
| **Ledger History Tampering** | Malicious user or compromised application service alters historical stock balance records. | `inventory_ledger` table is strictly append-only. No UPDATE or DELETE privileges for the application role. Compensating ledger entries only. |
| **Inventory State Divergence** | In-flight network failure leaves parent roll slit but child rolls or ledger unpersisted. | Full atomic database transactions (`@Transactional(rollbackFor = Exception.class)`) committing run, rolls, ledger, and waste simultaneously. |

## 2. Residual Risks & Deployment Assumptions

1. **Database Role Privileges**: PostgreSQL Row Level Security (RLS) policies are bypassed if the application connects as a database superuser or table owner. In production, Supabase / PostgreSQL runtime connections MUST use a restricted application user (e.g., `paper_erp_app_user`) while migrations run under `paper_erp_migration_user`.
2. **Factory Scale Precision**: Discrepancies between intake scales, crane scales, and slitting machine floor scales must be bounded by a documented factory tolerance policy (default: 0.5% or ±15 kg). If a physical scale drifts, the system fails closed to prevent unverified inventory creation.
