import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { UserProfile, JumboRoll, ChildSlitRoll, QCTest, StockMovement } from '../types';

// Initial pre-seeded mock dataset for instant zero-config interactive demo
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
    auditHash: '0x8f4d92a10c7e48b9'
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
    auditHash: '0x3b1c8f420e9a7d21'
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
    auditHash: '0x4a92c0183b5e8f17'
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
    auditHash: '0x7e29a41b5d803c4f'
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

    this.url = localUrl || envUrl;
    this.key = localKey || envKey;

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

  // Generic local storage helpers with defaults
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
        if (!error && data && data.length > 0) return data;
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
        await this.supabase.from('users').insert([newUser]);
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
        if (!error && data && data.length > 0) return data;
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
        await this.supabase.from('jumbo_rolls').insert([newJumbo]);
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
        if (!error && data && data.length > 0) return data;
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
        await this.supabase.from('slit_rolls').insert(createdRolls);
      } catch {}
    }

    const current = await this.getChildRolls();
    this.setLocal('child_rolls', [...createdRolls, ...current]);

    // Record stock movement for each child roll
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
        if (!error && data && data.length > 0) return data;
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
        await this.supabase.from('qc_inspections').insert([newTest]);
      } catch {}
    }

    const current = await this.getQCTests();
    this.setLocal('qc_tests', [newTest, ...current]);

    // Update the child roll's status if applicable
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

    // Record movement audit
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
        if (!error && data && data.length > 0) return data;
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
        await this.supabase.from('stock_movements').insert([newMovement]);
      } catch {}
    }

    const current = await this.getMovements();
    this.setLocal('movements', [newMovement, ...current]);
    return newMovement;
  }
}

export const db = new DatabaseManager();
