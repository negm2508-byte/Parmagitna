-- ==============================================================================
-- PaperERP Enterprise: V1 Baseline Schema
-- Industrial Multi-Tenant Paper Manufacturing ERP Kernel
-- Features: Composite Unique Keys, Composite Foreign Keys, Append-Only Ledger,
-- First-Class Slitting Runs, Exclusive Roll Movement Targets, and PostgreSQL RLS
-- ==============================================================================

-- Enable UUID extension if available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. TENANTS & CORPORATE IDENTITY
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name_ar VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. PLANTS
CREATE TABLE plants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name_ar VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_plants_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_plants_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_plants_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT
);

-- 3. MACHINES
CREATE TABLE machines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name_ar VARCHAR(255) NOT NULL,
    machine_type VARCHAR(50) NOT NULL, -- 'PAPER_MACHINE', 'SLITTER_REWINDER'
    max_deckle_width_mm NUMERIC(10,2) NOT NULL,
    min_trim_width_mm NUMERIC(10,2) NOT NULL DEFAULT 30.00,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_machines_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_machines_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_machines_plant FOREIGN KEY (tenant_id, plant_id) REFERENCES plants(tenant_id, id) ON DELETE RESTRICT
);

-- 4. PAPER GRADES
CREATE TABLE paper_grades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name_ar VARCHAR(255) NOT NULL,
    gsm_target NUMERIC(8,2) NOT NULL,
    gsm_min NUMERIC(8,2) NOT NULL,
    gsm_max NUMERIC(8,2) NOT NULL,
    caliper_target_um NUMERIC(8,2) NOT NULL,
    moisture_target_pct NUMERIC(5,2) NOT NULL,
    burst_index_target NUMERIC(8,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_paper_grades_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_paper_grades_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_paper_grades_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT chk_paper_grade_gsm CHECK (gsm_min <= gsm_target AND gsm_target <= gsm_max AND gsm_min > 0)
);

-- 5. CORE SPECIFICATIONS (Eliminates fabricated defaults)
CREATE TABLE core_specifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    diameter_mm NUMERIC(8,2) NOT NULL,
    tare_weight_per_m_kg NUMERIC(8,4) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_core_specs_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_core_specs_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_core_specs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT chk_core_diameter CHECK (diameter_mm > 0 AND tare_weight_per_m_kg > 0)
);

-- 6. CONFIGURABLE MANUFACTURING RULES
CREATE TABLE manufacturing_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plant_id UUID,
    rule_key VARCHAR(100) NOT NULL,
    rule_value_numeric NUMERIC(12,4),
    rule_value_text VARCHAR(255),
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mfg_rules_tenant_key UNIQUE (tenant_id, plant_id, rule_key),
    CONSTRAINT fk_mfg_rules_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT
);

-- 7. USERS & IDENTITY
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    default_plant_id UUID,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name_ar VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- 'ADMIN', 'PLANT_MANAGER', 'PRODUCTION_OPERATOR', 'QC_INSPECTOR', 'WAREHOUSE_KEEPER'
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_users_tenant_username UNIQUE (tenant_id, username),
    CONSTRAINT uk_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT
);

-- 8. REFRESH TOKENS (Rotating Sessions)
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    family_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_tokens_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (tenant_id, user_id) REFERENCES users(tenant_id, id) ON DELETE CASCADE
);

-- 9. WAREHOUSES & LOCATIONS
CREATE TABLE warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name_ar VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_warehouses_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_warehouses_tenant_code UNIQUE (tenant_id, plant_id, code),
    CONSTRAINT fk_warehouses_plant FOREIGN KEY (tenant_id, plant_id) REFERENCES plants(tenant_id, id) ON DELETE RESTRICT
);

CREATE TABLE warehouse_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    aisle VARCHAR(20),
    row_number VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_wh_locations_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_wh_locations_code UNIQUE (tenant_id, warehouse_id, code),
    CONSTRAINT fk_wh_locations_wh FOREIGN KEY (tenant_id, warehouse_id) REFERENCES warehouses(tenant_id, id) ON DELETE RESTRICT
);

