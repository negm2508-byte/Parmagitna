# PaperERP Enterprise — Supabase PostgreSQL Integration Guide

## 1. Overview & Connection Architecture

PaperERP Enterprise connects to Supabase PostgreSQL over secure JDBC with SSL enabled. All database traffic originates from the server-side Spring Boot application; no database credentials, direct JDBC connections, or service keys are ever shared with or exposed to browser clients.

```
[ React / SPA Client ]
         │ (HTTP REST / Cookies / CSRF / Bearer Token)
         ▼
[ PaperERP Spring Boot Backend ]
         │ (JDBC with TLS 1.3 / HikariCP Pool)
         ▼
[ Supabase PostgreSQL (Direct / Session Pooler) ]
```

## 2. Supabase Connection Modes

Supabase provides two primary connection strings:

### Mode A: Direct Connection (Recommended for persistent servers)
- **Host**: `db.[project-ref].supabase.co`
- **Port**: `5432`
- **Use Case**: Direct TCP connection. Recommended when backend runs in environments with IPv6 support or dedicated static hosting.
- **JDBC URL**:
  ```properties
  SUPABASE_DB_URL=jdbc:postgresql://db.[project-ref].supabase.co:5432/postgres?sslmode=require
  ```

### Mode B: Session Pooler (Supavisor - Port 5432)
- **Host**: `[region].pooler.supabase.com`
- **Port**: `5432`
- **Use Case**: Provides session-level connection pooling over IPv4 networks. Compatible with prepared statements and transaction-scoped `set_config('app.current_tenant_id', ...)` session settings.
- **JDBC URL**:
  ```properties
  SUPABASE_DB_URL=jdbc:postgresql://[region].pooler.supabase.com:5432/postgres?sslmode=require
  ```

> [!CAUTION]
> **Transaction Pooler (Port 6543) Incompatibility**:
> Do NOT use Supabase Transaction Pooler (port 6543) with PaperERP Enterprise. Transaction pooling resets session state between statements, breaking `set_config` transaction-scoped RLS contexts and named prepared statements.

## 3. Database Roles & Separation of Privileges

To ensure PostgreSQL RLS functions properly:
1. **Migration Owner Role (`paper_erp_migrator`)**:
   - Superuser or table owner privileges required to create tables, alter constraints, and define RLS policies.
   - Used only by Flyway during deployment.
2. **Runtime Application Role (`paper_erp_app`)**:
   - Restricted role with `SELECT`, `INSERT`, `UPDATE` on operational tables.
   - Explicitly does NOT own the tables, preventing RLS bypass (`FORCE ROW LEVEL SECURITY` applies).
   - Prohibited from `DELETE` or `UPDATE` on `inventory_ledger` and `audit_logs` to maintain append-only immutability.

## 4. Connection Smoke Test Procedure

To verify connectivity safely without modifying schema or applying migrations:
```powershell
# In PowerShell:
$env:SUPABASE_DB_URL="jdbc:postgresql://<host>:5432/postgres?sslmode=require"
$env:SUPABASE_DB_USERNAME="postgres.<project-ref>"
$env:SUPABASE_DB_PASSWORD="<password>"
$env:SPRING_PROFILES_ACTIVE="local"

# Execute Spring Boot smoke test profile
.\backend\mvnw.cmd test -Dtest=SupabaseConnectionSmokeTest
```
