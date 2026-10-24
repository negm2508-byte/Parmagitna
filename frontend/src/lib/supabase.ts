import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { UserProfile, JumboRoll, ChildSlitRoll, QCTest, StockMovement } from '../types';

export const DEFAULT_SUPABASE_URL = 'https://vjsdkmnkzadyefkftaeh.supabase.co';
export const DEFAULT_SUPABASE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZqc2RrbW5remFkeWVma2Z0YWVoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI4NjQ4NzcsImV4cCI6MjEwODQ0MDg3N30.qG-fWgtQRAmwZg7kjCWWc8S_TM_W2IKZjYr5b-43fNE';

const DEFAULT_USERS: UserProfile[] = [
  {
    id: 'usr-1',
    email: 'mmycomputar12@gmail.com',
    fullName: 'م. أحمد محمود',
    role: 'CHIEF_OPERATOR',
    plant: 'مصنع مصر للورق والكرتون • وحدة PM1',
    createdAt: '2026-10-24 14:00:00'
  },
  {
    id: 'usr-2',
    email: 'qc.inspector@cellupaper.com',
    fullName: 'ك. مصطفى إبراهيم',
    role: 'QC_INSPECTOR',
    plant: 'مختبر فحص الجودة المعتمد ISO 187',
    createdAt: '2026-10-24 14:00:00'
  },
  {
    id: 'usr-3',
    email: 'mill.manager@cellupaper.com',
    fullName: 'د. طارق السعيد',
    role: 'MILL_MANAGER',
    plant: 'الإدارة العامة لتشغيل المصانع',
    createdAt: '2026-10-24 14:00:00'
  }
];

const DEFAULT_JUMBOS: JumboRoll[] = [
  {
    id: 'jmb-1',
    serialNumber: 'JMB-2026-088',
    epcRfid: '0x98EF44B2004481A',
    grade: 'Testliner Grade-A',
    targetGsm: 150,
    deckleWidthMm: 2400,
    grossWeightKg: 3002,
    coreTareKg: 2,
    netWeightKg: 3000,
    machineId: 'خط إنتاج الورق PM1',
    status: 'ACTIVE_WIP',
    operatorName: 'م. أحمد محمود',
    shift: 'وردية المساء (15:00 - 23:00)',
    createdAt: '2026-10-24 14:15:00'
  },
  {
    id: 'jmb-2',
    serialNumber: 'JMB-2026-089',
    epcRfid: '0x98EF44B2004482B',
    grade: 'Fluting Heavy Craft',
    targetGsm: 140,
    deckleWidthMm: 2450,
    grossWeightKg: 3154,
    coreTareKg: 4,
    netWeightKg: 3150,
    machineId: 'خط إنتاج الورق PM1',
    status: 'ACTIVE_WIP',
    operatorName: 'م. أحمد محمود',
    shift: 'وردية المساء (15:00 - 23:00)',
    createdAt: '2026-10-24 16:30:00'
  }
];

const DEFAULT_CHILD_ROLLS: ChildSlitRoll[] = [
  {
    id: 'slt-1',
    parentJumboSerial: 'JMB-2026-088',
    serialNumber: 'SLT-01-JMB-2026-088',
    sequence: 1,
    widthMm: 1000,
    netWeightKg: 1245,
    grossWeightKg: 1247,
    grade: 'Testliner Grade-A',
    gsm: 150,
    coreMm: 76,
    diameterMm: 1250,
    status: 'QC_PASSED',
    location: 'مستودع A - ممر 04 - رصة 2',
    operatorName: 'م. أحمد محمود',
    createdAt: '2026-10-24 14:45:00'
  },
  {
    id: 'slt-2',
    parentJumboSerial: 'JMB-2026-088',
    serialNumber: 'SLT-02-JMB-2026-088',
    sequence: 2,
    widthMm: 1000,
    netWeightKg: 1245,
    grossWeightKg: 1247,
    grade: 'Testliner Grade-A',
    gsm: 150,
    coreMm: 76,
    diameterMm: 1250,
    status: 'QC_PASSED',
    location: 'مستودع A - ممر 04 - رصة 2',
    operatorName: 'م. أحمد محمود',
    createdAt: '2026-10-24 14:45:00'
  },
  {
    id: 'slt-3',
    parentJumboSerial: 'JMB-2026-088',
    serialNumber: 'SLT-03-JMB-2026-088',
    sequence: 3,
    widthMm: 350,
    netWeightKg: 435,
    grossWeightKg: 436,
    grade: 'Testliner Grade-A',
    gsm: 150,
    coreMm: 76,
    diameterMm: 1250,
    status: 'PRODUCED',
    location: 'منطقة سكاكين التقطيع SLT-04',
    operatorName: 'م. أحمد محمود',
    createdAt: '2026-10-24 14:45:00'
  }
];

