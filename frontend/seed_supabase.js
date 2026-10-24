import { createClient } from '@supabase/supabase-js';

const url = 'https://vjsdkmnkzadyefkftaeh.supabase.co';
const key = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZqc2RrbW5remFkeWVma2Z0YWVoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI4NjQ4NzcsImV4cCI6MjEwODQ0MDg3N30.qG-fWgtQRAmwZg7kjCWWc8S_TM_W2IKZjYr5b-43fNE';

const supabase = createClient(url, key);

async function seed() {
  console.log('Seeding Supabase tables...');

  // 1. Users
  const { error: userErr } = await supabase.from('users').upsert([
    {
      id: 'usr-1',
      email: 'mmycomputar12@gmail.com',
      full_name: 'م. أحمد محمود',
      role: 'CHIEF_OPERATOR',
      plant: 'مصنع مصر للورق والكرتون • وحدة PM1'
    },
    {
      id: 'usr-2',
      email: 'qc.inspector@cellupaper.com',
      full_name: 'ك. مصطفى إبراهيم',
      role: 'QC_INSPECTOR',
      plant: 'مختبر فحص الجودة المعتمد ISO 187'
    },
    {
      id: 'usr-3',
      email: 'mill.manager@cellupaper.com',
      full_name: 'د. طارق السعيد',
      role: 'MILL_MANAGER',
      plant: 'الإدارة العامة لتشغيل المصانع'
    }
  ]);
  console.log('Users seeded:', userErr || 'SUCCESS');

  // 2. Jumbo Rolls
  const { error: jmbErr } = await supabase.from('jumbo_rolls').upsert([
    {
      id: 'jmb-1',
      serial_number: 'JMB-2026-088',
      epc_rfid: '0x98EF44B2004481A',
      grade: 'Testliner Grade-A',
      target_gsm: 150,
      deckle_width_mm: 2400,
      gross_weight_kg: 3002,
      core_tare_kg: 2,
      net_weight_kg: 3000,
      machine_id: 'خط إنتاج الورق PM1',
      status: 'ACTIVE_WIP',
      operator_name: 'م. أحمد محمود',
      shift: 'وردية المساء (15:00 - 23:00)'
    },
    {
      id: 'jmb-2',
      serial_number: 'JMB-2026-089',
      epc_rfid: '0x98EF44B2004482B',
      grade: 'Fluting Heavy Craft',
      target_gsm: 140,
      deckle_width_mm: 2450,
      gross_weight_kg: 3154,
      core_tare_kg: 4,
      net_weight_kg: 3150,
      machine_id: 'خط إنتاج الورق PM1',
      status: 'ACTIVE_WIP',
      operator_name: 'م. أحمد محمود',
      shift: 'وردية المساء (15:00 - 23:00)'
    }
  ]);
  console.log('Jumbo rolls seeded:', jmbErr || 'SUCCESS');

  // 3. Slit Rolls
  const { error: sltErr } = await supabase.from('slit_rolls').upsert([
    {
      id: 'slt-1',
      parent_jumbo_serial: 'JMB-2026-088',
      serial_number: 'SLT-01-JMB-2026-088',
      sequence: 1,
      width_mm: 1000,
      net_weight_kg: 1245,
      gross_weight_kg: 1247,
      grade: 'Testliner Grade-A',
      gsm: 150,
      core_mm: 76,
      diameter_mm: 1250,
      status: 'QC_PASSED',
      location: 'مستودع A - ممر 04 - رصة 2',
      operator_name: 'م. أحمد محمود'
    },
    {
      id: 'slt-2',
      parent_jumbo_serial: 'JMB-2026-088',
      serial_number: 'SLT-02-JMB-2026-088',
      sequence: 2,
      width_mm: 1000,
      net_weight_kg: 1245,
      gross_weight_kg: 1247,
      grade: 'Testliner Grade-A',
      gsm: 150,
      core_mm: 76,
      diameter_mm: 1250,
      status: 'QC_PASSED',
      location: 'مستودع A - ممر 04 - رصة 2',
      operator_name: 'م. أحمد محمود'
    },
    {
      id: 'slt-3',
      parent_jumbo_serial: 'JMB-2026-088',
      serial_number: 'SLT-03-JMB-2026-088',
      sequence: 3,
      width_mm: 350,
      net_weight_kg: 435,
      gross_weight_kg: 436,
      grade: 'Testliner Grade-A',
      gsm: 150,
      core_mm: 76,
      diameter_mm: 1250,
      status: 'PRODUCED',
      location: 'منطقة سكاكين التقطيع SLT-04',
      operator_name: 'م. أحمد محمود'
    }
  ]);
  console.log('Slit rolls seeded:', sltErr || 'SUCCESS');

  // 4. QC Tests
  const { error: qcErr } = await supabase.from('qc_inspections').upsert([
    {
      id: 'qc-1',
      roll_serial: 'SLT-01-JMB-2026-088',
      grade: 'Testliner Grade-A',
      actual_gsm: 150.4,
      target_gsm: 150,
      moisture_pct: 7.2,
      caliper_microns: 220,
      burst_kpa: 395,
      cobb60: 32,
      disposition: 'RELEASED',
      inspector: 'ك. مصطفى إبراهيم',
      lab_notes: 'عينات مطابقة للمواصفة القياسية المصرية والعالمية ISO 187'
    },
    {
      id: 'qc-2',
      roll_serial: 'SLT-02-JMB-2026-088',
      grade: 'Testliner Grade-A',
      actual_gsm: 149.8,
      target_gsm: 150,
      moisture_pct: 7.4,
      caliper_microns: 218,
      burst_kpa: 390,
      cobb60: 34,
      disposition: 'RELEASED',
      inspector: 'ك. مصطفى إبراهيم',
      lab_notes: 'فحص ميكانيكي سليم وخالي من عيوب الوصلات'
    }
  ]);
  console.log('QC tests seeded:', qcErr || 'SUCCESS');

  // 5. Stock Movements
  const { error: movErr } = await supabase.from('stock_movements').upsert([
    {
      id: 'mov-1',
      roll_serial: 'JMB-2026-088',
      roll_category: 'جامبو',
      movement_type: 'PRODUCTION_RECEIPT',
      net_weight_kg: 3000,
      from_location: 'ماكينة الورق PM1 (Pope Reel)',
      to_location: 'منطقة الرولات الجامبو WIP',
      actor: 'م. أحمد محمود (مشغل رئيسي)',
      reason: 'خروج رول الجامبو من خط الورق ومطابقة ميزان البسكول',
      audit_hash: '0x8f4d92a10c7e'
    },
    {
      id: 'mov-2',
      roll_serial: 'JMB-2026-088',
      roll_category: 'جامبو',
      movement_type: 'STATUS_CHANGE',
      net_weight_kg: 3000,
      from_location: 'منطقة الرولات الجامبو WIP',
      to_location: 'محطة سكاكين التقطيع SLT-04',
      actor: 'م. أحمد محمود (مشغل رئيسي)',
      reason: 'تحميل الرول على ستاند التغذية وبدء أمر التقطيع الدائري',
      audit_hash: '0x3b1c8f420e9a'
    },
    {
      id: 'mov-3',
      roll_serial: 'SLT-01-JMB-2026-088',
      roll_category: 'فرعي',
      movement_type: 'PRODUCTION_RECEIPT',
      net_weight_kg: 1245,
      from_location: 'محطة سكاكين التقطيع SLT-04',
      to_location: 'محطة فحص الجودة QC Lab',
      actor: 'م. أحمد محمود (مشغل رئيسي)',
      reason: 'توليد رول مقصوص عرض 1000 مم وتثبيت ماسورة 76 مم',
      audit_hash: '0x4a92c0183b5e'
    },
    {
      id: 'mov-4',
      roll_serial: 'SLT-01-JMB-2026-088',
      roll_category: 'فرعي',
      movement_type: 'LOCATION_TRANSFER',
      net_weight_kg: 1245,
      from_location: 'محطة فحص الجودة QC Lab',
      to_location: 'مستودع A - ممر 04 - رصة 2',
      actor: 'ك. مصطفى إبراهيم (مفتش جودة)',
      reason: 'اعتماد الفحص المخبري وتخزين الرول كمنتج تام صالح للشحن',
      audit_hash: '0x7e29a41b5d80'
    }
  ]);
  console.log('Stock movements seeded:', movErr || 'SUCCESS');
}

seed();
