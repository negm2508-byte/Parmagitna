-- ==============================================================================
-- PaperERP Enterprise: Keyset Pagination Equal-Timestamp & EXPLAIN Verification
-- ==============================================================================

BEGIN;
SELECT set_config('app.current_tenant_id', '11111111-1111-1111-1111-111111111111', true);

-- 1. Setup Tenant and prerequisite entities
INSERT INTO tenants (id, code, name_ar, name_en, is_active)
VALUES ('11111111-1111-1111-1111-111111111111', 'TENANT_A', 'مصنع الورق الحديث', 'Modern Paper Mill', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO plants (id, tenant_id, code, name_ar, name_en, is_active)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'PLANT-A', 'مصنع أ', 'Plant A', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO machines (id, tenant_id, plant_id, code, name_ar, machine_type, max_deckle_width_mm, min_trim_width_mm)
VALUES ('aaaaaaaa-bbbb-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'PM-1', 'ماكينة 1', 'PAPER_MACHINE', 2800.00, 100.00)
ON CONFLICT (id) DO NOTHING;

INSERT INTO paper_grades (id, tenant_id, code, name_ar, gsm_target, gsm_min, gsm_max, caliper_target_um, moisture_target_pct, burst_index_target)
VALUES ('aaaaaaaa-cccc-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'FLUTING-125', 'فلوتنج 125', 125.00, 120.00, 130.00, 160.00, 7.50, 2.50)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, tenant_id, default_plant_id, username, email, password_hash, full_name_ar, role)
VALUES ('aaaaaaaa-eeee-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'actor_keyset', 'actor@papermill.com', 'hash123', 'مشغل المفاتيح', 'OPERATOR')
ON CONFLICT (id) DO NOTHING;

-- 2. Insert dummy jumbo roll for foreign key
INSERT INTO jumbo_rolls (
    id, tenant_id, plant_id, machine_id, grade_id, serial_number, barcode,
    gross_weight_kg, tare_weight_kg, net_weight_kg, deckle_width_mm, created_by
) VALUES (
    'aaaaaaaa-dddd-aaaa-aaaa-aaaaaaaaaaaa',
    '11111111-1111-1111-1111-111111111111',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'aaaaaaaa-bbbb-aaaa-aaaa-aaaaaaaaaaaa',
    'aaaaaaaa-cccc-aaaa-aaaa-aaaaaaaaaaaa',
    'JR-TEST-KEYSET',
    'BAR-KEYSET-001',
    2500.00, 100.00, 2400.00, 2800.00,
    'aaaaaaaa-eeee-aaaa-aaaa-aaaaaaaaaaaa'
) ON CONFLICT (id) DO NOTHING;

-- 3. Insert 5,000 ledger rows, where groups of 5 rows share identical movement_timestamps
INSERT INTO inventory_ledger (
    id, tenant_id, plant_id, movement_type, roll_type, jumbo_roll_id,
    quantity_change, weight_kg, actor_id, movement_timestamp, created_at
)
SELECT 
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'RECEIPT',
    'JUMBO_ROLL',
    'aaaaaaaa-dddd-aaaa-aaaa-aaaaaaaaaaaa',
    1.00,
    2400.00,
    'aaaaaaaa-eeee-aaaa-aaaa-aaaaaaaaaaaa',
    '2026-10-26 10:00:00+00'::timestamptz - ((n / 5) || ' minutes')::interval,
    NOW()
FROM generate_series(1, 5000) AS n;

COMMIT;

-- Verify keyset pagination across equal timestamps
BEGIN;
SELECT set_config('app.current_tenant_id', '11111111-1111-1111-1111-111111111111', true);

-- Step 1: Page 1 (Limit 5)
CREATE TEMP TABLE page_1 AS
SELECT id, movement_timestamp
FROM inventory_ledger
WHERE tenant_id = '11111111-1111-1111-1111-111111111111'
  AND plant_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
ORDER BY movement_timestamp DESC, id DESC
LIMIT 5;

SELECT 'PAGE 1 RESULTS' as title, id, movement_timestamp FROM page_1;

-- Extract last row cursor from Page 1
CREATE TEMP TABLE cursor_val AS
SELECT movement_timestamp AS cur_ts, id AS cur_id
FROM page_1
ORDER BY movement_timestamp ASC, id ASC
LIMIT 1;

-- Step 2: Page 2 (Limit 5) using keyset cursor condition with tie-breaking
CREATE TEMP TABLE page_2 AS
SELECT l.id, l.movement_timestamp
FROM inventory_ledger l, cursor_val c
WHERE l.tenant_id = '11111111-1111-1111-1111-111111111111'
  AND l.plant_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
  AND (l.movement_timestamp < c.cur_ts OR (l.movement_timestamp = c.cur_ts AND l.id < c.cur_id))
ORDER BY l.movement_timestamp DESC, l.id DESC
LIMIT 5;

SELECT 'PAGE 2 RESULTS' as title, id, movement_timestamp FROM page_2;

-- Step 3: Check duplicates between Page 1 and Page 2 (must be exactly 0)
SELECT COUNT(*) AS duplicate_count_between_pages
FROM (
    SELECT id FROM page_1
    INTERSECT
    SELECT id FROM page_2
) d;

-- Step 4: Check ordering anomaly count (must be exactly 0)
SELECT COUNT(*) AS ordering_anomaly_count
FROM page_1 p1, page_2 p2
WHERE (p2.movement_timestamp > p1.movement_timestamp)
   OR (p2.movement_timestamp = p1.movement_timestamp AND p2.id >= p1.id);

-- Step 5: Keyset query execution plan with EXPLAIN (ANALYZE, BUFFERS)
EXPLAIN (ANALYZE, BUFFERS)
SELECT l.id, l.movement_timestamp, l.quantity_change
FROM inventory_ledger l
WHERE l.tenant_id = '11111111-1111-1111-1111-111111111111'
  AND l.plant_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
  AND (l.movement_timestamp < '2026-10-26 09:00:00+00'::timestamptz 
       OR (l.movement_timestamp = '2026-10-26 09:00:00+00'::timestamptz AND l.id < 'ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid))
ORDER BY l.movement_timestamp DESC, l.id DESC
LIMIT 20;

COMMIT;
