# PaperERP Enterprise — Multi-Tenant Isolation Architecture

## 1. Multi-Tenant Architecture Strategy

PaperERP Enterprise implements **Defense-in-Depth Shared Database Multi-Tenancy**. Isolation is enforced across four independent, synchronized layers:

```
[ HTTP Request with Bearer Token / Cookie ]
                    │
                    ▼
       ┌─────────────────────────┐
 Layer 1: Server Authentication & TenantContext
       │ (Derives tenant_id exclusively from verified JWT, rejects unauthenticated claims)
       └─────────────────────────┘
                    │
                    ▼
       ┌─────────────────────────┐
 Layer 2: Domain Services & JPA Query Filters
       │ (Mandatory tenant filtering across all repository finders and mutations)
       └─────────────────────────┘
                    │
                    ▼
       ┌─────────────────────────┐
 Layer 3: PostgreSQL Row-Level Security (RLS)
       │ (Enforces session tenant `app.current_tenant_id` at database engine level)
       └─────────────────────────┘
                    │
                    ▼
       ┌─────────────────────────┐
 Layer 4: Composite Foreign Keys & Unique Constraints
       │ (UNIQUE(tenant_id, id) + FK(tenant_id, parent_id) prevents cross-tenant references)
       └─────────────────────────┘
```

## 2. Layer 1: Context Derivation & Lifecycle Safety

The authenticated tenant is derived strictly from server-validated cryptographic claims.
- The client cannot supply or override `tenant_id` via request body, query parameter, or custom header.
- `TenantContext` stores the active `UUID tenantId` in a `ThreadLocal`.
- A mandatory Spring `Filter` wrapped in a `try-finally` block executes `TenantContext.clear()` after every request, preventing ThreadLocal leaks across connection threads in the application pool.

## 3. Layer 2: Composite Unique Keys & Foreign Keys

To eliminate any possibility of relational cross-tenant contamination (e.g., Tenant B attaching their Slit Roll to Tenant A's Jumbo Roll):
- All parent tables declare:
  ```sql
  CONSTRAINT uk_<table>_tenant_id UNIQUE (tenant_id, id);
  ```
- All child tables reference the parent using composite foreign keys:
  ```sql
  CONSTRAINT fk_<child>_parent_tenant 
      FOREIGN KEY (tenant_id, parent_id) 
      REFERENCES <parent> (tenant_id, id) 
      ON DELETE RESTRICT;
  ```
This guarantees at the relational engine level that even if an attacker guesses a valid UUID belonging to another tenant, the database rejects the foreign key reference because the `tenant_id` mismatch violates relational integrity.

## 4. Layer 3: PostgreSQL Row-Level Security (RLS)

Every operational table containing tenant data enables RLS:
```sql
ALTER TABLE jumbo_rolls ENABLE ROW LEVEL SECURITY;
ALTER TABLE jumbo_rolls FORCE ROW LEVEL SECURITY;

CREATE POLICY jumbo_rolls_tenant_isolation ON jumbo_rolls
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);
```

### Transaction-Scoped Session Context
When a transaction begins, the application sets the tenant context inside the active transaction:
```sql
SELECT set_config('app.current_tenant_id', :tenantId, true);
```
Setting parameter `is_local = true` guarantees that the setting resets automatically at transaction commit or rollback, preventing tenant context from leaking to other transactions sharing the same pooled HikariCP connection.

## 5. Layer 4: Connection Pooler Hygiene

When HikariCP checks in a connection:
- The datasource executes `RESET app.current_tenant_id;` or cleans up session parameters before returning the physical connection to the idle pool.
- Tests verify pooled connection reuse across alternating tenants to confirm zero context leakage.
