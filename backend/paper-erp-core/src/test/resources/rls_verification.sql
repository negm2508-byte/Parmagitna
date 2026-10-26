-- ==============================================================================
-- PaperERP Enterprise: PostgreSQL 16 RLS & Keyset Performance Verification
-- ==============================================================================

-- 1. Setup Tenants (as superuser/admin)
INSERT INTO tenants (id, code, name_ar, name_en, is_active)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'TENANT_A', 'مصنع الورق الحديث (المستأجر أ)', 'Modern Paper Mill (Tenant A)', true),
    ('22222222-2222-2222-2222-222222222222', 'TENANT_B', 'شركة الأهرام للكرتون (المستأجر ب)', 'Ahram Carton Mill (Tenant B)', true)
ON CONFLICT (id) DO NOTHING;

-- 2. TEST 1: Missing tenant context must return 0 rows (Fail-Closed)
BEGIN;
SELECT 'TEST 1: Missing tenant context must return 0 rows' AS test_name;
SELECT count(*) AS should_be_zero FROM plants;
COMMIT;

-- 3. TEST 2: Transaction with Tenant A context creates and reads Tenant A records
BEGIN;
SELECT set_config('app.current_tenant_id', '11111111-1111-1111-1111-111111111111', true);
SELECT 'TEST 2: Insert and read under Tenant A' AS test_name;

INSERT INTO plants (id, tenant_id, code, name_ar, name_en, is_active)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'PLANT-A', 'مصنع أ', 'Plant A', true)
ON CONFLICT (id) DO UPDATE SET name_ar = EXCLUDED.name_ar;

INSERT INTO machines (id, tenant_id, plant_id, code, name_ar, machine_type, max_deckle_width_mm, min_trim_width_mm)
VALUES ('aaaaaaaa-bbbb-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'PM-1', 'ماكينة 1', 'PAPER_MACHINE', 2800.00, 100.00)
ON CONFLICT (id) DO UPDATE SET name_ar = EXCLUDED.name_ar;

SELECT count(*) AS tenant_a_plant_count FROM plants;
COMMIT;

-- 4. TEST 3: Tenant B context cannot see Tenant A records (Cross-tenant leak prevention)
BEGIN;
SELECT set_config('app.current_tenant_id', '22222222-2222-2222-2222-222222222222', true);
SELECT 'TEST 3: Tenant B cannot see Tenant A records' AS test_name;
SELECT count(*) AS tenant_b_sees_tenant_a_plants_should_be_zero FROM plants WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
SELECT count(*) AS tenant_b_plants_total_before_insert FROM plants;

-- Insert Tenant B's own plant
INSERT INTO plants (id, tenant_id, code, name_ar, name_en, is_active)
VALUES ('bbbbbbbb-aaaa-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222', 'PLANT-B', 'مصنع ب', 'Plant B', true)
ON CONFLICT (id) DO UPDATE SET name_ar = EXCLUDED.name_ar;

SELECT count(*) AS tenant_b_plants_total_after_insert FROM plants;
COMMIT;

-- 5. TEST 4: Tenant A cannot see Tenant B records
BEGIN;
SELECT set_config('app.current_tenant_id', '11111111-1111-1111-1111-111111111111', true);
SELECT 'TEST 4: Tenant A cannot see Tenant B records' AS test_name;
SELECT count(*) AS tenant_a_sees_tenant_b_plants_should_be_zero FROM plants WHERE id = 'bbbbbbbb-aaaa-bbbb-bbbb-bbbbbbbbbbbb';
COMMIT;

-- 6. TEST 5: Connection reused after transaction automatically reverts (is_local=true verification)
BEGIN;
-- Start fresh transaction without calling set_config:
SELECT 'TEST 5: Fresh transaction on same connection has no leaked context' AS test_name;
SELECT count(*) AS connection_reused_should_be_zero FROM plants;
COMMIT;
