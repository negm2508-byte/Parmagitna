# PaperERP Enterprise — Database Schema & Migration Governance

## 1. Migration Principles & Governance Rules

Flyway is the sole authoritative mechanism for database schema evolution in PaperERP Enterprise.

- **Zero Auto-Update in Production**: Hibernate `ddl-auto` is strictly set to `validate` in production and `none` during standard runtime. Schema mutation is exclusively driven by Flyway.
- **Forward-Only Migrations**: Once a migration has been applied or shared, it is immutable. Schema changes or fixes are introduced via forward-only versioned migration files (`V1`, `V2`, `V3`).
- **Safety First on Existing Supabase Instances**: Never execute migrations against a live client Supabase database without pre-migration schema review and an verified, independent backup (`pg_dump`).
- **Precision Data Types**: All physical measurements (weights, widths, GSM, moisture, caliper, bursting strength) are stored as `NUMERIC(12, 2)` or `NUMERIC(10, 3)`. Floating-point types (`REAL`, `FLOAT`, `DOUBLE PRECISION`) are prohibited.
- **Timezone Integrity**: All timestamp columns use `TIMESTAMPTZ` (UTC-aware timestamp with time zone).
- **UUID Primary Keys**: Primary keys use `UUID` with PostgreSQL `gen_random_uuid()` defaults.

## 2. Migration Progression

### `V1__baseline_paper_erp_schema.sql`
Establishes the foundation kernel schema:
1. Multi-tenant composite unique constraints: `UNIQUE (tenant_id, id)` on all parent entities.
2. Multi-tenant composite foreign keys: `FOREIGN KEY (tenant_id, parent_id) REFERENCES parent(tenant_id, id)`.
3. Strict roll-type check constraint on `inventory_ledger`:
   ```sql
   CONSTRAINT chk_ledger_target_exclusive CHECK (
       (jumbo_roll_id IS NOT NULL AND slit_roll_id IS NULL) OR
       (jumbo_roll_id IS NULL AND slit_roll_id IS NOT NULL)
   )
   ```
4. Physical dimension CHECK constraints (`weight_kg > 0`, `deckle_width_mm > 0`, `gsm > 0`).
5. First-class `slitting_runs` aggregate with parent locking reference, net weight snapshots, scale variance tracking, and idempotency key uniqueness.
6. Row-Level Security (RLS) policies on all tenant-scoped operational tables.
7. High-performance indexes on roll barcode/serial lookups, roll genealogy, and ledger sequence.

### Safe Forward-Only Correction Policy
If connecting to an existing environment that previously ran a legacy schema, forward-only corrective migrations (`V2__...sql`) must be applied rather than modifying `V1`. The migration runner checks Flyway metadata table (`flyway_schema_history`) to guarantee deterministic upgrade paths.
