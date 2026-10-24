import React, { useState, useEffect } from 'react';
import { 
  CheckCircle2, RefreshCw, Scissors, Search, QrCode, ArrowLeftRight, 
  Wifi, Globe, Plus, Trash2, Check, Factory, 
  Printer, X, Download, ShieldCheck, UserCheck, Building2, 
  ArrowRight, CheckCircle, HardDrive
} from 'lucide-react';

interface HealthData {
  status: string;
  system: string;
  version: string;
  timestamp: string;
  databaseConnected: boolean;
  activeProfiles: string[];
}

interface SlitCut {
  id: string;
  sequence: number;
  widthMm: number;
  netWeightKg: number;
  serialNumber: string;
}

interface MovementRecord {
  id: string;
  rollSerial: string;
  rollType: 'جامبو' | 'فرعي';
  movementType: 'PRODUCTION_RECEIPT' | 'STATUS_CHANGE' | 'LOCATION_TRANSFER' | 'DISPATCH' | 'SCRAP';
  netWeightKg: number;
  reason: string;
  actor: string;
  timestamp: string;
}

interface RollLabelData {
  serialNumber: string;
  barcode: string;
  grade: string;
  widthMm: number;
  netWeightKg: number;
  grossWeightKg: number;
  gsm: number;
  coreMm: number;
  date: string;
  operator: string;
  quality: string;
}

interface UserProfile {
  name: string;
  email: string;
  role: 'OPERATOR' | 'QC_ENGINEER' | 'WAREHOUSE' | 'PLANT_DIRECTOR';
  roleArabic: string;
  factoryName: string;
  factoryCode: string;
}

const DEFAULT_USERS: UserProfile[] = [
  {
    name: 'م. مسعد عبد الله',
    email: 'mmycomputar12@gmail.com',
    role: 'PLANT_DIRECTOR',
    roleArabic: 'مدير عام المصنع والعمليات',
    factoryName: 'مجموعة النيل لصناعة وتدوير كرتون التعبئة',
    factoryCode: 'MILL-EGY-01'
  },
  {
    name: 'أحمد محمود',
    email: 'ahmed.operator@papererp.internal',
    role: 'OPERATOR',
    roleArabic: 'مشغل مقص الرولات SLIT1',
    factoryName: 'مجموعة النيل لصناعة وتدوير كرتون التعبئة',
    factoryCode: 'MILL-EGY-01'
  },
  {
    name: 'سارة إبراهيم',
    email: 'sara.qc@papererp.internal',
    role: 'QC_ENGINEER',
    roleArabic: 'مهندسة فحص الجودة والمختبر',
    factoryName: 'مجموعة النيل لصناعة وتدوير كرتون التعبئة',
    factoryCode: 'MILL-EGY-01'
  }
];

