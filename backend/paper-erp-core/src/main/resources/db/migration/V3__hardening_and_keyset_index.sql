-- ==============================================================================
-- PaperERP Enterprise: V3 Production Blockers Remediation & Hardening Migration
-- ==============================================================================

-- 1. FIX INVENTORY LEDGER KEYSET PAGINATION INDEX
-- The previous V2 index idx_ledger_keyset was misaligned on (tenant_id, created_at DESC, id DESC).
-- The actual operational query filters by tenant_id, plant_id, and orders by movement_timestamp DESC, id DESC.
DROP INDEX IF EXISTS idx_ledger_keyset;

CREATE INDEX IF NOT EXISTS idx_ledger_keyset_v2 
    ON inventory_ledger (tenant_id, plant_id, movement_timestamp DESC, id DESC);

-- 2. HARDEN TRANSACTIONAL OUTBOX FOR HORIZONTAL SCALING & LEASING
-- Add locked_at column for lease tracking and zombie process recovery
ALTER TABLE outbox_events 
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMPTZ;

-- Partial index for high-throughput claiming by background workers
CREATE INDEX IF NOT EXISTS idx_outbox_claiming 
    ON outbox_events (created_at ASC) 
    WHERE status IN ('PENDING', 'PROCESSING');

-- Update RLS policy on outbox_events to allow system background workers
DROP POLICY IF EXISTS outbox_tenant_isolation_policy ON outbox_events;

CREATE POLICY outbox_tenant_isolation_policy ON outbox_events
    FOR ALL
    USING (
        CASE
            WHEN current_setting('app.current_tenant_id', true) IS NULL 
                 OR current_setting('app.current_tenant_id', true) = '' 
                 OR current_setting('app.current_tenant_id', true) = 'SYSTEM' THEN true
            WHEN current_setting('app.current_tenant_id', true) ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' 
                 THEN tenant_id = current_setting('app.current_tenant_id', true)::uuid
            ELSE false
        END
    );

-- 3. FORCE ROW LEVEL SECURITY ON IDEMPOTENCY RECORDS
ALTER TABLE idempotency_records FORCE ROW LEVEL SECURITY;
