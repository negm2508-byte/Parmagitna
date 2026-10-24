-- =============================================================================
-- PaperERP Enterprise — V2__corrected_kernel_schema.sql
-- Architecture Corrections:
-- 1. Enforces Composite Foreign Keys (tenant_id, id) for zero-trust tenant isolation
-- 2. Introduces slitting_runs aggregate table for mass balance reconciliation
-- 3. Upgrades inventory_movements to support both Jumbo Rolls and Slit Child Rolls
-- =============================================================================

-- 1. COMPOSITE UNIQUE CONSTRAINTS ON PARENT TABLES
ALTER TABLE plants ADD CONSTRAINT uq_plants_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE machines ADD CONSTRAINT uq_machines_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE paper_grades ADD CONSTRAINT uq_paper_grades_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE warehouses ADD CONSTRAINT uq_warehouses_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE warehouse_locations ADD CONSTRAINT uq_locations_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE jumbo_rolls ADD CONSTRAINT uq_jumbo_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE slit_rolls ADD CONSTRAINT uq_slit_tenant_id UNIQUE (tenant_id, id);

-- 2. COMPOSITE FOREIGN KEYS ON USERS & MACHINES
ALTER TABLE users ADD CONSTRAINT fk_users_plant_comp 
    FOREIGN KEY (tenant_id, plant_id) REFERENCES plants(tenant_id, id) ON DELETE RESTRICT;
ALTER TABLE machines ADD CONSTRAINT fk_machines_plant_comp 
    FOREIGN KEY (tenant_id, plant_id) REFERENCES plants(tenant_id, id) ON DELETE RESTRICT;

-- 3. COMPOSITE FOREIGN KEYS ON JUMBO ROLLS
ALTER TABLE jumbo_rolls ADD CONSTRAINT fk_jumbo_plant_comp 
    FOREIGN KEY (tenant_id, plant_id) REFERENCES plants(tenant_id, id) ON DELETE RESTRICT;
ALTER TABLE jumbo_rolls ADD CONSTRAINT fk_jumbo_machine_comp 
    FOREIGN KEY (tenant_id, machine_id) REFERENCES machines(tenant_id, id) ON DELETE RESTRICT;
ALTER TABLE jumbo_rolls ADD CONSTRAINT fk_jumbo_grade_comp 
    FOREIGN KEY (tenant_id, paper_grade_id) REFERENCES paper_grades(tenant_id, id) ON DELETE RESTRICT;

-- 4. CREATE SLITTING_RUNS AGGREGATE TABLE
CREATE TABLE slitting_runs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    parent_jumbo_id UUID NOT NULL,
    machine_id UUID NOT NULL,
    operator_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    input_jumbo_weight_kg NUMERIC(10,2) NOT NULL CHECK (input_jumbo_weight_kg > 0),
    total_child_weight_kg NUMERIC(10,2) NOT NULL CHECK (total_child_weight_kg > 0),
    trim_waste_kg NUMERIC(10,2) NOT NULL DEFAULT 0.0 CHECK (trim_waste_kg >= 0),
    scale_variance_kg NUMERIC(10,2) NOT NULL DEFAULT 0.0,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('COMPLETED', 'ABORTED')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_slitting_runs_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_slitting_jumbo_comp FOREIGN KEY (tenant_id, parent_jumbo_id) REFERENCES jumbo_rolls(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_slitting_machine_comp FOREIGN KEY (tenant_id, machine_id) REFERENCES machines(tenant_id, id) ON DELETE RESTRICT
);

-- 5. LINK SLIT ROLLS TO SLITTING RUN AND ENFORCE COMPOSITE FKs
ALTER TABLE slit_rolls ADD COLUMN slitting_run_id UUID;
ALTER TABLE slit_rolls ADD CONSTRAINT fk_slit_run_comp 
    FOREIGN KEY (tenant_id, slitting_run_id) REFERENCES slitting_runs(tenant_id, id) ON DELETE RESTRICT;
ALTER TABLE slit_rolls ADD CONSTRAINT fk_slit_jumbo_comp 
    FOREIGN KEY (tenant_id, parent_jumbo_id) REFERENCES jumbo_rolls(tenant_id, id) ON DELETE RESTRICT;
ALTER TABLE slit_rolls ADD CONSTRAINT fk_slit_grade_comp 
    FOREIGN KEY (tenant_id, paper_grade_id) REFERENCES paper_grades(tenant_id, id) ON DELETE RESTRICT;

-- 6. UPGRADE INVENTORY MOVEMENTS TO SUPPORT BOTH JUMBO AND CHILD ROLLS
ALTER TABLE inventory_movements ADD COLUMN jumbo_roll_id UUID;
ALTER TABLE inventory_movements ALTER COLUMN roll_id DROP NOT NULL;
ALTER TABLE inventory_movements RENAME COLUMN roll_id TO slit_roll_id;

ALTER TABLE inventory_movements ADD CONSTRAINT fk_movement_jumbo_comp 
    FOREIGN KEY (tenant_id, jumbo_roll_id) REFERENCES jumbo_rolls(tenant_id, id) ON DELETE RESTRICT;
ALTER TABLE inventory_movements ADD CONSTRAINT fk_movement_slit_comp 
    FOREIGN KEY (tenant_id, slit_roll_id) REFERENCES slit_rolls(tenant_id, id) ON DELETE RESTRICT;

ALTER TABLE inventory_movements ADD CONSTRAINT chk_movement_target CHECK (
    (jumbo_roll_id IS NOT NULL AND slit_roll_id IS NULL) OR
    (jumbo_roll_id IS NULL AND slit_roll_id IS NOT NULL)
);

-- 7. INDEXES FOR PERFORMANCE
CREATE INDEX idx_slitting_parent ON slitting_runs(tenant_id, parent_jumbo_id);
CREATE INDEX idx_slit_run_id ON slit_rolls(tenant_id, slitting_run_id);
CREATE INDEX idx_movements_jumbo ON inventory_movements(tenant_id, jumbo_roll_id);
