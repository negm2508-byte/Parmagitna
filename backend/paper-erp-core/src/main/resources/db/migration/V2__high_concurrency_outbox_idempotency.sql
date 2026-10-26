-- ==============================================================================
-- PaperERP Enterprise: V2 High Concurrency, Transactional Outbox, & Idempotency
-- ==============================================================================

-- 1. IDEMPOTENCY RECORDS
CREATE TABLE IF NOT EXISTS idempotency_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL, -- 'PENDING', 'COMPLETED', 'FAILED'
    response_code INTEGER,
    response_body TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_idempotency_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_idempotency_tenant_key UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT fk_idempotency_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- 2. TRANSACTIONAL OUTBOX EVENTS
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'PUBLISHED', 'FAILED'
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    CONSTRAINT uk_outbox_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_outbox_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox_events (status, created_at);

-- 3. HIGH-CONCURRENCY & KEYSET PAGINATION PERFORMANCE INDEXES
CREATE INDEX IF NOT EXISTS idx_ledger_keyset ON inventory_ledger (tenant_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_jumbo_rolls_concurrency ON jumbo_rolls (tenant_id, status, plant_id, machine_id);
CREATE INDEX IF NOT EXISTS idx_slit_rolls_concurrency ON slit_rolls (tenant_id, status, parent_jumbo_id);
CREATE INDEX IF NOT EXISTS idx_slitting_runs_idempotency ON slitting_runs (tenant_id, idempotency_key);

-- 4. ROW-LEVEL SECURITY FOR NEW TABLES
ALTER TABLE idempotency_records ENABLE ROW LEVEL SECURITY;
CREATE POLICY idempotency_tenant_isolation_policy ON idempotency_records
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

ALTER TABLE outbox_events ENABLE ROW LEVEL SECURITY;
CREATE POLICY outbox_tenant_isolation_policy ON outbox_events
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);
