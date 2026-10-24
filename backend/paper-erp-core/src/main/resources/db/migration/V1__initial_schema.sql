-- =============================================================================
-- PaperERP Enterprise — V1__initial_schema.sql
-- Baseline Schema: Multi-Tenancy, Master Data, Roll Traceability & QC Core
-- Tested and compatible with PostgreSQL 16+ and standard SQL / H2 compatibility
-- =============================================================================

-- 1. TENANTS & PLANTS
CREATE TABLE tenants (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name_ar VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plants (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    code VARCHAR(30) NOT NULL,
    name_ar VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_plants_tenant_code UNIQUE (tenant_id, code)
);

-- 2. USERS & ROLES
CREATE TABLE users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    plant_id UUID REFERENCES plants(id) ON DELETE RESTRICT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name_ar VARCHAR(100) NOT NULL,
    full_name_en VARCHAR(100),
    role VARCHAR(30) NOT NULL CHECK (role IN ('ADMIN', 'PLANT_MANAGER', 'PRODUCTION_OPERATOR', 'QC_INSPECTOR', 'WAREHOUSE_KEEPER')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_tenant_username UNIQUE (tenant_id, username)
);

-- 3. MASTER DATA: PAPER GRADES & MACHINES
CREATE TABLE paper_grades (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    code VARCHAR(30) NOT NULL,
    name_ar VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('FLUTING', 'TESTLINER', 'KRAFT', 'DUPLEX', 'OTHER')),
    default_gsm_min NUMERIC(6,2) NOT NULL CHECK (default_gsm_min > 0),
    default_gsm_max NUMERIC(6,2) NOT NULL CHECK (default_gsm_max >= default_gsm_min),
    burst_factor_min NUMERIC(6,2),
    cobb_60_max NUMERIC(6,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_grades_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE machines (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    plant_id UUID NOT NULL REFERENCES plants(id) ON DELETE RESTRICT,
    code VARCHAR(30) NOT NULL,
    name_ar VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('PAPER_MACHINE', 'SLITTER_REWINDER')),
    max_trim_width_mm NUMERIC(8,2) NOT NULL CHECK (max_trim_width_mm > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_machines_tenant_code UNIQUE (tenant_id, code)
);

-- 4. WAREHOUSES & LOCATIONS
CREATE TABLE warehouses (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    plant_id UUID NOT NULL REFERENCES plants(id) ON DELETE RESTRICT,
    code VARCHAR(30) NOT NULL,
    name_ar VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_warehouses_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE warehouse_locations (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    zone VARCHAR(20) NOT NULL,
    row_number VARCHAR(20) NOT NULL,
    bay_number VARCHAR(20) NOT NULL,
    barcode VARCHAR(50) NOT NULL,
    CONSTRAINT uq_locations_barcode UNIQUE (tenant_id, barcode)
);

-- 5. JUMBO ROLLS (MOTHER ROLLS FROM PAPER MACHINE)
CREATE TABLE jumbo_rolls (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    plant_id UUID NOT NULL REFERENCES plants(id) ON DELETE RESTRICT,
    machine_id UUID NOT NULL REFERENCES machines(id) ON DELETE RESTRICT,
    paper_grade_id UUID NOT NULL REFERENCES paper_grades(id) ON DELETE RESTRICT,
    serial_number VARCHAR(50) NOT NULL,
    target_gsm NUMERIC(6,2) NOT NULL CHECK (target_gsm > 0),
    deckle_width_mm NUMERIC(8,2) NOT NULL CHECK (deckle_width_mm > 0),
    gross_weight_kg NUMERIC(10,2) NOT NULL CHECK (gross_weight_kg > 0),
    net_weight_kg NUMERIC(10,2) NOT NULL CHECK (net_weight_kg > 0 AND net_weight_kg <= gross_weight_kg),
    status VARCHAR(30) NOT NULL CHECK (status IN ('ACTIVE_WIP', 'FULLY_SLIT', 'SCRAPPED')),
    manufactured_at TIMESTAMP WITH TIME ZONE NOT NULL,
    operator_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_jumbo_serial UNIQUE (tenant_id, serial_number)
);

-- 6. SLIT ROLLS (CUSTOMER/CHILD ROLLS)
CREATE TABLE slit_rolls (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    parent_jumbo_id UUID NOT NULL REFERENCES jumbo_rolls(id) ON DELETE RESTRICT,
    paper_grade_id UUID NOT NULL REFERENCES paper_grades(id) ON DELETE RESTRICT,
    warehouse_id UUID REFERENCES warehouses(id) ON DELETE RESTRICT,
    location_id UUID REFERENCES warehouse_locations(id) ON DELETE RESTRICT,
    serial_number VARCHAR(50) NOT NULL,
    barcode VARCHAR(100) NOT NULL,
    cut_sequence INTEGER NOT NULL CHECK (cut_sequence > 0),
    width_mm NUMERIC(8,2) NOT NULL CHECK (width_mm > 0),
    diameter_mm NUMERIC(8,2) CHECK (diameter_mm > 0),
    core_diameter_mm NUMERIC(6,2) NOT NULL DEFAULT 76.0 CHECK (core_diameter_mm > 0),
    measured_gsm NUMERIC(6,2) NOT NULL CHECK (measured_gsm > 0),
    gross_weight_kg NUMERIC(10,2) NOT NULL CHECK (gross_weight_kg > 0),
    tare_weight_kg NUMERIC(10,2) NOT NULL DEFAULT 2.0 CHECK (tare_weight_kg >= 0),
    net_weight_kg NUMERIC(10,2) NOT NULL CHECK (net_weight_kg > 0 AND net_weight_kg <= gross_weight_kg),
    quality_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_INSPECTION' 
        CHECK (quality_status IN ('PENDING_INSPECTION', 'RELEASED', 'QUARANTINE', 'REJECTED', 'DOWNGRADED')),
    inventory_status VARCHAR(30) NOT NULL DEFAULT 'IN_STOCK'
        CHECK (inventory_status IN ('IN_STOCK', 'RESERVED', 'DISPATCHED', 'CONSUMED_REPROCESS', 'SCRAPPED')),
    rejection_reason TEXT,
    slitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    operator_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_slit_serial UNIQUE (tenant_id, serial_number),
    CONSTRAINT uq_slit_barcode UNIQUE (tenant_id, barcode)
);

-- 7. QUALITY INSPECTIONS (LABORATORY QC)
CREATE TABLE quality_inspections (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    roll_id UUID NOT NULL REFERENCES slit_rolls(id) ON DELETE RESTRICT,
    tested_gsm NUMERIC(6,2) NOT NULL CHECK (tested_gsm > 0),
    moisture_percentage NUMERIC(5,2) NOT NULL CHECK (moisture_percentage >= 0 AND moisture_percentage <= 100),
    caliper_thickness_um NUMERIC(6,2),
    bursting_strength_kpa NUMERIC(6,2),
    cobb_60_gsm NUMERIC(6,2),
    disposition VARCHAR(30) NOT NULL CHECK (disposition IN ('RELEASED', 'QUARANTINE', 'REJECTED', 'DOWNGRADED')),
    notes TEXT,
    inspector_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    inspected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. IMMUTABLE INVENTORY MOVEMENT LEDGER (APPEND-ONLY)
CREATE TABLE inventory_movements (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    roll_id UUID NOT NULL REFERENCES slit_rolls(id) ON DELETE RESTRICT,
    movement_type VARCHAR(30) NOT NULL CHECK (movement_type IN (
        'PRODUCTION_RECEIPT', 'LOCATION_TRANSFER', 'STATUS_CHANGE', 'DISPATCH', 'RETURN', 'SCRAP'
    )),
    from_warehouse_id UUID REFERENCES warehouses(id) ON DELETE RESTRICT,
    to_warehouse_id UUID REFERENCES warehouses(id) ON DELETE RESTRICT,
    from_location_id UUID REFERENCES warehouse_locations(id) ON DELETE RESTRICT,
    to_location_id UUID REFERENCES warehouse_locations(id) ON DELETE RESTRICT,
    from_quality_status VARCHAR(30),
    to_quality_status VARCHAR(30),
    net_weight_kg NUMERIC(10,2) NOT NULL,
    reference_document VARCHAR(50),
    reason TEXT,
    actor_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. AUDIT EVENTS (SECURITY & INTEGRITY LOG)
CREATE TABLE audit_events (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    old_value_json TEXT,
    new_value_json TEXT,
    client_ip VARCHAR(45),
    correlation_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. CRITICAL INDEXES
CREATE INDEX idx_jumbo_tenant_serial ON jumbo_rolls(tenant_id, serial_number);
CREATE INDEX idx_slit_tenant_serial ON slit_rolls(tenant_id, serial_number);
CREATE INDEX idx_slit_parent_jumbo ON slit_rolls(tenant_id, parent_jumbo_id);
CREATE INDEX idx_slit_quality_inventory ON slit_rolls(tenant_id, quality_status, inventory_status);
CREATE INDEX idx_movements_roll ON inventory_movements(tenant_id, roll_id);
CREATE INDEX idx_movements_created ON inventory_movements(tenant_id, created_at);