const DEFAULT_QC_TESTS: QCTest[] = [
  {
    id: 'qc-1',
    rollSerial: 'SLT-01-JMB-2026-088',
    grade: 'Testliner Grade-A',
    actualGsm: 150.4,
    targetGsm: 150,
    moisturePct: 7.2,
    caliperMicrons: 220,
    burstKpa: 395,
    cobb60: 32,
    disposition: 'RELEASED',
    inspector: 'ك. مصطفى إبراهيم',
    labNotes: 'عينات مطابقة للمواصفة القياسية المصرية والعالمية ISO 187',
    inspectedAt: '2026-10-24 15:10:00'
  },
  {
    id: 'qc-2',
    rollSerial: 'SLT-02-JMB-2026-088',
    grade: 'Testliner Grade-A',
    actualGsm: 149.8,
    targetGsm: 150,
    moisturePct: 7.4,
    caliperMicrons: 218,
    burstKpa: 390,
    cobb60: 34,
    disposition: 'RELEASED',
    inspector: 'ك. مصطفى إبراهيم',
    labNotes: 'فحص ميكانيكي سليم وخالي من عيوب الوصلات',
    inspectedAt: '2026-10-24 15:20:00'
  }
];

const DEFAULT_MOVEMENTS: StockMovement[] = [
  {
    id: 'mov-1',
    timestamp: '2026-10-24 14:15:00',
    rollSerial: 'JMB-2026-088',
    rollCategory: 'جامبو',
    movementType: 'PRODUCTION_RECEIPT',
    netWeightKg: 3000,
    fromLocation: 'ماكينة الورق PM1 (Pope Reel)',
    toLocation: 'منطقة الرولات الجامبو WIP',
    actor: 'م. أحمد محمود (مشغل رئيسي)',
    reason: 'خروج رول الجامبو من خط الورق ومطابقة ميزان البسكول',
    auditHash: '0x8f4d92a10c7e'
  },
  {
    id: 'mov-2',
    timestamp: '2026-10-24 14:35:00',
    rollSerial: 'JMB-2026-088',
    rollCategory: 'جامبو',
    movementType: 'STATUS_CHANGE',
    netWeightKg: 3000,
    fromLocation: 'منطقة الرولات الجامبو WIP',
    toLocation: 'محطة سكاكين التقطيع SLT-04',
    actor: 'م. أحمد محمود (مشغل رئيسي)',
    reason: 'تحميل الرول على ستاند التغذية وبدء أمر التقطيع الدائري',
    auditHash: '0x3b1c8f420e9a'
  },
  {
    id: 'mov-3',
    timestamp: '2026-10-24 14:45:00',
    rollSerial: 'SLT-01-JMB-2026-088',
    rollCategory: 'فرعي',
    movementType: 'PRODUCTION_RECEIPT',
    netWeightKg: 1245,
    fromLocation: 'محطة سكاكين التقطيع SLT-04',
    toLocation: 'محطة فحص الجودة QC Lab',
    actor: 'م. أحمد محمود (مشغل رئيسي)',
    reason: 'توليد رول مقصوص عرض 1000 مم وتثبيت ماسورة 76 مم',
    auditHash: '0x4a92c0183b5e'
  },
  {
    id: 'mov-4',
    timestamp: '2026-10-24 15:15:00',
    rollSerial: 'SLT-01-JMB-2026-088',
    rollCategory: 'فرعي',
    movementType: 'LOCATION_TRANSFER',
    netWeightKg: 1245,
    fromLocation: 'محطة فحص الجودة QC Lab',
    toLocation: 'مستودع A - ممر 04 - رصة 2',
    actor: 'ك. مصطفى إبراهيم (مفتش جودة)',
    reason: 'اعتماد الفحص المخبري وتخزين الرول كمنتج تام صالح للشحن',
    auditHash: '0x7e29a41b5d80'
  }
];

class DatabaseManager {
  private supabase: SupabaseClient | null = null;
  private url: string = '';
  private key: string = '';

  constructor() {
    this.loadConfig();
  }

