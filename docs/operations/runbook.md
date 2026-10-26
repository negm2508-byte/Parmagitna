# PaperERP Enterprise — Operations & Troubleshooting Runbook

## 1. Service Management & Execution Commands

### Local Development / Testing (PowerShell)
```powershell
# Set Environment Variables Safely (No hardcoding)
$env:SUPABASE_DB_URL="jdbc:postgresql://localhost:5432/paper_erp_dev"
$env:SUPABASE_DB_USERNAME="postgres"
$env:SUPABASE_DB_PASSWORD="your_password"
$env:APP_JWT_SECRET="dGhpcy1pcy1hLXZlcnktc2VjdXJlLTI1Ni1iaXQta2V5LWZvci10ZXN0aW5nLW9ubHk="
$env:SPRING_PROFILES_ACTIVE="local"

# Build the modular project
cd backend
.\mvnw.cmd clean package -DskipTests=false

# Run the API application
java -jar paper-erp-api/target/paper-erp-api-1.0.0-SNAPSHOT.jar
```

## 2. Health & Diagnostics Checks

- **Application Health**:
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
  ```
- **Database Connection Verification**:
  Actuator verifies HikariCP pool connectivity without leaking hostnames, passwords, or connection URLs.

## 3. Troubleshooting Matrix

| Issue Symptom | Root Cause | Remediation Procedure |
|---|---|---|
| `PSQLException: Connection refused` | Database host is unreachable or SSL connection mode rejected. | 1. Verify `SUPABASE_DB_URL` contains `?sslmode=require`.<br>2. Check if connecting via IPv4 through Supabase Session Pooler (port 5432). |
| `HikariPool-1 - Connection is not available, request timed out after 30000ms` | Connection pool exhausted by unclosed connections or long-running queries. | 1. Verify all transactions release connections cleanly.<br>2. Check `DB_POOL_MAX_SIZE` (default 20).<br>3. Inspect PostgreSQL active queries: `SELECT * FROM pg_stat_activity WHERE state != 'idle';`. |
| `TenantAccessDeniedException` | User attempted to access entity belonging to another `tenant_id` or `TenantContext` was not set. | 1. Verify JWT token contains valid `tenant_id` claim.<br>2. Confirm request passes through `TenantContextFilter`. |
| `MassBalanceExceededToleranceException` | Difference between parent jumbo roll input weight and child rolls + waste exceeds configured tolerance. | 1. Calibrate scales.<br>2. Review slitting run input weights and recorded waste.<br>3. Do NOT artificially modify weights without operational approval. |
| `FlywaySqlException: Migration checksum mismatch` | An applied migration file was modified after execution. | 1. Never edit applied migrations.<br>2. Revert local edits to the migration file and issue a new forward-only migration. |
