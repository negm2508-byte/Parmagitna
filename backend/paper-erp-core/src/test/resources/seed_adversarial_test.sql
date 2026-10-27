-- ==============================================================================
-- PaperERP Enterprise: Adversarial Cross-Tenant Seed Data for Security Tests
-- ==============================================================================

-- 1. Adversary Tenant B
INSERT INTO tenants (id, code, name_ar, name_en, is_active)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'RIVAL_PAPER',
    'شركة المنافس للورق',
    'Rival Paper Mill',
    true
) ON CONFLICT (id) DO NOTHING;

-- 2. Adversary Plant B
INSERT INTO plants (id, tenant_id, code, name_ar, name_en, is_active)
VALUES (
    'b0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000002',
    'PLANT-RIVAL',
    'مصنع المنافس',
    'Rival Plant',
    true
) ON CONFLICT (tenant_id, id) DO NOTHING;

-- 3. Adversary User (Username: rival_operator, Password: Admin@123)
INSERT INTO users (id, tenant_id, default_plant_id, username, email, password_hash, full_name_ar, role, is_active)
VALUES (
    '90000000-0000-0000-0000-000000000099',
    'a0000000-0000-0000-0000-000000000002',
    'b0000000-0000-0000-0000-000000000002',
    'rival_operator',
    'rival@rivalpaper.com',
    '$2b$12$eX23FbKFC0.x78TRp0Mj2eLVhWBpSeG1do31W8TsqHREGRI7uSV9.',
    'مشغل مصنع المنافس',
    'PRODUCTION_OPERATOR',
    true
) ON CONFLICT (tenant_id, id) DO NOTHING;