-- 10. JUMBO ROLLS (Parent Rolls)
CREATE TABLE jumbo_rolls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plant_id UUID NOT NULL,
    machine_id UUID NOT NULL,
    grade_id UUID NOT NULL,
    core_spec_id UUID,
    location_id UUID,
    serial_number VARCHAR(100) NOT NULL,
    barcode VARCHAR(100) NOT NULL,
    gross_weight_kg NUMERIC(12,2) NOT NULL,
    tare_weight_kg NUMERIC(12,2) NOT NULL,
    net_weight_kg NUMERIC(12,2) NOT NULL,
    deckle_width_mm NUMERIC(10,2) NOT NULL,
    diameter_mm NUMERIC(10,2),
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    quality_disposition VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    production_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_jumbo_rolls_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_jumbo_rolls_tenant_serial UNIQUE (tenant_id, serial_number),
    CONSTRAINT uk_jumbo_rolls_tenant_barcode UNIQUE (tenant_id, barcode),
    CONSTRAINT fk_jumbo_plant FOREIGN KEY (tenant_id, plant_id) REFERENCES plants(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_jumbo_machine FOREIGN KEY (tenant_id, machine_id) REFERENCES machines(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_jumbo_grade FOREIGN KEY (tenant_id, grade_id) REFERENCES paper_grades(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT chk_jumbo_weights CHECK (gross_weight_kg > 0 AND tare_weight_kg >= 0 AND net_weight_kg > 0 AND net_weight_kg <= gross_weight_kg),
    CONSTRAINT chk_jumbo_width CHECK (deckle_width_mm > 0)
);

-- 11. SLITTING RUNS (First-Class Atomic Manufacturing Aggregate)
CREATE TABLE slitting_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plant_id UUID NOT NULL,
    parent_jumbo_id UUID NOT NULL,
    machine_id UUID NOT NULL,
    operator_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    input_gross_weight_kg NUMERIC(12,2) NOT NULL,
    input_tare_weight_kg NUMERIC(12,2) NOT NULL,
    input_net_weight_kg NUMERIC(12,2) NOT NULL,
    input_deckle_width_mm NUMERIC(10,2) NOT NULL,
    child_rolls_total_weight_kg NUMERIC(12,2) NOT NULL,
    recorded_trim_waste_kg NUMERIC(12,2) NOT NULL,
    scale_variance_kg NUMERIC(12,2) NOT NULL,
    mass_balance_status VARCHAR(50) NOT NULL,
    waste_disposition VARCHAR(50) NOT NULL DEFAULT 'BALED_FOR_RECYCLE',
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_slitting_runs_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_slitting_runs_tenant_idempotency UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT fk_slitting_runs_parent FOREIGN KEY (tenant_id, parent_jumbo_id) REFERENCES jumbo_rolls(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_slitting_runs_machine FOREIGN KEY (tenant_id, machine_id) REFERENCES machines(tenant_id, id) ON DELETE RESTRICT
);

-- 12. SLIT ROLLS (Child Rolls with Full Genealogy)
CREATE TABLE slit_rolls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plant_id UUID NOT NULL,
    parent_jumbo_id UUID NOT NULL,
    slitting_run_id UUID NOT NULL,
    grade_id UUID NOT NULL,
    core_spec_id UUID,
    location_id UUID,
    serial_number VARCHAR(100) NOT NULL,
    barcode VARCHAR(100) NOT NULL,
    cut_sequence_number INT NOT NULL,
    cut_width_mm NUMERIC(10,2) NOT NULL,
    diameter_mm NUMERIC(10,2),
    gross_weight_kg NUMERIC(12,2) NOT NULL,
    tare_weight_kg NUMERIC(12,2) NOT NULL,
    net_weight_kg NUMERIC(12,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    quality_disposition VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_slit_rolls_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_slit_rolls_tenant_serial UNIQUE (tenant_id, serial_number),
    CONSTRAINT uk_slit_rolls_tenant_barcode UNIQUE (tenant_id, barcode),
    CONSTRAINT fk_slit_rolls_parent FOREIGN KEY (tenant_id, parent_jumbo_id) REFERENCES jumbo_rolls(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_slit_rolls_run FOREIGN KEY (tenant_id, slitting_run_id) REFERENCES slitting_runs(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_slit_rolls_grade FOREIGN KEY (tenant_id, grade_id) REFERENCES paper_grades(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT chk_slit_weights CHECK (gross_weight_kg > 0 AND tare_weight_kg >= 0 AND net_weight_kg > 0 AND net_weight_kg <= gross_weight_kg),
    CONSTRAINT chk_slit_width CHECK (cut_width_mm > 0)
);

-- 13. INVENTORY LEDGER (Append-Only Transactional Stock Journal)
CREATE TABLE inventory_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plant_id UUID NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    roll_type VARCHAR(50) NOT NULL, -- 'JUMBO_ROLL', 'SLIT_ROLL'
    jumbo_roll_id UUID,
    slit_roll_id UUID,
    source_location_id UUID,
    destination_location_id UUID,
    quantity_change NUMERIC(12,2) NOT NULL,
    weight_kg NUMERIC(12,2) NOT NULL,
    actor_id UUID NOT NULL,
    reference_type VARCHAR(50), -- 'PRODUCTION', 'SLITTING_RUN', 'TRANSFER', 'DISPATCH'
    reference_id UUID,
    reason VARCHAR(255),
    movement_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inv_ledger_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_inv_ledger_jumbo FOREIGN KEY (tenant_id, jumbo_roll_id) REFERENCES jumbo_rolls(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_inv_ledger_slit FOREIGN KEY (tenant_id, slit_roll_id) REFERENCES slit_rolls(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT chk_ledger_target_exclusive CHECK (
        (jumbo_roll_id IS NOT NULL AND slit_roll_id IS NULL) OR
        (jumbo_roll_id IS NULL AND slit_roll_id IS NOT NULL)
    ),
    CONSTRAINT chk_ledger_weight CHECK (weight_kg > 0)
);

-- 14. QUALITY INSPECTIONS
CREATE TABLE quality_inspections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plant_id UUID NOT NULL,
    roll_type VARCHAR(50) NOT NULL,
    jumbo_roll_id UUID,
    slit_roll_id UUID,
    inspector_id UUID NOT NULL,
    measured_gsm NUMERIC(8,2),
    measured_moisture_pct NUMERIC(5,2),
    measured_caliper_um NUMERIC(8,2),
    measured_burst_strength NUMERIC(8,2),
    disposition VARCHAR(50) NOT NULL, -- 'PENDING', 'RELEASED', 'QUARANTINE', 'REJECTED', 'DOWNGRADED'
    notes VARCHAR(500),
    inspection_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_quality_insp_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT chk_quality_target_exclusive CHECK (
        (jumbo_roll_id IS NOT NULL AND slit_roll_id IS NULL) OR
        (jumbo_roll_id IS NULL AND slit_roll_id IS NOT NULL)
    )
);

-- 15. AUDIT LOGS (Append-Only Administrative & Security Audit Trail)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    actor_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    client_ip VARCHAR(50),
    details_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_audit_logs_tenant_id UNIQUE (tenant_id, id)
);

-- 16. INDEXES FOR OPERATIONAL PERFORMANCE & TRACEABILITY
CREATE INDEX idx_jumbo_serial_barcode ON jumbo_rolls(tenant_id, serial_number, barcode);
CREATE INDEX idx_jumbo_status_grade ON jumbo_rolls(tenant_id, plant_id, status, grade_id);
CREATE INDEX idx_slit_serial_barcode ON slit_rolls(tenant_id, serial_number, barcode);
CREATE INDEX idx_slit_parent_run ON slit_rolls(tenant_id, parent_jumbo_id, slitting_run_id);
CREATE INDEX idx_ledger_target_jumbo ON inventory_ledger(tenant_id, jumbo_roll_id, movement_timestamp);
CREATE INDEX idx_ledger_target_slit ON inventory_ledger(tenant_id, slit_roll_id, movement_timestamp);
CREATE INDEX idx_ledger_reference ON inventory_ledger(tenant_id, reference_type, reference_id);
CREATE INDEX idx_audit_tenant_actor ON audit_logs(tenant_id, actor_id, created_at);
CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);

-- 17. POSTGRESQL ROW-LEVEL SECURITY (RLS) POLICIES
-- Fail closed when app.current_tenant_id is missing or invalid

ALTER TABLE plants ENABLE ROW LEVEL SECURITY;
ALTER TABLE plants FORCE ROW LEVEL SECURITY;
CREATE POLICY plants_tenant_isolation ON plants
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

ALTER TABLE machines ENABLE ROW LEVEL SECURITY;
ALTER TABLE machines FORCE ROW LEVEL SECURITY;
CREATE POLICY machines_tenant_isolation ON machines
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

ALTER TABLE paper_grades ENABLE ROW LEVEL SECURITY;
ALTER TABLE paper_grades FORCE ROW LEVEL SECURITY;
CREATE POLICY paper_grades_tenant_isolation ON paper_grades
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

ALTER TABLE jumbo_rolls ENABLE ROW LEVEL SECURITY;
ALTER TABLE jumbo_rolls FORCE ROW LEVEL SECURITY;
CREATE POLICY jumbo_rolls_tenant_isolation ON jumbo_rolls
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

ALTER TABLE slitting_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE slitting_runs FORCE ROW LEVEL SECURITY;
CREATE POLICY slitting_runs_tenant_isolation ON slitting_runs
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

ALTER TABLE slit_rolls ENABLE ROW LEVEL SECURITY;
ALTER TABLE slit_rolls FORCE ROW LEVEL SECURITY;
CREATE POLICY slit_rolls_tenant_isolation ON slit_rolls
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

ALTER TABLE inventory_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_ledger FORCE ROW LEVEL SECURITY;
CREATE POLICY inventory_ledger_tenant_isolation ON inventory_ledger
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

ALTER TABLE quality_inspections ENABLE ROW LEVEL SECURITY;
ALTER TABLE quality_inspections FORCE ROW LEVEL SECURITY;
CREATE POLICY quality_inspections_tenant_isolation ON quality_inspections
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs FORCE ROW LEVEL SECURITY;
CREATE POLICY audit_logs_tenant_isolation ON audit_logs
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);
