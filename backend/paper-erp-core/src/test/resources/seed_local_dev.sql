-- ==============================================================================
-- PaperERP Enterprise: Local Development & Verification Seed Data
-- ==============================================================================

-- 1. Tenant
INSERT INTO tenants (id, code, name_ar, name_en, is_active)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'CELLUPAPER',
    'شركة تصنيع الورق والكرتون الحديثة',
    'CelluPaper Industrial Co.',
    true
) ON CONFLICT (id) DO NOTHING;

-- 2. Plant
INSERT INTO plants (id, tenant_id, code, name_ar, name_en, is_active)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'PLANT-01',
    'مصنع 6 أكتوبر الرئيسي',
    '6th of October Main Mill',
    true
) ON CONFLICT (tenant_id, id) DO NOTHING;

-- 3. Machines
INSERT INTO machines (id, tenant_id, plant_id, code, name_ar, machine_type, max_deckle_width_mm, min_trim_width_mm, is_active)
VALUES 
(
    'c0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'PM-01',
    'خط إنتاج الورق PM1',
    'PAPER_MACHINE',
    2500.00,
    30.00,
    true
),
(
    'c0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'SR-01',
    'ماكينة التقطيع وإعادة اللف SR1',
    'SLITTER_REWINDER',
    2500.00,
    30.00,
    true
) ON CONFLICT (tenant_id, id) DO NOTHING;

-- 4. Paper Grades
INSERT INTO paper_grades (id, tenant_id, code, name_ar, gsm_target, gsm_min, gsm_max, caliper_target_um, moisture_target_pct, burst_index_target, is_active)
VALUES (
    'd0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'TESTLINER-150',
    'تست لاينر فاخر 150 جرام',
    150.00,
    145.00,
    155.00,
    210.00,
    7.50,
    2.80,
    true
) ON CONFLICT (tenant_id, id) DO NOTHING;

-- 5. Core Specification
INSERT INTO core_specifications (id, tenant_id, code, diameter_mm, tare_weight_per_m_kg, is_active)
VALUES (
    'e0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'CORE-76MM',
    76.20,
    1.2500,
    true
) ON CONFLICT (tenant_id, id) DO NOTHING;

-- 6. Warehouse & Locations
INSERT INTO warehouses (id, tenant_id, plant_id, code, name_ar, is_active)
VALUES (
    'f0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'WH-MAIN',
    'مخزن الإنتاج التام الرئيسي',
    true
) ON CONFLICT (tenant_id, id) DO NOTHING;

INSERT INTO warehouse_locations (id, tenant_id, warehouse_id, code, aisle, row_number, is_active)
VALUES 
(
    'f0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'f0000000-0000-0000-0000-000000000001',
    'LOC-A1',
    'A',
    '01',
    true
),
(
    'f0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'f0000000-0000-0000-0000-000000000001',
    'LOC-B2',
    'B',
    '02',
    true
) ON CONFLICT (tenant_id, id) DO NOTHING;

-- 7. Users (Password: Admin@123)
-- Hash: $2b$12$eX23FbKFC0.x78TRp0Mj2eLVhWBpSeG1do31W8TsqHREGRI7uSV9.
INSERT INTO users (id, tenant_id, default_plant_id, username, email, password_hash, full_name_ar, role, is_active)
VALUES 
(
    '90000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'admin',
    'admin@cellupaper.com',
    '$2b$12$eX23FbKFC0.x78TRp0Mj2eLVhWBpSeG1do31W8TsqHREGRI7uSV9.',
    'مدير النظام العام',
    'ADMIN',
    true
),
(
    '90000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'operator',
    'operator@cellupaper.com',
    '$2b$12$eX23FbKFC0.x78TRp0Mj2eLVhWBpSeG1do31W8TsqHREGRI7uSV9.',
    'م. أحمد محمود (مشغل خط إنتاج)',
    'PRODUCTION_OPERATOR',
    true
),
(
    '90000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'inspector',
    'qc@cellupaper.com',
    '$2b$12$eX23FbKFC0.x78TRp0Mj2eLVhWBpSeG1do31W8TsqHREGRI7uSV9.',
    'ك. مصطفى إبراهيم (مفتش جودة معتمد)',
    'QC_INSPECTOR',
    true
),
(
    '90000000-0000-0000-0000-000000000004',
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'warehouse',
    'warehouse@cellupaper.com',
    '$2b$12$eX23FbKFC0.x78TRp0Mj2eLVhWBpSeG1do31W8TsqHREGRI7uSV9.',
    'أ. محمود عبد الرحمن (أمين المستودع)',
    'WAREHOUSE_KEEPER',
    true
) ON CONFLICT (tenant_id, id) DO NOTHING;