export default function App() {
  // Navigation & Authentication Views: 'app' | 'register' | 'login'
  const [authView, setAuthView] = useState<'app' | 'register' | 'login'>('app');
  const [currentUser, setCurrentUser] = useState<UserProfile>(DEFAULT_USERS[0]);
  
  // Registration Form State
  const [regFactoryName, setRegFactoryName] = useState('مصنع الفرات لورق الفلوتنج والتست لاينر');
  const [regFactoryCode, setRegFactoryCode] = useState('MILL-FRT-02');
  const [regName, setRegName] = useState('م. مسعد عبد الله');
  const [regEmail, setRegEmail] = useState('mmycomputar12@gmail.com');
  const [regPassword, setRegPassword] = useState('••••••••••••');
  const [regRole, setRegRole] = useState<'OPERATOR' | 'QC_ENGINEER' | 'WAREHOUSE' | 'PLANT_DIRECTOR'>('PLANT_DIRECTOR');
  const [registrationNotice, setRegistrationNotice] = useState<string | null>(null);

  // Core ERP Navigation Tabs
  const [activeTab, setActiveTab] = useState<'slitting' | 'genealogy' | 'qc' | 'ledger' | 'network'>('slitting');
  const [health, setHealth] = useState<HealthData | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [copiedLan, setCopiedLan] = useState<boolean>(false);
  const [executingCut, setExecutingCut] = useState<boolean>(false);

  // Thermal Barcode Label Modal State
  const [printModalRoll, setPrintModalRoll] = useState<RollLabelData | null>(null);

  // Slitting Calculator State
  const jumboDeckleWidth = 2400; // mm
  const jumboNetWeight = 3000; // kg
  const [cuts, setCuts] = useState<SlitCut[]>([
    { id: '1', sequence: 1, widthMm: 1000, netWeightKg: 1245, serialNumber: 'SLT-1-JMB-2026-001' },
    { id: '2', sequence: 2, widthMm: 1000, netWeightKg: 1245, serialNumber: 'SLT-2-JMB-2026-001' },
    { id: '3', sequence: 3, widthMm: 350, netWeightKg: 435, serialNumber: 'SLT-3-JMB-2026-001' },
  ]);

  const [trimWasteKg, setTrimWasteKg] = useState<number>(65);
  const [slittingSuccess, setSlittingSuccess] = useState<boolean>(false);

  // Calculations for Mass Balance
  const totalCutWidth = cuts.reduce((sum, c) => sum + c.widthMm, 0);
  const trimWasteMm = Math.max(0, jumboDeckleWidth - totalCutWidth);
  const totalChildWeight = cuts.reduce((sum, c) => sum + c.netWeightKg, 0);
  const massBalanceVariance = jumboNetWeight - (totalChildWeight + trimWasteKg);
  const isWidthExceeded = totalCutWidth > jumboDeckleWidth;
  const isVarianceExceeded = Math.abs(massBalanceVariance) > (jumboNetWeight * 0.01);

  // Search & Genealogy State
  const [searchSerial, setSearchSerial] = useState<string>('SLT-1-JMB-2026-001');

  // Quality Control Lab Gate State
  const [qcGsm, setQcGsm] = useState<number>(126.4);
  const [qcMoisture, setQcMoisture] = useState<number>(7.2);
  const [qcCaliper, setQcCaliper] = useState<number>(185);
  const [qcBurst, setQcBurst] = useState<number>(310);
  const [qcDisposition, setQcDisposition] = useState<'RELEASED' | 'QUARANTINE' | 'REJECTED' | 'DOWNGRADED'>('RELEASED');
  const [qcSavedSuccess, setQcSavedSuccess] = useState<boolean>(false);

  // Immutable Movement Ledger Records
  const [ledger, setLedger] = useState<MovementRecord[]>([
    { id: 'MOV-1001', rollSerial: 'JMB-2026-001', rollType: 'جامبو', movementType: 'PRODUCTION_RECEIPT', netWeightKg: 3000, reason: 'خروج رول الجامبو من درفيل ماكينة الورق PM1', actor: 'أحمد محمود (مشغل)', timestamp: '2026-10-24 14:15:00' },
    { id: 'MOV-1002', rollSerial: 'JMB-2026-001', rollType: 'جامبو', movementType: 'STATUS_CHANGE', netWeightKg: 3000, reason: 'استهلاك وتقطيع رول الجامبو على مقص الرولات SLIT1', actor: 'أحمد محمود (مشغل)', timestamp: '2026-10-24 14:30:00' },
    { id: 'MOV-1003', rollSerial: 'SLT-1-JMB-2026-001', rollType: 'فرعي', movementType: 'PRODUCTION_RECEIPT', netWeightKg: 1245, reason: 'إنتاج رول مقصوص عرض 1000 مم - مطابق فلوتنج 125 جم', actor: 'أحمد محمود (مشغل)', timestamp: '2026-10-24 14:30:00' },
    { id: 'MOV-1004', rollSerial: 'SLT-2-JMB-2026-001', rollType: 'فرعي', movementType: 'PRODUCTION_RECEIPT', netWeightKg: 1245, reason: 'إنتاج رول مقصوص عرض 1000 مم - مطابق فلوتنج 125 جم', actor: 'أحمد محمود (مشغل)', timestamp: '2026-10-24 14:30:00' },
    { id: 'MOV-1005', rollSerial: 'SLT-3-JMB-2026-001', rollType: 'فرعي', movementType: 'PRODUCTION_RECEIPT', netWeightKg: 435, reason: 'إنتاج رول مقصوص عرض 350 مم - مخصص لصناديق صغيرة', actor: 'أحمد محمود (مشغل)', timestamp: '2026-10-24 14:30:00' },
    { id: 'MOV-1006', rollSerial: 'SLT-1-JMB-2026-001', rollType: 'فرعي', movementType: 'LOCATION_TRANSFER', netWeightKg: 1245, reason: 'نقل من صالة التقطيع إلى رصيف التخزين المستودع A-04', actor: 'خالد إبراهيم (أمين مخزن)', timestamp: '2026-10-24 15:10:00' },
  ]);

  const [ledgerFilter, setLedgerFilter] = useState<string>('ALL');

  const fetchHealth = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/v1/health');
      if (res.ok) {
        const data = await res.json();
        setHealth(data);
      }
    } catch {
      // Offline fallback state
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  const addCut = () => {
    const nextSeq = cuts.length + 1;
    const defaultWidth = 300;
    const defaultWeight = Math.round((defaultWidth / jumboDeckleWidth) * jumboNetWeight * 0.98);
    setCuts([...cuts, {
      id: String(Date.now()),
      sequence: nextSeq,
      widthMm: defaultWidth,
      netWeightKg: defaultWeight,
      serialNumber: `SLT-${nextSeq}-JMB-2026-001`
    }]);
  };

  const removeCut = (id: string) => {
    setCuts(cuts.filter(c => c.id !== id));
  };

  const executeSlitting = async () => {
    if (isWidthExceeded || isVarianceExceeded) return;
    setExecutingCut(true);

    try {
      const payload = {
        jumboSerialNumber: 'JMB-2026-001',
        trimWasteKg: trimWasteKg,
        cuts: cuts.map(c => ({
          positionIndex: c.sequence,
          widthMm: c.widthMm,
          diameterMm: 1200,
          netWeightKg: c.netWeightKg,
          coreDiameterMm: 76
        }))
      };

      const res = await fetch('/api/v1/slitting/execute', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        // live backend acknowledged
      }
    } catch {
      // Offline fallback
    } finally {
      const newEntries: MovementRecord[] = cuts.map((c, idx) => ({
        id: `MOV-${Date.now().toString().slice(-4)}-${idx + 1}`,
        rollSerial: c.serialNumber,
        rollType: 'فرعي',
        movementType: 'PRODUCTION_RECEIPT',
        netWeightKg: c.netWeightKg,
        reason: `إنتاج رول مقصوص عرض ${c.widthMm} مم من الجامبو JMB-2026-001`,
        actor: currentUser.name,
        timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19)
      }));
      setLedger(prev => [...newEntries, ...prev]);
      setExecutingCut(false);
      setSlittingSuccess(true);
      setTimeout(() => setSlittingSuccess(false), 5000);
    }
  };

  const copyLanUrl = () => {
    navigator.clipboard.writeText('http://192.168.9.223:5173');
    setCopiedLan(true);
    setTimeout(() => setCopiedLan(false), 2500);
  };

  const openLabelPrint = (roll: Partial<RollLabelData>) => {
    setPrintModalRoll({
      serialNumber: roll.serialNumber || 'SLT-1-JMB-2026-001',
      barcode: roll.barcode || 'SR-2026-001-01',
      grade: roll.grade || 'فلوتنج 125 جم (Fluting 125)',
      widthMm: roll.widthMm || 1000,
      netWeightKg: roll.netWeightKg || 1245,
      grossWeightKg: (roll.netWeightKg || 1245) + 2,
      gsm: roll.gsm || 125,
      coreMm: roll.coreMm || 76,
      date: new Date().toLocaleDateString('ar-EG'),
      operator: currentUser.name,
      quality: 'مطابق للمواصفات القياسية (RELEASED)'
    });
  };

  const handlePrint = () => {
    window.print();
  };

  const exportLedgerCsv = () => {
    const headers = ['معرف الحركة', 'رقم الرول', 'نوع الرول', 'نوع الحركة', 'الوزن (كجم)', 'البيان', 'المشغل', 'التوقيت'];
    const rows = ledger.map(r => [r.id, r.rollSerial, r.rollType, r.movementType, r.netWeightKg, `"${r.reason}"`, r.actor, r.timestamp]);
    const csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `papererp_ledger_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleRegisterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const roleMap = {
      'OPERATOR': 'مشغل خط إنتاج ومقصات',
      'QC_ENGINEER': 'مهندس فحص جودة ومختبر',
      'WAREHOUSE': 'أمين مستودع ورولات',
      'PLANT_DIRECTOR': 'مدير عام المصنع والعمليات'
    };

    const newUser: UserProfile = {
      name: regName || 'مستخدم جديد',
      email: regEmail || 'user@papererp.internal',
      role: regRole,
      roleArabic: roleMap[regRole],
      factoryName: regFactoryName || 'مصنع تجريبي',
      factoryCode: regFactoryCode || 'MILL-DEMO'
    };

    setCurrentUser(newUser);
    setRegistrationNotice(`تم إنشاء حساب المنشأة [${newUser.factoryName}] بنجاح وتفعيل جلسة [${newUser.name}]!`);
    setTimeout(() => {
      setRegistrationNotice(null);
      setAuthView('app');
    }, 2000);
  };

  const switchDemoRole = (role: 'OPERATOR' | 'QC_ENGINEER' | 'PLANT_DIRECTOR') => {
    const found = DEFAULT_USERS.find(u => u.role === role);
    if (found) {
      setCurrentUser(found);
      setRegistrationNotice(`تم التبديل الفوري إلى صلاحية: ${found.roleArabic}`);
      setTimeout(() => setRegistrationNotice(null), 2500);
    }
  };

  const saveQcInspection = () => {
    setQcSavedSuccess(true);
    const newEntry: MovementRecord = {
      id: `MOV-QC-${Date.now().toString().slice(-4)}`,
      rollSerial: 'SLT-1-JMB-2026-001',
      rollType: 'فرعي',
      movementType: 'STATUS_CHANGE',
      netWeightKg: 1245,
      reason: `اعتماد قرار الجودة [${qcDisposition}] - GSM: ${qcGsm} جم، رطوبة: ${qcMoisture}%`,
      actor: currentUser.name,
      timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19)
    };
    setLedger(prev => [newEntry, ...prev]);
    setTimeout(() => setQcSavedSuccess(false), 3500);
  };

  // Filtered Ledger
  const filteredLedger = ledgerFilter === 'ALL' ? ledger : ledger.filter(r => r.movementType === ledgerFilter);

  // ---------------------------------------------------------------------------
  // VIEW: REGISTER PAGE
  // ---------------------------------------------------------------------------
  if (authView === 'register') {
    return (
      <div style={{ minHeight: '100vh', backgroundColor: '#090d16', color: '#f8fafc', fontFamily: 'Cairo, system-ui, sans-serif', padding: '32px 16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ maxWidth: 680, width: '100%', background: '#111726', border: '1px solid #1f293d', borderRadius: 12, padding: 32, boxShadow: '0 20px 40px rgba(0,0,0,0.6)' }}>
          
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #1f293d', paddingBottom: 20, marginBottom: 24 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{ background: '#1e2638', border: '1px solid #2e3b55', borderRadius: 8, padding: 8, display: 'flex' }}>
                <Building2 size={24} color="#c88a35" />
              </div>
              <div>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#f8fafc', margin: 0 }}>تسجيل مصنع / مستخدم جديد في المنصة</h2>
                <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>PaperERP Enterprise — تهيئة العزل السحابي والبيانات الأولية</span>
              </div>
            </div>
            <button 
              onClick={() => setAuthView('app')}
              style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.85rem' }}
            >
              <ArrowRight size={16} />
              العودة للوحة التحكم
            </button>
          </div>

          {registrationNotice && (
            <div style={{ background: '#064e3b', border: '1px solid #059669', color: '#6ee7b7', padding: '12px 16px', borderRadius: 8, marginBottom: 20, display: 'flex', alignItems: 'center', gap: 10, fontSize: '0.9rem' }}>
              <CheckCircle2 size={18} color="#34d399" />
              <span>{registrationNotice}</span>
            </div>
          )}

          <form onSubmit={handleRegisterSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', color: '#94a3b8', marginBottom: 6 }}>اسم المنشأة أو المصنع</label>
                <input 
                  type="text" 
                  required
                  value={regFactoryName}
                  onChange={e => setRegFactoryName(e.target.value)}
                  style={{ width: '100%', background: '#090d16', border: '1px solid #1f293d', color: '#f8fafc', padding: '10px 14px', borderRadius: 6, fontSize: '0.9rem', outline: 'none' }}
                  placeholder="مثال: مصنع النيل لتدوير الورق"
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', color: '#94a3b8', marginBottom: 6 }}>كود المصنع (Tenant ID)</label>
                <input 
                  type="text" 
                  required
                  value={regFactoryCode}
                  onChange={e => setRegFactoryCode(e.target.value)}
                  style={{ width: '100%', background: '#090d16', border: '1px solid #1f293d', color: '#c88a35', fontFamily: 'JetBrains Mono, monospace', padding: '10px 14px', borderRadius: 6, fontSize: '0.9rem', outline: 'none' }}
                  placeholder="MILL-01"
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', color: '#94a3b8', marginBottom: 6 }}>اسم المسؤول / المشغل</label>
                <input 
                  type="text" 
                  required
                  value={regName}
                  onChange={e => setRegName(e.target.value)}
                  style={{ width: '100%', background: '#090d16', border: '1px solid #1f293d', color: '#f8fafc', padding: '10px 14px', borderRadius: 6, fontSize: '0.9rem', outline: 'none' }}
                  placeholder="الاسم الثلاثي"
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', color: '#94a3b8', marginBottom: 6 }}>البريد الإلكتروني للعمل</label>
                <input 
                  type="email" 
                  required
                  value={regEmail}
                  onChange={e => setRegEmail(e.target.value)}
                  style={{ width: '100%', background: '#090d16', border: '1px solid #1f293d', color: '#f8fafc', padding: '10px 14px', borderRadius: 6, fontSize: '0.9rem', outline: 'none' }}
                  placeholder="mmycomputar12@gmail.com"
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 20 }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', color: '#94a3b8', marginBottom: 6 }}>الدور الوظيفي والصلاحيات</label>
                <select 
                  value={regRole}
                  onChange={e => setRegRole(e.target.value as any)}
                  style={{ width: '100%', background: '#090d16', border: '1px solid #1f293d', color: '#f8fafc', padding: '10px 14px', borderRadius: 6, fontSize: '0.9rem', outline: 'none' }}
                >
                  <option value="PLANT_DIRECTOR">مدير عام المصنع والعمليات (صلاحية كاملة)</option>
                  <option value="OPERATOR">مشغل خط إنتاج ومقصات (صالة المصنع)</option>
                  <option value="QC_ENGINEER">مهندس فحص جودة ومختبر قياس</option>
                  <option value="WAREHOUSE">أمين مستودع ورولات جاهزة</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', color: '#94a3b8', marginBottom: 6 }}>كلمة المرور</label>
                <input 
                  type="password" 
                  required
                  value={regPassword}
                  onChange={e => setRegPassword(e.target.value)}
                  style={{ width: '100%', background: '#090d16', border: '1px solid #1f293d', color: '#f8fafc', padding: '10px 14px', borderRadius: 6, fontSize: '0.9rem', outline: 'none' }}
                />
              </div>
            </div>

            <div style={{ background: '#090d16', border: '1px solid #1f293d', borderRadius: 8, padding: 16, marginBottom: 24, fontSize: '0.85rem', color: '#94a3b8' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#c88a35', fontWeight: 700, marginBottom: 6 }}>
                <ShieldCheck size={18} />
                <span>نظام عزل البيانات المتعدد (Composite Zero-Trust Multi-Tenancy)</span>
              </div>
              <p style={{ margin: 0, lineHeight: 1.6 }}>
                كل مصنع يحصل على مفتاح عزل برمجي مشفر يضمن عدم اختلاط سجلات تقطيع الرولات، مخزون الجامبو، أو شهادات فحص الجودة مع أي مصانع أو فروع أخرى على السيرفر.
              </p>
            </div>

            <div style={{ display: 'flex', gap: 12 }}>
              <button 
                type="submit"
                style={{ flex: 1, background: '#c88a35', border: '1px solid #e09b3d', color: '#090d16', padding: '12px 20px', borderRadius: 6, fontSize: '0.95rem', fontWeight: 800, cursor: 'pointer', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 8 }}
              >
                <CheckCircle size={18} />
                تأكيد تسجيل المنشأة والدخول المباشر
              </button>

              <button 
                type="button"
                onClick={() => setAuthView('app')}
                style={{ background: '#1e2638', border: '1px solid #2e3b55', color: '#cbd5e1', padding: '12px 20px', borderRadius: 6, fontSize: '0.9rem', cursor: 'pointer' }}
              >
                إلغاء
              </button>
            </div>
          </form>
        </div>
      </div>
    );
  }

  // ---------------------------------------------------------------------------
  // MAIN VIEW: INDUSTRIAL DASHBOARD
  // ---------------------------------------------------------------------------
  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#090d16', color: '#f8fafc', paddingBottom: 48, fontFamily: 'Cairo, system-ui, sans-serif' }}>
      
      {/* Top Banner Notice */}
      {registrationNotice && (
        <div style={{ background: '#064e3b', borderBottom: '1px solid #059669', color: '#a7f3d0', padding: '10px 24px', textAlign: 'center', fontSize: '0.85rem', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 8 }}>
          <CheckCircle2 size={16} color="#34d399" />
          <span>{registrationNotice}</span>
        </div>
      )}

      {/* Top Industrial Navigation Header */}
      <header style={{ background: '#111726', borderBottom: '1px solid #1f293d', padding: '14px 24px', boxShadow: '0 4px 16px rgba(0,0,0,0.4)' }}>
        <div style={{ maxWidth: 1320, margin: '0 auto', display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: 16 }}>
          
          {/* Brand & Factory Status */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <div style={{ background: '#1a2233', border: '1px solid #28354f', borderRadius: 8, padding: 9, display: 'flex' }}>
              <Factory size={26} color="#c88a35" />
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <h1 style={{ fontSize: '1.35rem', fontWeight: 800, letterSpacing: '-0.3px', color: '#f8fafc', margin: 0 }}>PaperERP Enterprise</h1>
                <span style={{ fontSize: '0.7rem', background: '#1c2436', border: '1px solid #2d3a56', color: '#c88a35', padding: '2px 8px', borderRadius: 4, fontWeight: 700, fontFamily: 'JetBrains Mono, monospace' }}>
                  {currentUser.factoryCode}
                </span>
                <span style={{ fontSize: '0.7rem', background: '#064e3b', border: '1px solid #059669', color: '#6ee7b7', padding: '2px 8px', borderRadius: 4, fontWeight: 700 }}>
                  ديمو تفاعلي مباشر
                </span>
              </div>
              <p style={{ color: '#94a3b8', fontSize: '0.8rem', margin: '2px 0 0 0' }}>
                نظام تتبع رولات الورق، ميزان الكتلة، وإدارة المخزون متعدد المصانع ({currentUser.factoryName})
              </p>
            </div>
          </div>

          {/* User Profile & Quick Switcher */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center' }}>
            
            {/* Active User Card */}
            <div style={{ background: '#151d2e', border: '1px solid #222e47', padding: '5px 12px', borderRadius: 6, display: 'flex', alignItems: 'center', gap: 10 }}>
              <UserCheck size={16} color="#38bdf8" />
              <div>
                <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#f8fafc' }}>{currentUser.name}</div>
                <div style={{ fontSize: '0.7rem', color: '#94a3b8' }}>{currentUser.roleArabic}</div>
              </div>
            </div>

            {/* Quick Role Switcher Buttons */}
            <div style={{ display: 'flex', gap: 4 }}>
              <button 
                onClick={() => switchDemoRole('PLANT_DIRECTOR')}
                style={{ background: currentUser.role === 'PLANT_DIRECTOR' ? '#c88a35' : '#151d2e', color: currentUser.role === 'PLANT_DIRECTOR' ? '#090d16' : '#94a3b8', border: '1px solid #222e47', padding: '5px 10px', borderRadius: 4, fontSize: '0.75rem', fontWeight: 700, cursor: 'pointer' }}
                title="الدخول كمدير مصنع"
              >
                مدير
              </button>
              <button 
                onClick={() => switchDemoRole('OPERATOR')}
                style={{ background: currentUser.role === 'OPERATOR' ? '#c88a35' : '#151d2e', color: currentUser.role === 'OPERATOR' ? '#090d16' : '#94a3b8', border: '1px solid #222e47', padding: '5px 10px', borderRadius: 4, fontSize: '0.75rem', fontWeight: 700, cursor: 'pointer' }}
                title="الدخول كمشغل مقصات"
              >
                مشغل
              </button>
              <button 
                onClick={() => switchDemoRole('QC_ENGINEER')}
                style={{ background: currentUser.role === 'QC_ENGINEER' ? '#c88a35' : '#151d2e', color: currentUser.role === 'QC_ENGINEER' ? '#090d16' : '#94a3b8', border: '1px solid #222e47', padding: '5px 10px', borderRadius: 4, fontSize: '0.75rem', fontWeight: 700, cursor: 'pointer' }}
                title="الدخول كمهندس جودة"
              >
                جودة
              </button>
            </div>

            {/* Register New Factory Button */}
            <button 
              onClick={() => setAuthView('register')}
              style={{ background: '#1c2436', border: '1px solid #2d3a56', color: '#c88a35', padding: '6px 12px', borderRadius: 6, fontSize: '0.8rem', fontWeight: 700, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
            >
              <Building2 size={14} />
              <span>تسجيل مصنع جديد</span>
            </button>

            {/* Factory LAN Copy Pill */}
            <button 
              onClick={copyLanUrl}
              style={{ background: '#151d2e', border: '1px solid #222e47', color: '#cbd5e1', padding: '6px 12px', borderRadius: 6, fontSize: '0.75rem', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
              title="انقر لنسخ رابط السيرفر المحلي"
            >
              <Wifi size={13} color="#38bdf8" />
              <span>LAN: <strong>192.168.9.223:5173</strong></span>
              {copiedLan ? <Check size={12} color="#4ade80" /> : <span style={{ fontSize: '0.65rem', color: '#64748b' }}>نسخ</span>}
            </button>

            {/* Cloudflare Pages Badge */}
            <div style={{ background: '#0a1d1d', border: '1px solid #0f4c4c', color: '#5eead4', padding: '6px 10px', borderRadius: 6, fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: 6 }}>
              <Globe size={13} color="#2dd4bf" />
              <span>Cloudflare Pages</span>
            </div>

            {/* API Health Refresh */}
            <button 
              onClick={fetchHealth} 
              disabled={loading}
              style={{ background: '#151d2e', border: '1px solid #222e47', color: '#94a3b8', padding: '6px 10px', borderRadius: 6, fontSize: '0.75rem', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
            >
              <RefreshCw size={12} className={loading ? 'animate-spin' : ''} />
              {health?.databaseConnected ? 'API متصل' : 'سيرفر محلي'}
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main style={{ maxWidth: 1320, margin: '24px auto', padding: '0 20px' }}>
        
        {/* Navigation Tabs Bar */}
        <div style={{ display: 'flex', gap: 6, borderBottom: '1px solid #1f293d', paddingBottom: 12, marginBottom: 24, overflowX: 'auto' }}>
          
          <button 
            onClick={() => setActiveTab('slitting')}
            style={{ 
              padding: '10px 18px', borderRadius: 6, border: '1px solid', cursor: 'pointer', fontFamily: 'inherit', fontWeight: 700, fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 8,
              background: activeTab === 'slitting' ? '#1c253b' : '#111726', 
              borderColor: activeTab === 'slitting' ? '#c88a35' : '#1f293d', 
              color: activeTab === 'slitting' ? '#f8fafc' : '#94a3b8'
            }}
          >
            <Scissors size={16} color={activeTab === 'slitting' ? '#c88a35' : '#94a3b8'} />
            حاسبة تقطيع الرولات وميزان الكتلة
          </button>

          <button 
            onClick={() => setActiveTab('genealogy')}
            style={{ 
              padding: '10px 18px', borderRadius: 6, border: '1px solid', cursor: 'pointer', fontFamily: 'inherit', fontWeight: 700, fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 8,
              background: activeTab === 'genealogy' ? '#1c253b' : '#111726', 
              borderColor: activeTab === 'genealogy' ? '#c88a35' : '#1f293d', 
              color: activeTab === 'genealogy' ? '#f8fafc' : '#94a3b8'
            }}
          >
            <QrCode size={16} color={activeTab === 'genealogy' ? '#c88a35' : '#94a3b8'} />
            شجرة تتبع الرول والباركود
          </button>

          <button 
            onClick={() => setActiveTab('qc')}
            style={{ 
              padding: '10px 18px', borderRadius: 6, border: '1px solid', cursor: 'pointer', fontFamily: 'inherit', fontWeight: 700, fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 8,
              background: activeTab === 'qc' ? '#1c253b' : '#111726', 
              borderColor: activeTab === 'qc' ? '#c88a35' : '#1f293d', 
              color: activeTab === 'qc' ? '#f8fafc' : '#94a3b8'
            }}
          >
            <ShieldCheck size={16} color={activeTab === 'qc' ? '#c88a35' : '#94a3b8'} />
            محطة فحص الجودة ومختبر القياس
          </button>

          <button 
            onClick={() => setActiveTab('ledger')}
            style={{ 
              padding: '10px 18px', borderRadius: 6, border: '1px solid', cursor: 'pointer', fontFamily: 'inherit', fontWeight: 700, fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 8,
              background: activeTab === 'ledger' ? '#1c253b' : '#111726', 
              borderColor: activeTab === 'ledger' ? '#c88a35' : '#1f293d', 
              color: activeTab === 'ledger' ? '#f8fafc' : '#94a3b8'
            }}
          >
            <ArrowLeftRight size={16} color={activeTab === 'ledger' ? '#c88a35' : '#94a3b8'} />
            سجل حركات المخزون المزدوج
          </button>

          <button 
            onClick={() => setActiveTab('network')}
            style={{ 
              padding: '10px 18px', borderRadius: 6, border: '1px solid', cursor: 'pointer', fontFamily: 'inherit', fontWeight: 700, fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 8,
              background: activeTab === 'network' ? '#1c253b' : '#111726', 
              borderColor: activeTab === 'network' ? '#c88a35' : '#1f293d', 
              color: activeTab === 'network' ? '#f8fafc' : '#94a3b8'
            }}
          >
            <HardDrive size={16} color={activeTab === 'network' ? '#c88a35' : '#94a3b8'} />
            إعدادات السيرفر والشبكة
          </button>
        </div>

        {/* ---------------------------------------------------------------- */}
        {/* TAB 1: SLITTING & MASS BALANCE CALCULATOR */}
        {/* ---------------------------------------------------------------- */}
        {activeTab === 'slitting' && (
          <div>
            {/* Top Metrics Cards */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 16, marginBottom: 24 }}>
              
              <div style={{ background: '#111726', borderRadius: 8, padding: 18, border: '1px solid #1f293d' }}>
                <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>رول الجامبو قيد التقطيع (Mother Roll)</span>
                <div style={{ fontSize: '1.4rem', fontWeight: 800, marginTop: 4, color: '#f8fafc', fontFamily: 'JetBrains Mono, monospace' }}>JMB-2026-001</div>
                <div style={{ fontSize: '0.85rem', color: '#94a3b8', marginTop: 4 }}>
                  عرض الشت: <strong style={{ color: '#f8fafc' }}>2,400 مم</strong> | الوزن الصافي: <strong style={{ color: '#c88a35' }}>3,000 كجم</strong>
                </div>
              </div>

              <div style={{ background: '#111726', borderRadius: 8, padding: 18, border: '1px solid #1f293d' }}>
                <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>مجموع عروض السكاكين (Deckle Cut)</span>
                <div style={{ fontSize: '1.4rem', fontWeight: 800, marginTop: 4, color: isWidthExceeded ? '#ef4444' : '#10b981', fontFamily: 'JetBrains Mono, monospace' }}>
                  {totalCutWidth.toLocaleString()} مم
                </div>
                <div style={{ fontSize: '0.85rem', color: '#94a3b8', marginTop: 4 }}>
                  هالك الحواف الفعلي: <strong style={{ color: '#f8fafc' }}>{trimWasteMm} مم</strong> ({((trimWasteMm / jumboDeckleWidth) * 100).toFixed(1)}%)
                </div>
              </div>

              <div style={{ background: '#111726', borderRadius: 8, padding: 18, border: '1px solid #1f293d' }}>
                <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>ميزان الكتلة والمطابقة (Mass Balance)</span>
                <div style={{ fontSize: '1.4rem', fontWeight: 800, marginTop: 4, color: isVarianceExceeded ? '#ef4444' : '#10b981', fontFamily: 'JetBrains Mono, monospace' }}>
                  {massBalanceVariance >= 0 ? `+${massBalanceVariance}` : massBalanceVariance} كجم
                </div>
                <div style={{ fontSize: '0.85rem', color: '#94a3b8', marginTop: 4 }}>
                  نسبة التفاوت الميزانية: <strong>{((Math.abs(massBalanceVariance) / jumboNetWeight) * 100).toFixed(2)}%</strong> (الحد الأقصى 1.0%)
                </div>
              </div>

              <div style={{ background: '#111726', borderRadius: 8, padding: 18, border: '1px solid #1f293d' }}>
                <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>عدد الرولات الناتجة (Child Rolls)</span>
                <div style={{ fontSize: '1.4rem', fontWeight: 800, marginTop: 4, color: '#f8fafc', fontFamily: 'JetBrains Mono, monospace' }}>
                  {cuts.length} رولات فرعية
                </div>
                <div style={{ fontSize: '0.85rem', color: '#94a3b8', marginTop: 4 }}>
                  الوزن الإجمالي للرولات: <strong style={{ color: '#f8fafc' }}>{totalChildWeight.toLocaleString()} كجم</strong>
                </div>
              </div>
            </div>

            {/* Interactive Visual Deckle Diagram */}
            <div style={{ background: '#111726', borderRadius: 8, padding: 20, border: '1px solid #1f293d', marginBottom: 24 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Scissors size={18} color="#c88a35" />
                  <h3 style={{ fontSize: '1rem', fontWeight: 700, margin: 0 }}>مخطط وضع سكاكين المقص الآلي (Visual Deckle Layout)</h3>
                </div>
                <span style={{ fontSize: '0.8rem', color: '#94a3b8', fontFamily: 'JetBrains Mono, monospace' }}>عرض الدرفيل الكامل: 2400 mm</span>
              </div>

              {/* Progress bar representing deckle */}
              <div style={{ width: '100%', height: 42, background: '#090d16', borderRadius: 6, display: 'flex', overflow: 'hidden', border: '1px solid #1f293d' }}>
                {cuts.map((c, idx) => {
                  const widthPercent = (c.widthMm / jumboDeckleWidth) * 100;
                  const colors = ['#1d4ed8', '#0284c7', '#0f766e', '#b45309', '#6d28d9'];
                  return (
                    <div 
                      key={c.id} 
                      style={{ 
                        width: `${widthPercent}%`, 
                        background: colors[idx % colors.length], 
                        display: 'flex', 
                        alignItems: 'center', 
                        justifyContent: 'center', 
                        color: '#ffffff', 
                        fontSize: '0.75rem', 
                        fontWeight: 700,
                        borderLeft: '1px solid rgba(255,255,255,0.2)',
                        fontFamily: 'JetBrains Mono, monospace'
                      }}
                      title={`سكينة #${c.sequence}: ${c.widthMm} مم`}
                    >
                      {c.widthMm >= 300 ? `#${c.sequence} (${c.widthMm}mm)` : `${c.widthMm}`}
                    </div>
                  );
                })}
                {trimWasteMm > 0 && (
                  <div 
                    style={{ 
                      width: `${(trimWasteMm / jumboDeckleWidth) * 100}%`, 
                      background: '#7f1d1d', 
                      display: 'flex', 
                      alignItems: 'center', 
                      justifyContent: 'center', 
                      color: '#fca5a5', 
                      fontSize: '0.7rem', 
                      fontWeight: 700,
                      fontFamily: 'JetBrains Mono, monospace'
                    }}
                    title={`هالك حواف الشت: ${trimWasteMm} مم`}
                  >
                    هالك ({trimWasteMm}mm)
                  </div>
                )}
              </div>
            </div>

            {/* Slit Cuts Table & Interactive Editor */}
            <div style={{ background: '#111726', borderRadius: 8, padding: 20, border: '1px solid #1f293d', marginBottom: 24 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                <div>
                  <h3 style={{ fontSize: '1rem', fontWeight: 700, margin: 0 }}>جدول أبعاد وتوليد الرولات الفرعية (Slit Cuts Specification)</h3>
                  <p style={{ fontSize: '0.8rem', color: '#94a3b8', margin: '2px 0 0 0' }}>يمكنك تعديل عروض السكاكين أو إضافة رول جديد لحساب الميزان آنياً</p>
                </div>

                <button 
                  onClick={addCut}
                  style={{ background: '#1a2233', border: '1px solid #2e3b55', color: '#c88a35', padding: '8px 14px', borderRadius: 6, fontSize: '0.85rem', fontWeight: 700, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
                >
                  <Plus size={16} />
                  إضافة سكينة تقطيع جديدة
                </button>
              </div>

              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'right', fontSize: '0.85rem' }}>
                  <thead>
                    <tr style={{ background: '#090d16', borderBottom: '1px solid #1f293d', color: '#94a3b8' }}>
                      <th style={{ padding: '12px 14px' }}>#</th>
                      <th style={{ padding: '12px 14px' }}>الرقم التسلسلي الفرعي المقترح</th>
                      <th style={{ padding: '12px 14px' }}>العرض (مم)</th>
                      <th style={{ padding: '12px 14px' }}>الوزن الصافي التقديري (كجم)</th>
                      <th style={{ padding: '12px 14px' }}>قطر الماسورة Core</th>
                      <th style={{ padding: '12px 14px' }}>معاينة الملصق</th>
                      <th style={{ padding: '12px 14px', textAlign: 'center' }}>إجراء</th>
                    </tr>
                  </thead>
                  <tbody>
                    {cuts.map((c, index) => (
                      <tr key={c.id} style={{ borderBottom: '1px solid #161e30' }}>
                        <td style={{ padding: '12px 14px', fontWeight: 700 }}>{index + 1}</td>
                        <td style={{ padding: '12px 14px', fontFamily: 'JetBrains Mono, monospace', color: '#38bdf8' }}>{c.serialNumber}</td>
                        <td style={{ padding: '12px 14px' }}>
                          <input 
                            type="number" 
                            value={c.widthMm}
                            onChange={(e) => {
                              const val = Math.max(50, parseInt(e.target.value) || 0);
                              const newWeight = Math.round((val / jumboDeckleWidth) * jumboNetWeight * 0.98);
                              setCuts(cuts.map(item => item.id === c.id ? { ...item, widthMm: val, netWeightKg: newWeight } : item));
                            }}
                            style={{ background: '#090d16', border: '1px solid #1f293d', color: '#f8fafc', padding: '6px 10px', borderRadius: 4, width: 90, textAlign: 'center', fontFamily: 'JetBrains Mono, monospace' }}
                          />
                        </td>
                        <td style={{ padding: '12px 14px' }}>
                          <input 
                            type="number" 
                            value={c.netWeightKg}
                            onChange={(e) => {
                              const val = parseInt(e.target.value) || 0;
                              setCuts(cuts.map(item => item.id === c.id ? { ...item, netWeightKg: val } : item));
                            }}
                            style={{ background: '#090d16', border: '1px solid #1f293d', color: '#f8fafc', padding: '6px 10px', borderRadius: 4, width: 100, textAlign: 'center', fontFamily: 'JetBrains Mono, monospace' }}
                          />
                        </td>
                        <td style={{ padding: '12px 14px', color: '#94a3b8' }}>76 مم (3 بوصة)</td>
                        <td style={{ padding: '12px 14px' }}>
                          <button 
                            onClick={() => openLabelPrint(c)}
                            style={{ background: '#1a2233', border: '1px solid #28354f', color: '#94a3b8', padding: '4px 10px', borderRadius: 4, cursor: 'pointer', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: 6 }}
                          >
                            <Printer size={13} color="#c88a35" />
                            ملصق 100x150
                          </button>
                        </td>
                        <td style={{ padding: '12px 14px', textAlign: 'center' }}>
                          <button 
                            onClick={() => removeCut(c.id)}
                            disabled={cuts.length <= 1}
                            style={{ background: 'transparent', border: 'none', color: cuts.length <= 1 ? '#475569' : '#ef4444', cursor: cuts.length <= 1 ? 'not-allowed' : 'pointer' }}
                            title="حذف السكينة"
                          >
                            <Trash2 size={16} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Trim Waste Slider & Execution Bar */}
              <div style={{ marginTop: 20, paddingTop: 16, borderTop: '1px solid #1f293d', display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: 16 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <label style={{ fontSize: '0.85rem', color: '#94a3b8' }}>وزن هالك الحواف المقدر (كجم):</label>
                  <input 
                    type="number" 
                    value={trimWasteKg}
                    onChange={(e) => setTrimWasteKg(parseInt(e.target.value) || 0)}
                    style={{ background: '#090d16', border: '1px solid #1f293d', color: '#f8fafc', padding: '6px 10px', borderRadius: 4, width: 80, textAlign: 'center', fontFamily: 'JetBrains Mono, monospace' }}
                  />
                  <span style={{ fontSize: '0.75rem', color: '#64748b' }}>يتم إعادة تدويره في حوض الـ Pulper</span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  {slittingSuccess && (
                    <div style={{ color: '#10b981', display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.85rem', fontWeight: 700 }}>
                      <CheckCircle2 size={18} />
                      تم اعتماد التقطيع الذري وتوليد الرولات بنجاح!
                    </div>
                  )}

                  <button 
                    onClick={executeSlitting}
                    disabled={executingCut || isWidthExceeded || isVarianceExceeded}
                    style={{ 
                      background: (isWidthExceeded || isVarianceExceeded) ? '#1f293d' : '#c88a35', 
                      color: (isWidthExceeded || isVarianceExceeded) ? '#64748b' : '#090d16', 
                      border: 'none', 
                      padding: '10px 24px', 
                      borderRadius: 6, 
                      fontWeight: 800, 
                      fontSize: '0.9rem', 
                      cursor: (isWidthExceeded || isVarianceExceeded) ? 'not-allowed' : 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8
                    }}
                  >
                    <CheckCircle size={18} />
                    {executingCut ? 'جاري قفل المعاملة ACID...' : 'اعتماد التقطيع الذري وتوليد الرولات'}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ---------------------------------------------------------------- */}
        {/* TAB 2: ROLL GENEALOGY & TRACEABILITY */}
        {/* ---------------------------------------------------------------- */}
        {activeTab === 'genealogy' && (
          <div>
            <div style={{ background: '#111726', borderRadius: 8, padding: 20, border: '1px solid #1f293d', marginBottom: 24 }}>
              <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: 16 }}>
                <div>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 800, margin: 0 }}>شجرة تتبع ونسب الرول (Parent-Child Roll Genealogy Tree)</h3>
                  <p style={{ fontSize: '0.8rem', color: '#94a3b8', margin: '2px 0 0 0' }}>تتبع كامل من رول الجامبو حتى الرول المقصوص وموقع التخزين بالمستودع</p>
                </div>

                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                  <div style={{ position: 'relative' }}>
                    <Search size={16} color="#94a3b8" style={{ position: 'absolute', right: 10, top: 10 }} />
                    <input 
                      type="text" 
                      value={searchSerial}
                      onChange={e => setSearchSerial(e.target.value)}
                      placeholder="بحث برقم السيريال أو الباركود..."
                      style={{ background: '#090d16', border: '1px solid #1f293d', color: '#f8fafc', padding: '8px 36px 8px 12px', borderRadius: 6, fontSize: '0.85rem', width: 260, outline: 'none', fontFamily: 'JetBrains Mono, monospace' }}
                    />
                  </div>
                </div>
              </div>

              {/* Genealogy Tree Visual */}
              <div style={{ marginTop: 24, padding: '24px 16px', background: '#090d16', borderRadius: 8, border: '1px solid #1f293d' }}>
                
                {/* Level 1: Mother Jumbo */}
                <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 24 }}>
                  <div style={{ background: '#1a2233', border: '2px solid #c88a35', borderRadius: 8, padding: 16, width: 340, textAlign: 'center', boxShadow: '0 8px 24px rgba(0,0,0,0.5)' }}>
                    <div style={{ fontSize: '0.7rem', color: '#c88a35', fontWeight: 800 }}>رول الجامبو الأصلي (Mother Pope Reel)</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#f8fafc', fontFamily: 'JetBrains Mono, monospace', margin: '4px 0' }}>JMB-2026-001</div>
                    <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                      الوزن: 3,000 كجم | العرض: 2,400 مم | الجراماج: 125 جم
                    </div>
                    <div style={{ fontSize: '0.75rem', color: '#10b981', marginTop: 6, fontWeight: 700 }}>
                      الحالة: تم التقطيع بالكامل (FULLY_SLIT)
                    </div>
                  </div>
                </div>

                {/* Tree Connector */}
                <div style={{ textAlign: 'center', color: '#c88a35', fontSize: '0.8rem', fontWeight: 700, marginBottom: 16, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 6 }}>
                  <Scissors size={14} />
                  <span>عملية التقطيع SLIT-BATCH-001 (مقص الرولات SLIT1)</span>
                </div>

                {/* Level 2: Child Slit Rolls */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16 }}>
                  {cuts.map((c) => (
                    <div key={c.id} style={{ background: '#111726', border: '1px solid #1f293d', borderRadius: 8, padding: 16 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                        <span style={{ fontSize: '0.75rem', color: '#c88a35', fontWeight: 700 }}>رول فرعي #{c.sequence}</span>
                        <span style={{ fontSize: '0.7rem', background: '#064e3b', color: '#6ee7b7', padding: '2px 6px', borderRadius: 4 }}>معتمد (RELEASED)</span>
                      </div>
                      <div style={{ fontSize: '1rem', fontWeight: 800, color: '#38bdf8', fontFamily: 'JetBrains Mono, monospace' }}>
                        {c.serialNumber}
                      </div>
                      <div style={{ fontSize: '0.8rem', color: '#94a3b8', marginTop: 6 }}>
                        العرض: <strong style={{ color: '#f8fafc' }}>{c.widthMm} مم</strong> | الوزن: <strong style={{ color: '#f8fafc' }}>{c.netWeightKg} كجم</strong>
                      </div>
                      <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: 4 }}>
                        الموقع: المستودع A-04 (رصة 2)
                      </div>

                      <div style={{ marginTop: 12, paddingTop: 10, borderTop: '1px solid #1f293d', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <button 
                          onClick={() => openLabelPrint(c)}
                          style={{ background: '#1a2233', border: '1px solid #28354f', color: '#c88a35', padding: '4px 10px', borderRadius: 4, cursor: 'pointer', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: 6 }}
                        >
                          <Printer size={13} />
                          طباعة الملصق الحراري
                        </button>

                        <button 
                          onClick={() => setActiveTab('qc')}
                          style={{ background: 'transparent', border: 'none', color: '#38bdf8', fontSize: '0.75rem', cursor: 'pointer' }}
                        >
                          تقرير الفحص
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ---------------------------------------------------------------- */}
        {/* TAB 3: QUALITY CONTROL LABORATORY GATE */}
        {/* ---------------------------------------------------------------- */}
        {activeTab === 'qc' && (
          <div>
            <div style={{ background: '#111726', borderRadius: 8, padding: 20, border: '1px solid #1f293d', marginBottom: 24 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                <div>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 800, margin: 0 }}>محطة فحص الجودة ومختبر القياس (QC Laboratory Gate)</h3>
                  <p style={{ fontSize: '0.8rem', color: '#94a3b8', margin: '2px 0 0 0' }}>تسجيل القياسات المعملية وإصدار قرار الإفراج أو الحجز للرول</p>
                </div>
                <div style={{ background: '#151d2e', border: '1px solid #222e47', padding: '6px 12px', borderRadius: 6, fontSize: '0.8rem', color: '#94a3b8', fontFamily: 'JetBrains Mono, monospace' }}>
                  الرول المفحوص: <strong style={{ color: '#38bdf8' }}>SLT-1-JMB-2026-001</strong>
                </div>
              </div>

              {qcSavedSuccess && (
                <div style={{ background: '#064e3b', border: '1px solid #059669', color: '#6ee7b7', padding: '12px 16px', borderRadius: 6, marginBottom: 20, display: 'flex', alignItems: 'center', gap: 10, fontSize: '0.85rem' }}>
                  <CheckCircle2 size={18} color="#34d399" />
                  <span>تم حفظ وتوثيق نتائج الفحص وتحديث سجل الحركة في المخزن بنجاح!</span>
                </div>
              )}

              {/* Lab Inputs Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 16, marginBottom: 24 }}>
                
                <div style={{ background: '#090d16', border: '1px solid #1f293d', borderRadius: 8, padding: 16 }}>
                  <label style={{ display: 'block', fontSize: '0.8rem', color: '#94a3b8', marginBottom: 6 }}>الوزن النوعي الفعلي (GSM)</label>
                  <input 
                    type="number" 
                    step="0.1"
                    value={qcGsm}
                    onChange={e => setQcGsm(parseFloat(e.target.value) || 0)}
                    style={{ width: '100%', background: '#111726', border: '1px solid #1f293d', color: '#f8fafc', padding: '8px 12px', borderRadius: 6, fontSize: '1.1rem', fontWeight: 800, fontFamily: 'JetBrains Mono, monospace', outline: 'none' }}
                  />
                  <span style={{ fontSize: '0.7rem', color: '#64748b', marginTop: 4, display: 'block' }}>المواصفة المستهدفة: 125 ± 3 جم/م²</span>
                </div>

                <div style={{ background: '#090d16', border: '1px solid #1f293d', borderRadius: 8, padding: 16 }}>
                  <label style={{ display: 'block', fontSize: '0.8rem', color: '#94a3b8', marginBottom: 6 }}>نسبة الرطوبة الفاحصة (%)</label>
                  <input 
                    type="number" 
                    step="0.1"
                    value={qcMoisture}
                    onChange={e => setQcMoisture(parseFloat(e.target.value) || 0)}
                    style={{ width: '100%', background: '#111726', border: '1px solid #1f293d', color: '#f8fafc', padding: '8px 12px', borderRadius: 6, fontSize: '1.1rem', fontWeight: 800, fontFamily: 'JetBrains Mono, monospace', outline: 'none' }}
                  />
                  <span style={{ fontSize: '0.7rem', color: '#64748b', marginTop: 4, display: 'block' }}>المدى المسموح: 6.5% - 8.5%</span>
                </div>

                <div style={{ background: '#090d16', border: '1px solid #1f293d', borderRadius: 8, padding: 16 }}>
                  <label style={{ display: 'block', fontSize: '0.8rem', color: '#94a3b8', marginBottom: 6 }}>السُمك Caliper (ميكرون)</label>
                  <input 
                    type="number" 
                    value={qcCaliper}
                    onChange={e => setQcCaliper(parseInt(e.target.value) || 0)}
                    style={{ width: '100%', background: '#111726', border: '1px solid #1f293d', color: '#f8fafc', padding: '8px 12px', borderRadius: 6, fontSize: '1.1rem', fontWeight: 800, fontFamily: 'JetBrains Mono, monospace', outline: 'none' }}
                  />
                  <span style={{ fontSize: '0.7rem', color: '#64748b', marginTop: 4, display: 'block' }}>المعيار: 180 - 195 µm</span>
                </div>

                <div style={{ background: '#090d16', border: '1px solid #1f293d', borderRadius: 8, padding: 16 }}>
                  <label style={{ display: 'block', fontSize: '0.8rem', color: '#94a3b8', marginBottom: 6 }}>قوة الانفجار Mullen (kPa)</label>
                  <input 
                    type="number" 
                    value={qcBurst}
                    onChange={e => setQcBurst(parseInt(e.target.value) || 0)}
                    style={{ width: '100%', background: '#111726', border: '1px solid #1f293d', color: '#f8fafc', padding: '8px 12px', borderRadius: 6, fontSize: '1.1rem', fontWeight: 800, fontFamily: 'JetBrains Mono, monospace', outline: 'none' }}
                  />
                  <span style={{ fontSize: '0.7rem', color: '#64748b', marginTop: 4, display: 'block' }}>الحد الأدنى: 290 kPa</span>
                </div>
              </div>

              {/* Disposition Selection */}
              <div style={{ background: '#090d16', border: '1px solid #1f293d', borderRadius: 8, padding: 18, marginBottom: 20 }}>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, color: '#f8fafc', marginBottom: 12 }}>
                  قرار الإفراج النهائي للرول (Disposition Decision):
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 12 }}>
                  
                  <button 
                    type="button"
                    onClick={() => setQcDisposition('RELEASED')}
                    style={{ 
                      padding: 12, borderRadius: 6, border: '1px solid', cursor: 'pointer', textAlign: 'center', fontFamily: 'inherit',
                      background: qcDisposition === 'RELEASED' ? '#064e3b' : '#111726',
                      borderColor: qcDisposition === 'RELEASED' ? '#059669' : '#1f293d',
                      color: qcDisposition === 'RELEASED' ? '#6ee7b7' : '#94a3b8'
                    }}
                  >
                    <div style={{ fontWeight: 800, fontSize: '0.9rem' }}>صالح للشحن (RELEASED)</div>
                    <div style={{ fontSize: '0.7rem', marginTop: 2 }}>مطابق للمواصفات 100%</div>
                  </button>

                  <button 
                    type="button"
                    onClick={() => setQcDisposition('QUARANTINE')}
                    style={{ 
                      padding: 12, borderRadius: 6, border: '1px solid', cursor: 'pointer', textAlign: 'center', fontFamily: 'inherit',
                      background: qcDisposition === 'QUARANTINE' ? '#78350f' : '#111726',
                      borderColor: qcDisposition === 'QUARANTINE' ? '#d97706' : '#1f293d',
                      color: qcDisposition === 'QUARANTINE' ? '#fde68a' : '#94a3b8'
                    }}
                  >
                    <div style={{ fontWeight: 800, fontSize: '0.9rem' }}>حجز معملي (QUARANTINE)</div>
                    <div style={{ fontSize: '0.7rem', marginTop: 2 }}>إعادة اختبار الرطوبة والشد</div>
                  </button>

                  <button 
                    type="button"
                    onClick={() => setQcDisposition('DOWNGRADED')}
                    style={{ 
                      padding: 12, borderRadius: 6, border: '1px solid', cursor: 'pointer', textAlign: 'center', fontFamily: 'inherit',
                      background: qcDisposition === 'DOWNGRADED' ? '#1e293b' : '#111726',
                      borderColor: qcDisposition === 'DOWNGRADED' ? '#475569' : '#1f293d',
                      color: qcDisposition === 'DOWNGRADED' ? '#cbd5e1' : '#94a3b8'
                    }}
                  >
                    <div style={{ fontWeight: 800, fontSize: '0.9rem' }}>تخفيض رتبة (DOWNGRADED)</div>
                    <div style={{ fontSize: '0.7rem', marginTop: 2 }}>تحويل من فلوتنج إلى تغليف خفيف</div>
                  </button>

                  <button 
                    type="button"
                    onClick={() => setQcDisposition('REJECTED')}
                    style={{ 
                      padding: 12, borderRadius: 6, border: '1px solid', cursor: 'pointer', textAlign: 'center', fontFamily: 'inherit',
                      background: qcDisposition === 'REJECTED' ? '#7f1d1d' : '#111726',
                      borderColor: qcDisposition === 'REJECTED' ? '#dc2626' : '#1f293d',
                      color: qcDisposition === 'REJECTED' ? '#fca5a5' : '#94a3b8'
                    }}
                  >
                    <div style={{ fontWeight: 800, fontSize: '0.9rem' }}>مرفوض تالف (REJECTED)</div>
                    <div style={{ fontSize: '0.7rem', marginTop: 2 }}>إرسال لإعادة الفرم والتدوير</div>
                  </button>
                </div>
              </div>

              <button 
                onClick={saveQcInspection}
                style={{ background: '#c88a35', border: 'none', color: '#090d16', padding: '12px 24px', borderRadius: 6, fontSize: '0.9rem', fontWeight: 800, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}
              >
                <CheckCircle size={18} />
                توثيق شهادة الفحص وختم السجل
              </button>
            </div>
          </div>
        )}

        {/* ---------------------------------------------------------------- */}
        {/* TAB 4: IMMUTABLE INVENTORY LEDGER */}
        {/* ---------------------------------------------------------------- */}
        {activeTab === 'ledger' && (
          <div>
            <div style={{ background: '#111726', borderRadius: 8, padding: 20, border: '1px solid #1f293d', marginBottom: 24 }}>
              <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: 16, marginBottom: 16 }}>
                <div>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 800, margin: 0 }}>سجل حركات المخزون المزدوج (Immutable Movement Ledger)</h3>
                  <p style={{ fontSize: '0.8rem', color: '#94a3b8', margin: '2px 0 0 0' }}>سجل غير قابل للتعديل لكافة حركات الإنتاج، التقطيع، النقل، والصرف</p>
                </div>

                <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                  <select 
                    value={ledgerFilter}
                    onChange={e => setLedgerFilter(e.target.value)}
                    style={{ background: '#090d16', border: '1px solid #1f293d', color: '#f8fafc', padding: '8px 12px', borderRadius: 6, fontSize: '0.8rem', outline: 'none' }}
                  >
                    <option value="ALL">كافة أنواع الحركات</option>
                    <option value="PRODUCTION_RECEIPT">ورود إنتاج (PRODUCTION_RECEIPT)</option>
                    <option value="STATUS_CHANGE">تغيير حالة وتقطيع (STATUS_CHANGE)</option>
                    <option value="LOCATION_TRANSFER">نقل مستودع (LOCATION_TRANSFER)</option>
                  </select>

                  <button 
                    onClick={exportLedgerCsv}
                    style={{ background: '#1a2233', border: '1px solid #2e3b55', color: '#c88a35', padding: '8px 14px', borderRadius: 6, fontSize: '0.85rem', fontWeight: 700, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
                  >
                    <Download size={15} />
                    تصدير ملف CSV
                  </button>
                </div>
              </div>

              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'right', fontSize: '0.85rem' }}>
                  <thead>
                    <tr style={{ background: '#090d16', borderBottom: '1px solid #1f293d', color: '#94a3b8' }}>
                      <th style={{ padding: '12px 14px' }}>معرف الحركة</th>
                      <th style={{ padding: '12px 14px' }}>رقم الرول</th>
                      <th style={{ padding: '12px 14px' }}>نوع الرول</th>
                      <th style={{ padding: '12px 14px' }}>نوع الحركة</th>
                      <th style={{ padding: '12px 14px' }}>الوزن الصافي</th>
                      <th style={{ padding: '12px 14px' }}>البيان والتفاصيل</th>
                      <th style={{ padding: '12px 14px' }}>المسؤول</th>
                      <th style={{ padding: '12px 14px' }}>التوقيت</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredLedger.map((row) => (
                      <tr key={row.id} style={{ borderBottom: '1px solid #161e30' }}>
                        <td style={{ padding: '12px 14px', fontFamily: 'JetBrains Mono, monospace', color: '#c88a35' }}>{row.id}</td>
                        <td style={{ padding: '12px 14px', fontFamily: 'JetBrains Mono, monospace', color: '#38bdf8' }}>{row.rollSerial}</td>
                        <td style={{ padding: '12px 14px' }}>
                          <span style={{ fontSize: '0.75rem', background: row.rollType === 'جامبو' ? '#1e293b' : '#064e3b', color: row.rollType === 'جامبو' ? '#cbd5e1' : '#6ee7b7', padding: '2px 8px', borderRadius: 4 }}>
                            {row.rollType}
                          </span>
                        </td>
                        <td style={{ padding: '12px 14px', fontFamily: 'JetBrains Mono, monospace', fontSize: '0.75rem' }}>{row.movementType}</td>
                        <td style={{ padding: '12px 14px', fontFamily: 'JetBrains Mono, monospace', fontWeight: 700 }}>{row.netWeightKg.toLocaleString()} كجم</td>
                        <td style={{ padding: '12px 14px', color: '#cbd5e1' }}>{row.reason}</td>
                        <td style={{ padding: '12px 14px', color: '#94a3b8' }}>{row.actor}</td>
                        <td style={{ padding: '12px 14px', color: '#64748b', fontSize: '0.75rem', fontFamily: 'JetBrains Mono, monospace' }}>{row.timestamp}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* ---------------------------------------------------------------- */}
        {/* TAB 5: SERVER & CLOUDFLARE SETTINGS */}
        {/* ---------------------------------------------------------------- */}
        {activeTab === 'network' && (
          <div>
            <div style={{ background: '#111726', borderRadius: 8, padding: 20, border: '1px solid #1f293d', marginBottom: 24 }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 800, margin: '0 0 4px 0' }}>إعدادات السيرفر والربط السحابي (Cloudflare & Local LAN)</h3>
              <p style={{ fontSize: '0.8rem', color: '#94a3b8', margin: '0 0 20px 0' }}>معلومات الوصول لصالة الإنتاج والعملاء عبر الإنترنت</p>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 16 }}>
                
                <div style={{ background: '#090d16', border: '1px solid #1f293d', borderRadius: 8, padding: 18 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
                    <Wifi size={20} color="#38bdf8" />
                    <h4 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 700 }}>شبكة صالة المصنع الداخلية (Local LAN)</h4>
                  </div>
                  <p style={{ fontSize: '0.8rem', color: '#94a3b8', lineHeight: 1.6, margin: '0 0 12px 0' }}>
                    يمكن لجميع أجهزة التابلت وأجهزة الوزن ومقصات الرولات الدخول مباشرة عبر الآي بي الداخلي بدون إنترنت:
                  </p>
                  <div style={{ background: '#111726', border: '1px solid #1f293d', borderRadius: 6, padding: '10px 14px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontFamily: 'JetBrains Mono, monospace', fontSize: '0.85rem' }}>
                    <span style={{ color: '#38bdf8' }}>http://192.168.9.223:5173</span>
                    <button onClick={copyLanUrl} style={{ background: '#1a2233', border: '1px solid #2e3b55', color: '#cbd5e1', padding: '4px 10px', borderRadius: 4, cursor: 'pointer', fontSize: '0.75rem' }}>
                      {copiedLan ? 'تم النسخ' : 'نسخ'}
                    </button>
                  </div>
                </div>

                <div style={{ background: '#090d16', border: '1px solid #1f293d', borderRadius: 8, padding: 18 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
                    <Globe size={20} color="#2dd4bf" />
                    <h4 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 700 }}>الربط السحابي التلقائي (Cloudflare Pages)</h4>
                  </div>
                  <p style={{ fontSize: '0.8rem', color: '#94a3b8', lineHeight: 1.6, margin: '0 0 12px 0' }}>
                    كل عملية رفع على GitHub (Push to main) بحسابك <code style={{ color: '#c88a35' }}>mosaad20</code> تقوم بإعادة بناء الواجهة ونشرها للعميل تلقائياً على الرابط الحي.
                  </p>
                  <div style={{ background: '#111726', border: '1px solid #1f293d', borderRadius: 6, padding: '10px 14px', color: '#2dd4bf', fontFamily: 'JetBrains Mono, monospace', fontSize: '0.85rem' }}>
                    https://papererp.pages.dev
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* ------------------------------------------------------------------ */}
      {/* THERMAL BARCODE LABEL MODAL (100x150 mm) */}
      {/* ------------------------------------------------------------------ */}
      {printModalRoll && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.85)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: 20 }}>
          <div style={{ background: '#ffffff', color: '#000000', width: 440, borderRadius: 8, padding: 24, boxShadow: '0 25px 50px -12px rgba(0,0,0,0.5)', position: 'relative', fontFamily: 'Cairo, sans-serif' }}>
            
            {/* Close Modal Button */}
            <button 
              onClick={() => setPrintModalRoll(null)}
              style={{ position: 'absolute', top: 14, left: 14, background: '#f1f5f9', border: 'none', borderRadius: '50%', width: 28, height: 28, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}
            >
              <X size={16} color="#0f172a" />
            </button>

            {/* Industrial Label Layout (100x150 mm Zebra Style) */}
            <div style={{ border: '3px solid #000000', padding: 14, textAlign: 'center' }}>
              <div style={{ fontSize: '0.85rem', fontWeight: 900, textTransform: 'uppercase', letterSpacing: 0.5 }}>
                {currentUser.factoryName}
              </div>
              <div style={{ fontSize: '0.7rem', fontWeight: 700, color: '#334155', borderBottom: '2px solid #000000', paddingBottom: 6, marginBottom: 10 }}>
                ملصق بطاقة تعريف الرول الفرعي | PAPER ROLL IDENTITY TAG
              </div>

              {/* Barcode Graphic Representation */}
              <div style={{ margin: '10px 0', padding: '6px 0', background: '#f8fafc', border: '1px dashed #cbd5e1' }}>
                <svg width="280" height="48" viewBox="0 0 280 48" style={{ display: 'block', margin: '0 auto' }}>
                  {/* Stylized high contrast Barcode lines */}
                  {[2, 3, 1, 4, 2, 1, 3, 2, 4, 1, 2, 3, 1, 2, 4, 2, 3, 1, 4, 2, 1, 3, 1, 4, 2, 3, 1, 2, 4, 1, 3, 2, 4, 1, 2, 3, 2, 4].map((width, i) => (
                    <rect key={i} x={i * 7 + 6} y="4" width={width} height="40" fill="#000000" />
                  ))}
                </svg>
                <div style={{ fontSize: '0.9rem', fontWeight: 900, letterSpacing: 3, fontFamily: 'JetBrains Mono, monospace', marginTop: 4 }}>
                  {printModalRoll.serialNumber}
                </div>
              </div>

              {/* Specs Grid Table */}
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.75rem', textAlign: 'right', margin: '12px 0', border: '1px solid #000000' }}>
                <tbody>
                  <tr style={{ borderBottom: '1px solid #000000' }}>
                    <td style={{ padding: '4px 6px', fontWeight: 700, background: '#f1f5f9', width: '35%' }}>نوع الورق (Grade):</td>
                    <td style={{ padding: '4px 6px', fontWeight: 800 }}>{printModalRoll.grade}</td>
                  </tr>
                  <tr style={{ borderBottom: '1px solid #000000' }}>
                    <td style={{ padding: '4px 6px', fontWeight: 700, background: '#f1f5f9' }}>عرض الرول (Width):</td>
                    <td style={{ padding: '4px 6px', fontWeight: 800, fontFamily: 'JetBrains Mono, monospace' }}>{printModalRoll.widthMm} mm</td>
                  </tr>
                  <tr style={{ borderBottom: '1px solid #000000' }}>
                    <td style={{ padding: '4px 6px', fontWeight: 700, background: '#f1f5f9' }}>الوزن الصافي (Net Wt):</td>
                    <td style={{ padding: '4px 6px', fontWeight: 900, fontSize: '0.9rem', fontFamily: 'JetBrains Mono, monospace' }}>{printModalRoll.netWeightKg} KG</td>
                  </tr>
                  <tr style={{ borderBottom: '1px solid #000000' }}>
                    <td style={{ padding: '4px 6px', fontWeight: 700, background: '#f1f5f9' }}>الوزن القائم (Gross Wt):</td>
                    <td style={{ padding: '4px 6px', fontWeight: 800, fontFamily: 'JetBrains Mono, monospace' }}>{printModalRoll.grossWeightKg} KG</td>
                  </tr>
                  <tr style={{ borderBottom: '1px solid #000000' }}>
                    <td style={{ padding: '4px 6px', fontWeight: 700, background: '#f1f5f9' }}>قطر الماسورة (Core):</td>
                    <td style={{ padding: '4px 6px', fontWeight: 800 }}>{printModalRoll.coreMm} mm (3 inch)</td>
                  </tr>
                  <tr style={{ borderBottom: '1px solid #000000' }}>
                    <td style={{ padding: '4px 6px', fontWeight: 700, background: '#f1f5f9' }}>تاريخ وخطة الإنتاج:</td>
                    <td style={{ padding: '4px 6px' }}>{printModalRoll.date} — وردية المساء</td>
                  </tr>
                  <tr>
                    <td style={{ padding: '4px 6px', fontWeight: 700, background: '#f1f5f9' }}>اعتماد الجودة:</td>
                    <td style={{ padding: '4px 6px', fontWeight: 800, color: '#15803d' }}>{printModalRoll.quality}</td>
                  </tr>
                </tbody>
              </table>

              {/* Bottom Operator & QR Code Section */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 10 }}>
                <div style={{ textAlign: 'right', fontSize: '0.7rem' }}>
                  <div>المشغل المسجل: <strong>{printModalRoll.operator}</strong></div>
                  <div>المقص: <strong>SLIT-01</strong></div>
                </div>

                <div style={{ width: 44, height: 44, border: '1px solid #000000', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <QrCode size={36} color="#000000" />
                </div>
              </div>
            </div>

            {/* Modal Actions */}
            <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
              <button 
                onClick={handlePrint}
                style={{ flex: 1, background: '#0f172a', color: '#ffffff', border: 'none', padding: '10px 16px', borderRadius: 6, fontWeight: 700, fontSize: '0.85rem', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}
              >
                <Printer size={16} />
                إرسال لطابعة الباركود Zebra (Print)
              </button>

              <button 
                onClick={() => setPrintModalRoll(null)}
                style={{ background: '#f1f5f9', color: '#475569', border: '1px solid #cbd5e1', padding: '10px 16px', borderRadius: 6, fontWeight: 700, fontSize: '0.85rem', cursor: 'pointer' }}
              >
                إغلاق
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
