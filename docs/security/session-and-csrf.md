# PaperERP Enterprise — Session Management, Authentication & CSRF Protection

## 1. Authentication Architecture

PaperERP Enterprise implements a hardened dual-token authentication pattern tailored for enterprise manufacturing web clients and factory-floor terminals:

```
[ Frontend Client ]                                      [ PaperERP API ]
        │                                                        │
        │─── POST /api/v1/auth/login (username, password) ──────>│
        │                                                        │ Validate Argon2id / BCrypt
        │                                                        │ Generate Access JWT (15 min)
        │                                                        │ Generate Rotating Refresh Token (8 hr)
        │                                                        │ Save Hashed Refresh Token to DB
        │<── Set HttpOnly Refresh Cookie + Return Access Token ──│
        │                                                        │
        │─── GET /api/v1/masterdata/grades (Bearer Token) ──────>│
        │                                                        │ Validates signature, tenant claims
        │<── 200 OK + Data ──────────────────────────────────────│
        │                                                        │
        │ (When Access Token Expires after 15m)                 │
        │─── POST /api/v1/auth/refresh (HttpOnly Cookie) ───────>│
        │                                                        │ Validates Refresh Token Family
        │                                                        │ Detects Reuse & Revokes on Tampering
        │                                                        │ Issues New Token Pair
        │<── 200 OK + New Access Token + Rotated Cookie ─────────│
```

## 2. Token Specifications & Expiration Policies

1. **Access Token (JWT)**:
   - Lifetime: 15 minutes.
   - Alg: HMAC-SHA256 (`HS256`) using verified 256-bit secret key.
   - Claims: `sub` (User UUID), `tenant_id` (Tenant UUID), `plant_id` (Default Plant UUID), `role` (`ADMIN`, `PLANT_MANAGER`, etc.), `email`, `iat`, `exp`.
2. **Refresh Token**:
   - Lifetime: 8 hours (configured to cover standard factory operating shifts).
   - Storage: Cryptographically secure random 64-byte token stored as SHA-256 hash in `refresh_tokens` table.
   - Rotation: Every refresh request generates a new refresh token and revokes the predecessor.
   - Reuse Detection: If an already-revoked refresh token is presented, the entire token family is immediately revoked, protecting compromised sessions.

## 3. Cookie Hardening & Cross-Site Request Forgery (CSRF)

1. **Cookie Attributes**:
   - `HttpOnly`: True (prevents client-side XSS from extracting the refresh token).
   - `Secure`: True in production (enforced via HTTPS).
   - `SameSite`: `Lax` (prevents cross-site cookie transmission on third-party links while supporting seamless SPA navigation).
2. **CSRF Protection**:
   - Double-submit CSRF cookie token mechanism via Spring Security's `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
   - The frontend reads the `XSRF-TOKEN` cookie and transmits it back on state-changing operations (`POST`, `PUT`, `PATCH`, `DELETE`) via the `X-XSRF-TOKEN` header.
   - CSRF is not disabled for the API simply because JSON is consumed.

## 4. Role-Based Access Control (RBAC) Matrix

| Endpoint Group | Allowed Roles |
|---|---|
| `/api/v1/auth/**` | Public (Login, Refresh, Logout, CSRF) |
| `/api/v1/masterdata/**` (Write) | `ADMIN`, `PLANT_MANAGER` |
| `/api/v1/masterdata/**` (Read) | `ADMIN`, `PLANT_MANAGER`, `PRODUCTION_OPERATOR`, `QC_INSPECTOR`, `WAREHOUSE_KEEPER` |
| `/api/v1/production/jumbo-rolls` | `ADMIN`, `PLANT_MANAGER`, `PRODUCTION_OPERATOR` |
| `/api/v1/production/slitting-operations` | `ADMIN`, `PLANT_MANAGER`, `PRODUCTION_OPERATOR` |
| `/api/v1/quality/**` | `ADMIN`, `PLANT_MANAGER`, `QC_INSPECTOR` |
| `/api/v1/inventory/transfers` | `ADMIN`, `PLANT_MANAGER`, `WAREHOUSE_KEEPER` |
| `/api/v1/inventory/dispatch` | `ADMIN`, `PLANT_MANAGER`, `WAREHOUSE_KEEPER` (Requires `RELEASED` quality disposition) |
| `/api/v1/audit/**` | `ADMIN`, `PLANT_MANAGER` |
| `/actuator/health` | Public (Summary only; detailed health restricted) |
| `/actuator/**` (Sensors/Metrics) | `ADMIN` only |
