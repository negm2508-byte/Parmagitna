-- ==============================================================================
-- PaperERP Enterprise: Keyset Pagination Index EXPLAIN (ANALYZE, BUFFERS)
-- ==============================================================================

BEGIN;
SELECT set_config('app.current_tenant_id', '11111111-1111-1111-1111-111111111111', true);

-- Insert dummy jumbo roll for foreign key
INSERT INTO jumbo_rolls (
    id, tenant_id, plant_id, machine_id, grade_id, serial_number, barcode,
    gross_weight_kg, tare_weight_kg, net_weight_kg, deckle_width_mm, created_by
) VALUES (
    'aaaaaaaa-dddd-aaaa-aaaa-aaaaaaaaaaaa',
    '11111111-1111-1111-1111-111111111111',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'aaaaaaaa-bbbb-aaaa-aaaa-aaaaaaaaaaaa',
    'aaaaaaaa-cccc-aaaa-aaaa-aaaaaaaaaaaa',
    'JR-TEST-001',
    'BAR-001',
    2500.00, 100.00, 2400.00, 2800.00,
    'aaaaaaaa-eeee-aaaa-aaaa-aaaaaaaaaaaa'
) ON CONFLICT (id) DO NOTHING;

-- Insert 1000 representative ledger rows
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
    NOW() - (n || ' minutes')::interval,
    NOW() - (n || ' minutes')::interval
FROM generate_series(1, 1000) AS n;

COMMIT;

-- Keyset pagination query with EXPLAIN (ANALYZE, BUFFERS)
BEGIN;
SELECT set_config('app.current_tenant_id', '11111111-1111-1111-1111-111111111111', true);

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, tenant_id, plant_id, movement_timestamp, quantity_change, weight_kg
FROM inventory_ledger
WHERE tenant_id = '11111111-1111-1111-1111-111111111111'
  AND plant_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
  AND movement_timestamp < NOW() - INTERVAL '10 minutes'
ORDER BY movement_timestamp DESC, id DESC
LIMIT 20;

COMMIT;