  private loadConfig() {
    const env = (import.meta as any).env || {};
    const envUrl = env.VITE_SUPABASE_URL || '';
    const envKey = env.VITE_SUPABASE_ANON_KEY || '';
    const localUrl = localStorage.getItem('papererp_supabase_url') || '';
    const localKey = localStorage.getItem('papererp_supabase_key') || '';

    this.url = localUrl || envUrl || DEFAULT_SUPABASE_URL;
    this.key = localKey || envKey || DEFAULT_SUPABASE_KEY;

    if (this.url && this.key) {
      try {
        this.supabase = createClient(this.url, this.key);
      } catch (e) {
        console.warn('Supabase initialization warning:', e);
        this.supabase = null;
      }
    }
  }

  public isSupabaseConnected(): boolean {
    return !!(this.supabase && this.url && this.key);
  }

  public getSupabaseConfig(): { url: string; key: string } {
    return { url: this.url, key: this.key };
  }

  public setSupabaseConfig(url: string, key: string) {
    this.url = url.trim();
    this.key = key.trim();
    if (this.url && this.key) {
      localStorage.setItem('papererp_supabase_url', this.url);
      localStorage.setItem('papererp_supabase_key', this.key);
      this.supabase = createClient(this.url, this.key);
    } else {
      localStorage.removeItem('papererp_supabase_url');
      localStorage.removeItem('papererp_supabase_key');
      this.supabase = null;
    }
  }

  private getLocal<T>(key: string, defaultValue: T): T {
    try {
      const data = localStorage.getItem(`papererp_${key}`);
      return data ? JSON.parse(data) : defaultValue;
    } catch {
      return defaultValue;
    }
  }

  private setLocal<T>(key: string, value: T) {
    try {
      localStorage.setItem(`papererp_${key}`, JSON.stringify(value));
    } catch (e) {
      console.error('LocalStorage write error:', e);
    }
  }

  // --- Users & Auth ---
  public async getUsers(): Promise<UserProfile[]> {
    if (this.supabase) {
      try {
        const { data, error } = await this.supabase.from('users').select('*');
        if (!error && data && data.length > 0) {
          return data.map((d: any) => ({
            id: d.id,
            email: d.email,
            fullName: d.full_name,
            role: d.role,
            plant: d.plant,
            createdAt: d.created_at
          }));
        }
      } catch {}
    }
    return this.getLocal<UserProfile[]>('users', DEFAULT_USERS);
  }

  public async registerUser(user: Omit<UserProfile, 'id' | 'createdAt'>): Promise<UserProfile> {
    const newUser: UserProfile = {
      ...user,
      id: `usr-${Date.now().toString().slice(-5)}`,
      createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19)
    };

    if (this.supabase) {
      try {
        await this.supabase.from('users').insert([{
          id: newUser.id,
          email: newUser.email,
          full_name: newUser.fullName,
          role: newUser.role,
          plant: newUser.plant
        }]);
      } catch {}
    }

