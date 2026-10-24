-- ==============================================================================
-- PaperERP Enterprise — Supabase PostgreSQL Schema
-- Run this in your Supabase SQL Editor (https://supabase.com/dashboard/project/_/sql)
-- ==============================================================================

-- 1. Users & Factory Operators Table
CREATE TABLE IF NOT EXISTS public.users (
    id TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'CHIEF_OPERATOR',
    plant TEXT NOT NULL DEFAULT 'مصنع مصر للورق والكرتون • وحدة PM1',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Jumbo Mother Rolls Table
CREATE TABLE IF NOT EXISTS public.jumbo_rolls (
    id TEXT PRIMARY KEY,
    serial_number TEXT UNIQUE NOT NULL,
    epc_rfid TEXT,
    grade TEXT NOT NULL,
    target_gsm NUMERIC(6, 2) NOT NULL,
    deckle_width_mm NUMERIC(8, 2) NOT NULL,
    gross_weight_kg NUMERIC(8, 2) NOT NULL,
    core_tare_kg NUMERIC(6, 2) NOT NULL,
    net_weight_kg NUMERIC(8, 2) NOT NULL,
    machine_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE_WIP',
    operator_name TEXT NOT NULL,
    shift TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Child Slit Rolls Table
CREATE TABLE IF NOT EXISTS public.slit_rolls (
    id TEXT PRIMARY KEY,
    parent_jumbo_serial TEXT NOT NULL,
    serial_number TEXT UNIQUE NOT NULL,
    sequence INTEGER NOT NULL,
    width_mm NUMERIC(8, 2) NOT NULL,
    net_weight_kg NUMERIC(8, 2) NOT NULL,
    gross_weight_kg NUMERIC(8, 2) NOT NULL,
    grade TEXT NOT NULL,
    gsm NUMERIC(6, 2) NOT NULL,
    core_mm INTEGER NOT NULL DEFAULT 76,
    diameter_mm NUMERIC(8, 2) NOT NULL DEFAULT 1250,
    status TEXT NOT NULL DEFAULT 'PRODUCED',
    location TEXT NOT NULL,
    operator_name TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. Quality Control Inspections Table
CREATE TABLE IF NOT EXISTS public.qc_inspections (
    id TEXT PRIMARY KEY,
    roll_serial TEXT NOT NULL,
    grade TEXT NOT NULL,
    actual_gsm NUMERIC(6, 2) NOT NULL,
    target_gsm NUMERIC(6, 2) NOT NULL,
    moisture_pct NUMERIC(5, 2) NOT NULL,
    caliper_microns NUMERIC(6, 2) NOT NULL,
    burst_kpa NUMERIC(6, 2) NOT NULL,
    cobb60 NUMERIC(6, 2) NOT NULL,
    disposition TEXT NOT NULL DEFAULT 'RELEASED',
    inspector TEXT NOT NULL,
    lab_notes TEXT,
    inspected_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. Immutable Stock Movement Ledger Table
CREATE TABLE IF NOT EXISTS public.stock_movements (
    id TEXT PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    roll_serial TEXT NOT NULL,
    roll_category TEXT NOT NULL,
    movement_type TEXT NOT NULL,
    net_weight_kg NUMERIC(8, 2) NOT NULL,
    from_location TEXT NOT NULL,
    to_location TEXT NOT NULL,
    actor TEXT NOT NULL,
    reason TEXT NOT NULL,
    audit_hash TEXT NOT NULL
);

-- 6. Enable Row Level Security (RLS) & Public Anonymous Read/Write for demo
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.jumbo_rolls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.slit_rolls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.qc_inspections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stock_movements ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public access to users" ON public.users FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public access to jumbo_rolls" ON public.jumbo_rolls FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public access to slit_rolls" ON public.slit_rolls FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public access to qc_inspections" ON public.qc_inspections FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public access to stock_movements" ON public.stock_movements FOR ALL USING (true) WITH CHECK (true);
