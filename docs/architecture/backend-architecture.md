# PaperERP Enterprise — Backend Architecture Specification

## 1. Executive Summary

PaperERP Enterprise is an Arabic-first, multi-tenant Enterprise Resource Planning backend built specifically for industrial paper manufacturing facilities. It enforces multi-layered defense-in-depth security, strict tenant and plant isolation, atomic slitting execution with mass-balance accounting, immutable inventory ledgers, and comprehensive auditability.

## 2. Architectural Paradigm: Modular Monolith

The backend is architected as an industrial-grade **Modular Monolith** using Java 17 LTS and Spring Boot 3.4.x. 

```
backend/
├── pom.xml                     (Root Aggregator & Dependency Management)
├── mvnw / mvnw.cmd             (Cross-platform Maven Wrapper)
├── paper-erp-common/           (Tenant context, shared value objects, validation, base error models)
├── paper-erp-core/             (Domain entities, Flyway migrations, JPA repositories, domain services)
└── paper-erp-api/              (REST controllers, Spring Security, JWT filters, CSRF, OpenAPI)
```

### Module Boundaries and Responsibilities

1. **`paper-erp-common`**:
   - `TenantContext` & `PlantContext`: Request-scoped thread-safe execution context with deterministic cleanup.
   - Core Domain Primitives: Strongly-typed identifiers, BigDecimal math utilities, Arabic-first localization messages.
   - API Contracts: Standardized `ApiResponse<T>`, `ApiErrorResponse`, `PageResponse<T>`.
   - Security Enums: System roles (`ADMIN`, `PLANT_MANAGER`, `PRODUCTION_OPERATOR`, `QC_INSPECTOR`, `WAREHOUSE_KEEPER`).
   - Base Exceptions: `DomainException`, `EntityNotFoundException`, `TenantAccessDeniedException`, `ValidationException`, `MassBalanceExceededToleranceException`.

2. **`paper-erp-core`**:
   - **Identity**: Multi-tenant users, password hashing (Argon2id / BCrypt), rotating refresh token families, session invalidation.
   - **Master Data**: Plants, production machines, paper grades (GSM, caliper, burst, moisture specifications), units of measure, configurable manufacturing tolerance rules.
   - **Inventory Ledger**: Append-only transaction ledger (`inventory_ledger`) with composite tenant integrity and strict exactly-one-target constraints (`jumbo_roll_id` XOR `slit_roll_id`).
   - **Production & Slitting**: Jumbo roll manufacturing registration and atomic slitting runs (`slitting_runs`) with parent locking, mass balance accounting, deckle width reconciliation, and child roll genealogy.
   - **Quality**: Laboratory roll inspections, raw parameter recording, and disposition transitions (`RELEASED`, `QUARANTINE`, `REJECTED`, `DOWNGRADED`).
   - **Audit**: Append-only security and operational audit trail (`audit_logs`).

3. **`paper-erp-api`**:
   - Secure REST endpoints prefixed with `/api/v1`.
   - Cookie-based authentication filter with dual-token model (short-lived JWT access token in memory/cookie + secure rotating refresh token).
   - Double-submit CSRF cookie protection (`XSRF-TOKEN` / `X-XSRF-TOKEN`).
   - Restrictive CORS configuration matching verified frontend origins.
   - Centralized exception translation preventing secret or topology leakage.
   - Springdoc OpenAPI 3.0 documentation.

## 3. Strict Package & Architectural Governance (ArchUnit)

Cross-module coupling rules:
- Controllers in `paper-erp-api` must call domain services; controllers MUST NEVER inject JPA repositories directly.
- Repositories in `paper-erp-core` are private to their domain packages. Cross-domain interactions occur strictly via domain service contracts.
- Database access is strictly parameterized. No dynamic SQL concatenation.
- Architecture rules are formally enforced via `ArchUnitTest` running in the automated test suite.