    const current = await this.getUsers();
    const updated = [newUser, ...current];
    this.setLocal('users', updated);
    return newUser;
  }

  // --- Jumbo Rolls ---
  public async getJumboRolls(): Promise<JumboRoll[]> {
    if (this.supabase) {
      try {
        const { data, error } = await this.supabase.from('jumbo_rolls').select('*').order('created_at', { ascending: false });
        if (!error && data && data.length > 0) {
          return data.map((d: any) => ({
            id: d.id,
            serialNumber: d.serial_number,
            epcRfid: d.epc_rfid,
            grade: d.grade,
            targetGsm: Number(d.target_gsm),
            deckleWidthMm: Number(d.deckle_width_mm),
            grossWeightKg: Number(d.gross_weight_kg),
            coreTareKg: Number(d.core_tare_kg),
            netWeightKg: Number(d.net_weight_kg),
            machineId: d.machine_id,
            status: d.status,
            operatorName: d.operator_name,
            shift: d.shift,
            createdAt: d.created_at
          }));
        }
      } catch {}
    }
    return this.getLocal<JumboRoll[]>('jumbo_rolls', DEFAULT_JUMBOS);
  }

  public async addJumboRoll(jumbo: Omit<JumboRoll, 'id' | 'createdAt'>): Promise<JumboRoll> {
    const newJumbo: JumboRoll = {
      ...jumbo,
      id: `jmb-${Date.now().toString().slice(-5)}`,
      createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19)
    };

    if (this.supabase) {
      try {
        await this.supabase.from('jumbo_rolls').insert([{
          id: newJumbo.id,
          serial_number: newJumbo.serialNumber,
          epc_rfid: newJumbo.epcRfid,
          grade: newJumbo.grade,
          target_gsm: newJumbo.targetGsm,
          deckle_width_mm: newJumbo.deckleWidthMm,
          gross_weight_kg: newJumbo.grossWeightKg,
          core_tare_kg: newJumbo.coreTareKg,
          net_weight_kg: newJumbo.netWeightKg,
          machine_id: newJumbo.machineId,
          status: newJumbo.status,
          operator_name: newJumbo.operatorName,
          shift: newJumbo.shift
        }]);
      } catch {}
    }

    const current = await this.getJumboRolls();
    this.setLocal('jumbo_rolls', [newJumbo, ...current]);
    return newJumbo;
  }

  // --- Child Slit Rolls ---
  public async getChildRolls(): Promise<ChildSlitRoll[]> {
    if (this.supabase) {
      try {
        const { data, error } = await this.supabase.from('slit_rolls').select('*').order('created_at', { ascending: false });
        if (!error && data && data.length > 0) {
          return data.map((d: any) => ({
            id: d.id,
            parentJumboSerial: d.parent_jumbo_serial,
            serialNumber: d.serial_number,
            sequence: d.sequence,
            widthMm: Number(d.width_mm),
            netWeightKg: Number(d.net_weight_kg),
            grossWeightKg: Number(d.gross_weight_kg),
            grade: d.grade,
            gsm: Number(d.gsm),
            coreMm: d.core_mm,
            diameterMm: Number(d.diameter_mm),
            status: d.status,
            location: d.location,
            operatorName: d.operator_name,
            createdAt: d.created_at
          }));
        }
      } catch {}
    }
    return this.getLocal<ChildSlitRoll[]>('child_rolls', DEFAULT_CHILD_ROLLS);
  }

  public async saveSlittingRun(
    parentJumboSerial: string,
    rolls: Array<{ widthMm: number; netWeightKg: number; serialNumber: string; coreMm: number }>,
    operatorName: string
  ): Promise<ChildSlitRoll[]> {
    const timestamp = new Date().toISOString().replace('T', ' ').slice(0, 19);
    const createdRolls: ChildSlitRoll[] = rolls.map((r, idx) => ({
      id: `slt-${Date.now()}-${idx + 1}`,
      parentJumboSerial,
      serialNumber: r.serialNumber,
      sequence: idx + 1,
      widthMm: r.widthMm,
      netWeightKg: r.netWeightKg,
      grossWeightKg: r.netWeightKg + 2,
      grade: 'Testliner Grade-A',
      gsm: 150,
      coreMm: r.coreMm || 76,
      diameterMm: 1250,
      status: 'PRODUCED',
      location: 'محطة سكاكين التقطيع SLT-04',
      operatorName,
      createdAt: timestamp
    }));

    if (this.supabase) {
      try {
        await this.supabase.from('slit_rolls').insert(createdRolls.map(r => ({
          id: r.id,
          parent_jumbo_serial: r.parentJumboSerial,
          serial_number: r.serialNumber,
          sequence: r.sequence,
          width_mm: r.widthMm,
          net_weight_kg: r.netWeightKg,
          gross_weight_kg: r.grossWeightKg,
          grade: r.grade,
          gsm: r.gsm,
          core_mm: r.coreMm,
          diameter_mm: r.diameterMm,
          status: r.status,
          location: r.location,
          operator_name: r.operatorName
        })));
      } catch {}
    }

    const current = await this.getChildRolls();
    this.setLocal('child_rolls', [...createdRolls, ...current]);

    for (const roll of createdRolls) {
      await this.addMovement({
        rollSerial: roll.serialNumber,
        rollCategory: 'فرعي',
        movementType: 'PRODUCTION_RECEIPT',
        netWeightKg: roll.netWeightKg,
        fromLocation: 'محطة سكاكين التقطيع SLT-04',
        toLocation: 'منطقة الرولات المشقوقة WIP',
        actor: operatorName,
        reason: `إنتاج رول مقصوص عرض ${roll.widthMm} مم من الرول الأم ${parentJumboSerial}`
      });
    }

    return createdRolls;
  }

  // --- QC Inspections ---
  public async getQCTests(): Promise<QCTest[]> {
    if (this.supabase) {
      try {
        const { data, error } = await this.supabase.from('qc_inspections').select('*').order('inspected_at', { ascending: false });
        if (!error && data && data.length > 0) {
          return data.map((d: any) => ({
            id: d.id,
            rollSerial: d.roll_serial,
            grade: d.grade,
            actualGsm: Number(d.actual_gsm),
            targetGsm: Number(d.target_gsm),
            moisturePct: Number(d.moisture_pct),
            caliperMicrons: Number(d.caliper_microns),
            burstKpa: Number(d.burst_kpa),
            cobb60: Number(d.cobb60),
            disposition: d.disposition,
            inspector: d.inspector,
            labNotes: d.lab_notes,
            inspectedAt: d.inspected_at
          }));
        }
      } catch {}
    }
    return this.getLocal<QCTest[]>('qc_tests', DEFAULT_QC_TESTS);
  }

  public async saveQCTest(test: Omit<QCTest, 'id' | 'inspectedAt'>): Promise<QCTest> {
    const newTest: QCTest = {
      ...test,
      id: `qc-${Date.now().toString().slice(-5)}`,
      inspectedAt: new Date().toISOString().replace('T', ' ').slice(0, 19)
    };

    if (this.supabase) {
      try {
        await this.supabase.from('qc_inspections').insert([{
          id: newTest.id,
          roll_serial: newTest.rollSerial,
          grade: newTest.grade,
          actual_gsm: newTest.actualGsm,
          target_gsm: newTest.targetGsm,
          moisture_pct: newTest.moisturePct,
          caliper_microns: newTest.caliperMicrons,
          burst_kpa: newTest.burstKpa,
          cobb60: newTest.cobb60,
          disposition: newTest.disposition,
          inspector: newTest.inspector,
          lab_notes: newTest.labNotes
        }]);

        // Also update status in Supabase slit_rolls table
        await this.supabase.from('slit_rolls').update({
          status: newTest.disposition === 'RELEASED' ? 'QC_PASSED' : 'REJECTED'
        }).eq('serial_number', newTest.rollSerial);
      } catch {}
    }

    const current = await this.getQCTests();
    this.setLocal('qc_tests', [newTest, ...current]);

    const rolls = await this.getChildRolls();
    const updatedRolls = rolls.map(r => {
      if (r.serialNumber === test.rollSerial) {
        return {
          ...r,
          status: test.disposition === 'RELEASED' ? ('QC_PASSED' as const) : ('REJECTED' as const)
        };
      }
      return r;
    });
    this.setLocal('child_rolls', updatedRolls);

    await this.addMovement({
      rollSerial: test.rollSerial,
      rollCategory: 'فرعي',
      movementType: 'STATUS_CHANGE',
      netWeightKg: 1245,
      fromLocation: 'محطة فحص الجودة QC Lab',
      toLocation: test.disposition === 'RELEASED' ? 'مستودع A (جاهز للشحن)' : 'منطقة الحجر والعزل',
      actor: test.inspector,
      reason: `اعتماد قرار الجودة [${test.disposition}] بمتوسط كراماج ${test.actualGsm} GSM ورطوبة ${test.moisturePct}%`
    });

    return newTest;
  }

  // --- Stock Movements ---
  public async getMovements(): Promise<StockMovement[]> {
    if (this.supabase) {
      try {
        const { data, error } = await this.supabase.from('stock_movements').select('*').order('timestamp', { ascending: false });
        if (!error && data && data.length > 0) {
          return data.map((d: any) => ({
            id: d.id,
            timestamp: d.timestamp,
            rollSerial: d.roll_serial,
            rollCategory: d.roll_category,
            movementType: d.movement_type,
            netWeightKg: Number(d.net_weight_kg),
            fromLocation: d.from_location,
            toLocation: d.to_location,
            actor: d.actor,
            reason: d.reason,
            auditHash: d.audit_hash
          }));
        }
      } catch {}
    }
    return this.getLocal<StockMovement[]>('movements', DEFAULT_MOVEMENTS);
  }

  public async addMovement(mov: Omit<StockMovement, 'id' | 'timestamp' | 'auditHash'>): Promise<StockMovement> {
    const hash = '0x' + Math.floor(Math.random() * 0xffffffffffff).toString(16).padStart(12, '0');
    const newMovement: StockMovement = {
      ...mov,
      id: `mov-${Date.now().toString().slice(-5)}`,
      timestamp: new Date().toISOString().replace('T', ' ').slice(0, 19),
      auditHash: hash
    };

    if (this.supabase) {
      try {
        await this.supabase.from('stock_movements').insert([{
          id: newMovement.id,
          roll_serial: newMovement.rollSerial,
          roll_category: newMovement.rollCategory,
          movement_type: newMovement.movementType,
          net_weight_kg: newMovement.netWeightKg,
          from_location: newMovement.fromLocation,
          to_location: newMovement.toLocation,
          actor: newMovement.actor,
          reason: newMovement.reason,
          audit_hash: newMovement.auditHash
        }]);
      } catch {}
    }

    const current = await this.getMovements();
    this.setLocal('movements', [newMovement, ...current]);
    return newMovement;
  }
}

export const db = new DatabaseManager();
