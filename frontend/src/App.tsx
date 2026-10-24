import React, { useState, useEffect } from 'react';
import { db } from './lib/supabase';
import { UserProfile, JumboRoll, ChildSlitRoll, QCTest, StockMovement, SlitCut } from './types';

export default function App() {
  // Navigation State
  const [activeTab, setActiveTab] = useState<
    'operational-cockpit' | 'deckle-calculator' | 'roll-genealogy' | 'qc-laboratory-gate' | 'thermal-label-printer' | 'immutable-stock-ledger' | 'rugged-tablet'
  >('operational-cockpit');

  // Core Data State (Loaded from DB / Supabase)
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [jumbos, setJumbos] = useState<JumboRoll[]>([]);
  const [childRolls, setChildRolls] = useState<ChildSlitRoll[]>([]);
  const [qcTests, setQcTests] = useState<QCTest[]>([]);
  const [movements, setMovements] = useState<StockMovement[]>([]);

  // Modals State
  const [showAuthModal, setShowAuthModal] = useState<boolean>(false);
  const [showDbModal, setShowDbModal] = useState<boolean>(false);
  const [showMovementModal, setShowMovementModal] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Live Mill Clock
  const [currentTimeStr, setCurrentTimeStr] = useState<string>('');

  // Slitting Calculator State
  const [selectedJumbo, setSelectedJumbo] = useState<JumboRoll | null>(null);
  const [cuts, setCuts] = useState<SlitCut[]>([
    { id: '1', sequence: 1, widthMm: 1000, netWeightKg: 1245, serialNumber: 'SLT-01-JMB-2026-088', coreMm: 76, targetDiameterMm: 1250, customer: 'شركة الأهرام لكرتون التعبئة' },
    { id: '2', sequence: 2, widthMm: 1000, netWeightKg: 1245, serialNumber: 'SLT-02-JMB-2026-088', coreMm: 76, targetDiameterMm: 1250, customer: 'مصنع الشرق الأوسط للورق' },
    { id: '3', sequence: 3, widthMm: 350, netWeightKg: 435, serialNumber: 'SLT-03-JMB-2026-088', coreMm: 76, targetDiameterMm: 1250, customer: 'العبور للطباعة والتغليف' },
  ]);
  const [trimWasteKg, setTrimWasteKg] = useState<number>(65);
  const [newCutWidth, setNewCutWidth] = useState<number>(500);
  const [newCutWeight, setNewCutWeight] = useState<number>(620);
  const [executingSlit, setExecutingSlit] = useState<boolean>(false);

  // QC Form State
  const [qcRollSerial, setQcRollSerial] = useState<string>('SLT-03-JMB-2026-088');
  const [qcGsm, setQcGsm] = useState<number>(150.2);
  const [qcMoisture, setQcMoisture] = useState<number>(7.3);
  const [qcCaliper, setQcCaliper] = useState<number>(220);
  const [qcBurst, setQcBurst] = useState<number>(392);
  const [qcCobb, setQcCobb] = useState<number>(33);
  const [qcDisposition, setQcDisposition] = useState<'RELEASED' | 'QUARANTINE' | 'REJECTED' | 'DOWNGRADED'>('RELEASED');
  const [qcNotes, setQcNotes] = useState<string>('تم الفحص المخبري وتأكيد مطابقة الوزن والشد والمحتوى الرطوبي');

  // Thermal Label State
  const [selectedLabelRollSerial, setSelectedLabelRollSerial] = useState<string>('SLT-01-JMB-2026-088');
  const [labelPrinter, setLabelPrinter] = useState<string>('Zebra ZT411 (صالة الإنتاج PM1)');
  const [labelCopies, setLabelCopies] = useState<number>(2);

  // Genealogy Search State
  const [genealogySearch, setGenealogySearch] = useState<string>('JMB-2026-088');

  // Stock Movement Filter State
  const [movementFilter, setMovementFilter] = useState<string>('ALL');

  // Register Form State
  const [regFullName, setRegFullName] = useState<string>('');
  const [regEmail, setRegEmail] = useState<string>('');
  const [regRole, setRegRole] = useState<'CHIEF_OPERATOR' | 'QC_INSPECTOR' | 'MILL_MANAGER' | 'WAREHOUSE_KEEPER'>('CHIEF_OPERATOR');
  const [regPlant, setRegPlant] = useState<string>('مصنع مصر للورق والكرتون • وحدة PM1');

  // Database Supabase Settings State
  const [dbUrl, setDbUrl] = useState<string>('');
  const [dbKey, setDbKey] = useState<string>('');
  const [dbStatusText, setDbStatusText] = useState<string>('');

  // Manual Movement Form State
  const [newMovSerial, setNewMovSerial] = useState<string>('SLT-02-JMB-2026-088');
  const [newMovType, setNewMovType] = useState<'PRODUCTION_RECEIPT' | 'STATUS_CHANGE' | 'LOCATION_TRANSFER' | 'DISPATCH' | 'SCRAP'>('LOCATION_TRANSFER');
  const [newMovWeight, setNewMovWeight] = useState<number>(1245);
  const [newMovFrom, setNewMovFrom] = useState<string>('محطة فحص الجودة QC Lab');
  const [newMovTo, setNewMovTo] = useState<string>('مستودع A - ممر 04 - رصة 2');
  const [newMovReason, setNewMovReason] = useState<string>('نقل للمستودع بعد انتهاء الاختبارات');

  // Load Data on Startup
  const loadAllData = async () => {
    try {
      const u = await db.getUsers();
      setUsers(u);
      if (u.length > 0 && !currentUser) setCurrentUser(u[0]);

      const j = await db.getJumboRolls();
      setJumbos(j);
      if (j.length > 0 && !selectedJumbo) setSelectedJumbo(j[0]);

      const c = await db.getChildRolls();
      setChildRolls(c);

      const q = await db.getQCTests();
      setQcTests(q);

      const m = await db.getMovements();
      setMovements(m);
    } catch (err) {
      console.error('Error loading data:', err);
    }
  };

  useEffect(() => {
    loadAllData();
    const config = db.getSupabaseConfig();
    setDbUrl(config.url);
    setDbKey(config.key);
  }, []);

  // Live Mill Clock update
  useEffect(() => {
    const updateClock = () => {
      const now = new Date();
      const h = String(now.getHours()).padStart(2, '0');
      const m = String(now.getMinutes()).padStart(2, '0');
      const s = String(now.getSeconds()).padStart(2, '0');
      const ms = String(now.getMilliseconds()).padStart(3, '0');
      setCurrentTimeStr(`${h}:${m}:${s}.${ms}`);
    };
    updateClock();
    const timer = setInterval(updateClock, 100);
    return () => clearInterval(timer);
  }, []);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 4000);
  };

  // Calculations for Slitting Deckle
  const currentJumboWidth = selectedJumbo ? selectedJumbo.deckleWidthMm : 2400;
  const currentJumboNetWeight = selectedJumbo ? selectedJumbo.netWeightKg : 3000;
  const totalCutsWidth = cuts.reduce((sum, c) => sum + c.widthMm, 0);
  const trimWasteMm = Math.max(0, currentJumboWidth - totalCutsWidth);
  const totalChildWeight = cuts.reduce((sum, c) => sum + c.netWeightKg, 0);
  const massBalanceVarianceKg = currentJumboNetWeight - (totalChildWeight + trimWasteKg);
  const massBalanceVariancePct = ((Math.abs(massBalanceVarianceKg) / currentJumboNetWeight) * 100);
  const isWidthExceeded = totalCutsWidth > currentJumboWidth;
  const isMassBalanceValid = massBalanceVariancePct <= 1.0;

  // Add Cut Knife
  const handleAddCut = () => {
    if (newCutWidth <= 0 || newCutWeight <= 0) {
      alert('يرجى إدخال أبعاد صحيحة للرول');
      return;
    }
    const nextSeq = cuts.length + 1;
    const parentSerial = selectedJumbo ? selectedJumbo.serialNumber : 'JMB-2026-088';
    const newCut: SlitCut = {
      id: String(Date.now()),
      sequence: nextSeq,
      widthMm: Number(newCutWidth),
      netWeightKg: Number(newCutWeight),
      serialNumber: `SLT-0${nextSeq}-${parentSerial}`,
      coreMm: 76,
      targetDiameterMm: 1250,
      customer: 'عميل تصنيع كرتون محلي'
    };
    setCuts([...cuts, newCut]);
    showToast(`تمت إضافة سكين التقطيع رقم ${nextSeq} بعرض ${newCutWidth} مم`);
  };

  const handleDeleteCut = (id: string) => {
    setCuts(cuts.filter(c => c.id !== id));
  };

  // Execute Slitting Run
  const handleExecuteSlitting = async () => {
    if (isWidthExceeded) {
      alert('خطأ: إجمالي عرض الرولات يتجاوز عرض الرول الأم!');
      return;
    }
    setExecutingSlit(true);
    try {
      const parentSerial = selectedJumbo ? selectedJumbo.serialNumber : 'JMB-2026-088';
      const operator = currentUser ? currentUser.fullName : 'م. أحمد محمود';
      
      const newRolls = await db.saveSlittingRun(parentSerial, cuts, operator);
      await loadAllData();
      
      setExecutingSlit(false);
      showToast(`تم تنفيذ أمر التقطيع وتوليد ${newRolls.length} رول فرعي وحفظ القيود في سجل المخزن بنجاح!`);
      setActiveTab('roll-genealogy');
    } catch (e) {
      setExecutingSlit(false);
      alert('حدث خطأ أثناء تنفيذ أمر التقطيع');
    }
  };

  // Save QC Test
  const handleSaveQCTest = async () => {
    if (!qcRollSerial) {
      alert('يرجى تحديد سيريال الرول المراد فحصه');
      return;
    }
    const inspector = currentUser ? currentUser.fullName : 'ك. مصطفى إبراهيم';
    await db.saveQCTest({
      rollSerial: qcRollSerial,
      grade: 'Testliner Grade-A',
      actualGsm: Number(qcGsm),
      targetGsm: 150,
      moisturePct: Number(qcMoisture),
      caliperMicrons: Number(qcCaliper),
      burstKpa: Number(qcBurst),
      cobb60: Number(qcCobb),
      disposition: qcDisposition,
      inspector,
      labNotes: qcNotes
    });
    await loadAllData();
    showToast(`تم اعتماد وتوثيق فحص الجودة للرول ${qcRollSerial} بالحالة [${qcDisposition}]`);
  };

  // Register New User
  const handleRegisterUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!regFullName || !regEmail) {
      alert('يرجى كتابة الاسم والبريد الإلكتروني');
      return;
    }
    const newUser = await db.registerUser({
      fullName: regFullName,
      email: regEmail,
      role: regRole,
      plant: regPlant
    });
    setCurrentUser(newUser);
    setShowAuthModal(false);
    setRegFullName('');
    setRegEmail('');
    await loadAllData();
    showToast(`مرحباً بك ${newUser.fullName}! تم تسجيل حسابك بصلاحية [${newUser.role}] بنجاح.`);
  };

  // Save Supabase Configuration
  const handleSaveDbConfig = () => {
    db.setSupabaseConfig(dbUrl, dbKey);
    const connected = db.isSupabaseConnected();
    setDbStatusText(connected ? 'تم الاتصال بقاعدة بيانات Supabase بنجاح! سيتم التزامن السحابي الآن.' : 'تم التبديل للتخزين المحلي عالي السرعة (Local Persistence).');
    showToast(connected ? 'متصل بسحابة Supabase PostgreSQL' : 'يعمل الآن في وضع التخزين المحلي الآمن');
    setTimeout(() => setShowDbModal(false), 1500);
  };

  // Add Manual Movement
  const handleAddManualMovement = async (e: React.FormEvent) => {
    e.preventDefault();
    const actor = currentUser ? currentUser.fullName : 'م. أحمد محمود';
    await db.addMovement({
      rollSerial: newMovSerial,
      rollCategory: newMovSerial.startsWith('JMB') ? 'جامبو' : 'فرعي',
      movementType: newMovType,
      netWeightKg: Number(newMovWeight),
      fromLocation: newMovFrom,
      toLocation: newMovTo,
      actor,
      reason: newMovReason
    });
    await loadAllData();
    setShowMovementModal(false);
    showToast(`تم توثيق حركة المخزن للرول ${newMovSerial} بنجاح!`);
  };

  // Get active roll for label
  const activeLabelRoll = childRolls.find(r => r.serialNumber === selectedLabelRollSerial) || childRolls[0] || {
    serialNumber: 'SLT-01-JMB-2026-088',
    parentJumboSerial: 'JMB-2026-088',
    grade: 'Testliner Grade-A',
    widthMm: 1000,
    netWeightKg: 1245,
    grossWeightKg: 1247,
    gsm: 150,
    coreMm: 76,
    diameterMm: 1250,
    location: 'مستودع A - ممر 04 - رصة 2',
    operatorName: 'م. أحمد محمود',
    createdAt: '2026-10-24 14:45:00',
    status: 'QC_PASSED'
  };

  // Filtered movements
  const filteredMovements = movements.filter(m => {
    if (movementFilter === 'ALL') return true;
    return m.movementType === movementFilter;
  });

  return (
    <div className="bg-[#10131a] text-[#e1e2ec] min-h-screen antialiased selection:bg-[#c88a35] selection:text-[#452900] flex flex-col font-sans">
      
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 left-6 z-50 bg-[#1d1f27] border-2 border-[#4edea3] text-[#e1e2ec] px-5 py-3 shadow-2xl flex items-center gap-3 animate-bounce">
          <span className="material-symbols-outlined text-[#4edea3] text-[24px]">verified</span>
          <span className="font-medium text-sm">{toastMessage}</span>
        </div>
      )}

      {/* TOP HEADER */}
      <header className="fixed top-0 left-0 right-0 h-16 z-50 bg-[#0b0e15]/95 backdrop-blur-md flex items-center justify-between px-6 border-b border-[#32353d]/40">
        <div className="flex items-center gap-5">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-[#c88a35] flex items-center justify-center">
              <span className="material-symbols-outlined text-[#452900] text-[20px]">precision_manufacturing</span>
            </div>
            <div className="flex flex-col">
              <span className="text-[17px] font-bold text-[#feb95f] tracking-tight">PaperERP Enterprise</span>
              <span className="text-[10px] text-[#d6c3b2]">مصنع مصر للورق والكرتون • وحدة التحكم الرئيسية PM1</span>
            </div>
          </div>

          <div className="h-6 w-px bg-[#32353d]/50 hidden md:block"></div>

          <div className="hidden lg:flex items-center gap-3 text-[11px] font-mono">
            <div className="flex items-center gap-1.5 px-2.5 py-1 bg-[#191b23] border border-[#32353d]/60">
              <span className="w-2 h-2 rounded-full bg-[#4edea3] animate-pulse"></span>
              <span className="text-[#4edea3] font-semibold">LAN 192.168.9.223</span>
              <span className="text-[#94a3b8]">(متصل - صالة الإنتاج PM1)</span>
            </div>
            <div className="flex items-center gap-1.5 px-2.5 py-1 bg-[#191b23] border border-[#32353d]/60 font-sans">
              <span className="material-symbols-outlined text-[#feb95f] text-[14px]">schedule</span>
              <span className="text-[#e1e2ec]">وردية المساء • خط PM1</span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {/* Live Mill Clock */}
          <div className="hidden sm:flex flex-col items-end px-3 py-1 bg-[#191b23] border border-[#32353d]/40 font-mono">
            <span className="text-[14px] text-[#feb95f] tracking-wider font-semibold">{currentTimeStr || '14:48:22.840'}</span>
            <span className="text-[9px] text-[#94a3b8] tracking-widest font-sans">UTC+02:00 CAIRO MILL TIME</span>
          </div>

          {/* Database Cloud Sync Status Button */}
          <button 
            onClick={() => setShowDbModal(true)}
            className="flex items-center gap-1.5 px-2.5 py-1.5 bg-[#1d1f27] hover:bg-[#272a32] border border-[#32353d] text-xs transition-colors"
            title="إعدادات قاعدة البيانات والسحابة"
          >
            <span className={`w-2 h-2 rounded-full ${db.isSupabaseConnected() ? 'bg-[#4edea3]' : 'bg-[#feb95f]'}`}></span>
            <span className="material-symbols-outlined text-[16px] text-[#feb95f]">database</span>
            <span className="hidden md:inline font-mono text-[11px]">
              {db.isSupabaseConnected() ? 'Supabase PostgreSQL' : 'Local Persistence'}
            </span>
          </button>

          {/* User Account / Register Button */}
          <div className="flex items-center gap-2 pr-2 border-r border-[#32353d]/50">
            <button 
              onClick={() => setShowAuthModal(true)}
              className="flex items-center gap-2 px-2.5 py-1 bg-[#1d1f27] hover:bg-[#272a32] border border-[#32353d] transition-colors"
            >
              <div className="hidden md:flex flex-col text-right">
                <span className="text-[11px] font-bold text-[#e1e2ec]">{currentUser ? currentUser.fullName : 'أحمد محمود'}</span>
                <span className="text-[9px] text-[#feb95f] font-mono">{currentUser ? currentUser.role : 'CHIEF_OPERATOR'}</span>
              </div>
              <div className="w-8 h-8 rounded-full bg-[#feb95f] flex items-center justify-center">
                <span className="material-symbols-outlined text-[#472a00] text-[18px]">person</span>
              </div>
            </button>
          </div>
        </div>
      </header>

      {/* MAIN CONTAINER */}
      <div className="flex pt-16 min-h-screen">
        
        {/* RIGHT SIDEBAR (Industrial Navigation) */}
        <aside className="w-72 bg-[#0b0e15] border-l border-[#32353d]/40 flex flex-col justify-between shrink-0 z-40 fixed right-0 top-16 bottom-0">
          <div className="py-3">
            <div className="px-5 mb-3">
              <span className="text-[10px] text-[#9e8e7e] tracking-widest font-mono uppercase block">وحدات التشغيل والتحكم INDUSTRIAL GATES</span>
            </div>

            <nav className="space-y-1 px-2 font-sans">
              {/* Tab 1 */}
              <button
                onClick={() => setActiveTab('operational-cockpit')}
                className={`w-full flex items-center justify-between px-3 py-2.5 transition-colors border-r-2 ${
                  activeTab === 'operational-cockpit'
                    ? 'bg-[#c88a35] text-[#452900] border-[#feb95f] font-bold'
                    : 'text-[#d6c3b2] hover:bg-[#1d1f27] hover:text-[#e1e2ec] border-transparent'
                }`}
              >
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[20px]">grid_view</span>
                  <div className="flex flex-col text-right">
                    <span className="text-[13px]">نظرة عامة على صالة الإنتاج</span>
                    <span className="text-[9px] opacity-75 font-mono uppercase">Operational Cockpit</span>
                  </div>
                </div>
                <span className="text-[10px] font-mono text-[#4edea3]">ONLINE</span>
              </button>

              {/* Tab 2 */}
              <button
                onClick={() => setActiveTab('deckle-calculator')}
                className={`w-full flex items-center justify-between px-3 py-2.5 transition-colors border-r-2 ${
                  activeTab === 'deckle-calculator'
                    ? 'bg-[#c88a35] text-[#452900] border-[#feb95f] font-bold'
                    : 'text-[#d6c3b2] hover:bg-[#1d1f27] hover:text-[#e1e2ec] border-transparent'
                }`}
              >
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[20px]">straighten</span>
                  <div className="flex flex-col text-right">
                    <span className="text-[13px]">حاسبة تقطيع الرولات</span>
                    <span className="text-[9px] opacity-75 font-mono uppercase">Deckle Calculator</span>
                  </div>
                </div>
                <span className="text-[10px] font-mono opacity-80">v4.2</span>
              </button>

              {/* Tab 3 */}
              <button
                onClick={() => setActiveTab('roll-genealogy')}
                className={`w-full flex items-center justify-between px-3 py-2.5 transition-colors border-r-2 ${
                  activeTab === 'roll-genealogy'
                    ? 'bg-[#c88a35] text-[#452900] border-[#feb95f] font-bold'
                    : 'text-[#d6c3b2] hover:bg-[#1d1f27] hover:text-[#e1e2ec] border-transparent'
                }`}
              >
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[20px]">account_tree</span>
                  <div className="flex flex-col text-right">
                    <span className="text-[13px]">شجرة نسب الرول</span>
                    <span className="text-[9px] opacity-75 font-mono uppercase">Roll Genealogy</span>
                  </div>
                </div>
                <span className="text-[10px] font-mono opacity-80">PARENT</span>
              </button>

              {/* Tab 4 */}
              <button
                onClick={() => setActiveTab('qc-laboratory-gate')}
                className={`w-full flex items-center justify-between px-3 py-2.5 transition-colors border-r-2 ${
                  activeTab === 'qc-laboratory-gate'
                    ? 'bg-[#c88a35] text-[#452900] border-[#feb95f] font-bold'
                    : 'text-[#d6c3b2] hover:bg-[#1d1f27] hover:text-[#e1e2ec] border-transparent'
                }`}
              >
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[20px]">biotech</span>
                  <div className="flex flex-col text-right">
                    <span className="text-[13px]">مختبر فحص الجودة</span>
                    <span className="text-[9px] opacity-75 font-mono uppercase">QC Laboratory Gate</span>
                  </div>
                </div>
                <span className="text-[10px] font-mono text-[#4edea3]">ISO 187</span>
              </button>

              {/* Tab 5 */}
              <button
                onClick={() => setActiveTab('thermal-label-printer')}
                className={`w-full flex items-center justify-between px-3 py-2.5 transition-colors border-r-2 ${
                  activeTab === 'thermal-label-printer'
                    ? 'bg-[#c88a35] text-[#452900] border-[#feb95f] font-bold'
                    : 'text-[#d6c3b2] hover:bg-[#1d1f27] hover:text-[#e1e2ec] border-transparent'
                }`}
              >
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[20px]">print</span>
                  <div className="flex flex-col text-right">
                    <span className="text-[13px]">طباعة الباركود الحراري</span>
                    <span className="text-[9px] opacity-75 font-mono uppercase">Thermal Label Printer</span>
                  </div>
                </div>
                <span className="text-[10px] font-mono opacity-80">ZEBRA</span>
              </button>

              {/* Tab 6 */}
              <button
                onClick={() => setActiveTab('immutable-stock-ledger')}
                className={`w-full flex items-center justify-between px-3 py-2.5 transition-colors border-r-2 ${
                  activeTab === 'immutable-stock-ledger'
                    ? 'bg-[#c88a35] text-[#452900] border-[#feb95f] font-bold'
                    : 'text-[#d6c3b2] hover:bg-[#1d1f27] hover:text-[#e1e2ec] border-transparent'
                }`}
              >
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[20px]">history_edu</span>
                  <div className="flex flex-col text-right">
                    <span className="text-[13px]">سجل حركات المخزن</span>
                    <span className="text-[9px] opacity-75 font-mono uppercase">Immutable Stock Ledger</span>
                  </div>
                </div>
                <span className="text-[10px] font-mono opacity-80">AUDIT</span>
              </button>

              {/* Tab 7 */}
              <button
                onClick={() => setActiveTab('rugged-tablet')}
                className={`w-full flex items-center justify-between px-3 py-2.5 transition-colors border-r-2 ${
                  activeTab === 'rugged-tablet'
                    ? 'bg-[#c88a35] text-[#452900] border-[#feb95f] font-bold'
                    : 'text-[#d6c3b2] hover:bg-[#1d1f27] hover:text-[#e1e2ec] border-transparent'
                }`}
              >
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[20px]">tablet_android</span>
                  <div className="flex flex-col text-right">
                    <span className="text-[13px]">شاشة التابلت الصناعي</span>
                    <span className="text-[9px] opacity-75 font-mono uppercase">Rugged Floor Tablet</span>
                  </div>
                </div>
                <span className="text-[10px] font-mono text-[#feb95f]">TOUCH</span>
              </button>
            </nav>
          </div>

          {/* Bottom Machine Telemetry Sidebar Widget */}
          <div className="p-4 bg-[#191b23] border-t border-[#32353d]/40">
            <div className="flex items-center justify-between text-[11px] font-mono text-[#d6c3b2] mb-1.5">
              <span>REEL STAND PM1 SPEED</span>
              <span className="text-[#feb95f] font-bold">1,240 M/MIN</span>
            </div>
            <div className="w-full bg-[#10131a] h-2 overflow-hidden border border-[#32353d]/40">
              <div className="bg-[#c88a35] h-full w-[82%]"></div>
            </div>
            <div className="flex items-center justify-between mt-2 text-[#9e8e7e] font-mono text-[10px]">
              <span>BASIS WT: 150 GSM</span>
              <span className="text-[#4edea3]">SLIT EFFICIENCY: 99.4%</span>
            </div>
          </div>
        </aside>

        {/* CONTENT AREA (Offset by sidebar width 288px on right) */}
        <main className="flex-1 mr-72 p-6 overflow-y-auto">

          {/* TAB 1: OPERATIONAL COCKPIT */}
          {activeTab === 'operational-cockpit' && (
            <div className="space-y-6">
              {/* Context Header Bar */}
              <div className="bg-[#0b0e15] p-4 border border-[#32353d]/40 flex flex-wrap items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="w-3 h-3 bg-[#4edea3]"></div>
                  <div>
                    <h1 className="text-[18px] font-bold text-[#feb95f]">صالة الإنتاج الرئيسية PM1 • وحدة التحكّم التنفيذي</h1>
                    <span className="text-[10px] font-mono text-[#bec6de] tracking-wider">EXECUTIVE OPERATIONAL COCKPIT • MILL SHIFT A</span>
                  </div>
                </div>

                <div className="flex items-center gap-3 font-mono text-xs">
                  <div className="bg-[#1d1f27] px-3 py-1.5 border border-[#32353d]">
                    <span className="text-[#9e8e7e]">سرعة السحب: </span>
                    <span className="text-[#feb95f] font-bold">850 م/دقيقة</span>
                  </div>
                  <div className="bg-[#1d1f27] px-3 py-1.5 border border-[#32353d]">
                    <span className="text-[#9e8e7e]">عرض الماكينة: </span>
                    <span className="text-[#e1e2ec] font-bold">2400 مم</span>
                  </div>
                  <div className="bg-[#1d1f27] px-3 py-1.5 border border-[#32353d]">
                    <span className="text-[#9e8e7e]">صنف الورق: </span>
                    <span className="text-[#4edea3] font-bold">150 GSM KRAFT</span>
                  </div>
                </div>
              </div>

              {/* KPI Metrics Ribbon */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                {/* Metric 1 */}
                <div className="bg-[#0b0e15] p-4 border border-[#32353d]/40 flex flex-col justify-between">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <span className="text-xs text-[#e1e2ec]">إجمالي الإنتاج اليومي</span>
                      <span className="block text-[9px] font-mono text-[#94a3b8]">DAILY GROSS TONNAGE</span>
                    </div>
                    <span className="text-[10px] font-mono bg-[#4edea3]/10 text-[#4edea3] px-1.5 py-0.5 border border-[#4edea3]/30">+4.8% ↑</span>
                  </div>
                  <div className="flex items-baseline gap-2 mb-2 font-mono">
                    <span className="text-3xl font-bold text-[#feb95f]">142.500</span>
                    <span className="text-xs text-[#94a3b8]">طن متري / MT</span>
                  </div>
                  <div className="w-full bg-[#1d1f27] h-1.5 mb-2">
                    <div className="bg-[#c88a35] h-full w-[88%]"></div>
                  </div>
                  <span className="text-[10px] font-mono text-[#94a3b8]">المستهدف اليومي: 136.000 طن</span>
                </div>

                {/* Metric 2 */}
                <div className="bg-[#0b0e15] p-4 border border-[#32353d]/40 flex flex-col justify-between">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <span className="text-xs text-[#e1e2ec]">رولات الجامبو النشطة</span>
                      <span className="block text-[9px] font-mono text-[#94a3b8]">ACTIVE POPE JUMBO REELS</span>
                    </div>
                    <span className="text-[10px] font-mono bg-[#feb95f]/10 text-[#feb95f] px-1.5 py-0.5 border border-[#feb95f]/30">POPE-PM1</span>
                  </div>
                  <div className="flex items-baseline gap-2 mb-2 font-mono">
                    <span className="text-3xl font-bold text-[#e1e2ec]">{jumbos.length}</span>
                    <span className="text-xs text-[#94a3b8]">رول جاهز للتقطيع</span>
                  </div>
                  <div className="w-full bg-[#1d1f27] h-1.5 mb-2">
                    <div className="bg-[#4edea3] h-full w-[75%]"></div>
                  </div>
                  <span className="text-[10px] font-mono text-[#94a3b8]">إجمالي الكتلة: 36,450 كجم</span>
                </div>

                {/* Metric 3 */}
                <div className="bg-[#0b0e15] p-4 border border-[#32353d]/40 flex flex-col justify-between">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <span className="text-xs text-[#e1e2ec]">نسبة الهالك والتشذيب</span>
                      <span className="block text-[9px] font-mono text-[#94a3b8]">TRIM &amp; SCRAP RATIO</span>
                    </div>
                    <span className="text-[10px] font-mono bg-[#4edea3]/10 text-[#4edea3] px-1.5 py-0.5 border border-[#4edea3]/30">OPTIMAL</span>
                  </div>
                  <div className="flex items-baseline gap-2 mb-2 font-mono">
                    <span className="text-3xl font-bold text-[#4edea3]">2.18%</span>
                    <span className="text-xs text-[#94a3b8]">أقل من 3.0%</span>
                  </div>
                  <div className="w-full bg-[#1d1f27] h-1.5 mb-2">
                    <div className="bg-[#4edea3] h-full w-[28%]"></div>
                  </div>
                  <span className="text-[10px] font-mono text-[#94a3b8]">وفر مادي: 680 كجم / الوردية</span>
                </div>

                {/* Metric 4 */}
                <div className="bg-[#0b0e15] p-4 border border-[#32353d]/40 flex flex-col justify-between">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <span className="text-xs text-[#e1e2ec]">كفاءة خطوط التقطيع OEE</span>
                      <span className="block text-[9px] font-mono text-[#94a3b8]">SLITTING LINES OVERALL OEE</span>
                    </div>
                    <span className="text-[10px] font-mono bg-[#feb95f]/10 text-[#feb95f] px-1.5 py-0.5 border border-[#feb95f]/30">SHIFT A</span>
                  </div>
                  <div className="flex items-baseline gap-2 mb-2 font-mono">
                    <span className="text-3xl font-bold text-[#feb95f]">94.2%</span>
                    <span className="text-xs text-[#94a3b8]">جاهزية السكاكين</span>
                  </div>
                  <div className="w-full bg-[#1d1f27] h-1.5 mb-2">
                    <div className="bg-[#c88a35] h-full w-[94%]"></div>
                  </div>
                  <span className="text-[10px] font-mono text-[#94a3b8]">محطة السكاكين SLT-04 تعمل</span>
                </div>
              </div>

              {/* Active Jumbos Table */}
              <div className="bg-[#0b0e15] border border-[#32353d]/40 p-5">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <span className="material-symbols-outlined text-[#feb95f] text-[20px]">album</span>
                    <h2 className="text-base font-bold text-[#e1e2ec]">رولات الجامبو المنتجة من خط الورق (Jumbo Mother Reels)</h2>
                  </div>
                  <button 
                    onClick={() => setActiveTab('deckle-calculator')}
                    className="px-3 py-1.5 bg-[#c88a35] hover:bg-[#feb95f] text-[#452900] text-xs font-bold flex items-center gap-1.5 transition-colors"
                  >
                    <span className="material-symbols-outlined text-[16px]">content_cut</span>
                    <span>بدء أمر تقطيع جديد</span>
                  </button>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-right text-xs">
                    <thead>
                      <tr className="border-b border-[#32353d]/60 text-[#9e8e7e] font-mono text-[11px]">
                        <th className="py-2.5 px-3">سيريال الرول الأم</th>
                        <th className="py-2.5 px-3">صنف الورق</th>
                        <th className="py-2.5 px-3">الكراماج (GSM)</th>
                        <th className="py-2.5 px-3">عرض الديكل (مم)</th>
                        <th className="py-2.5 px-3">الوزن الصافي (كجم)</th>
                        <th className="py-2.5 px-3">الماكينة</th>
                        <th className="py-2.5 px-3">الحالة</th>
                        <th className="py-2.5 px-3">الإجراء</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#32353d]/30 font-mono">
                      {jumbos.map((j) => (
                        <tr key={j.id} className="hover:bg-[#191b23] transition-colors">
                          <td className="py-3 px-3 text-[#feb95f] font-bold">{j.serialNumber}</td>
                          <td className="py-3 px-3 font-sans text-[#e1e2ec]">{j.grade}</td>
                          <td className="py-3 px-3">{j.targetGsm} GSM</td>
                          <td className="py-3 px-3 font-bold">{j.deckleWidthMm} مم</td>
                          <td className="py-3 px-3 text-[#4edea3] font-bold">{j.netWeightKg.toLocaleString()} كجم</td>
                          <td className="py-3 px-3 font-sans text-[#d6c3b2]">{j.machineId}</td>
                          <td className="py-3 px-3">
                            <span className="px-2 py-0.5 bg-[#4edea3]/10 text-[#4edea3] border border-[#4edea3]/30 text-[10px]">
                              {j.status}
                            </span>
                          </td>
                          <td className="py-3 px-3">
                            <button
                              onClick={() => {
                                setSelectedJumbo(j);
                                setActiveTab('deckle-calculator');
                              }}
                              className="px-2.5 py-1 bg-[#1d1f27] hover:bg-[#c88a35] hover:text-[#452900] text-[#feb95f] border border-[#32353d] transition-colors font-sans text-[11px]"
                            >
                              تحميل للمقص
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: DECKLE CALCULATOR */}
          {activeTab === 'deckle-calculator' && (
            <div className="space-y-6">
              {/* Header Bar */}
              <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pb-4 border-b border-[#32353d]/40">
                <div>
                  <div className="flex items-center gap-2 text-[#9e8e7e] font-mono text-xs uppercase tracking-wider mb-1">
                    <span>وحدة التقطيع والتشذيب SLITTING &amp; REWINDING</span>
                    <span>•</span>
                    <span className="text-[#feb95f]">DECKLE RUN MATRIX</span>
                  </div>
                  <h1 className="text-xl font-bold text-[#e1e2ec]">حاسبة تقطيع الرولات وميزان الكتلة الرياضي</h1>
                </div>

                <div className="flex items-center gap-3 font-mono text-xs">
                  <div className="px-3 py-1.5 bg-[#191b23] border border-[#32353d]">
                    <span className="text-[#9e8e7e]">سرعة السكاكين: </span>
                    <span className="text-[#feb95f] font-bold">1,450 م/دقيقة</span>
                  </div>
                  <div className="px-3 py-1.5 bg-[#191b23] border border-[#32353d]">
                    <span className="text-[#9e8e7e]">ضغط الشد: </span>
                    <span className="text-[#e1e2ec] font-bold">4.85 بار</span>
                  </div>
                </div>
              </div>

              {/* Selected Jumbo Info Card */}
              <div className="bg-[#191b23] border border-[#32353d]/40 p-4">
                <div className="flex flex-col xl:flex-row items-stretch xl:items-center justify-between gap-4">
                  <div className="flex items-start gap-4">
                    <div className="w-12 h-12 bg-[#272a32] border border-[#feb95f]/40 flex flex-col items-center justify-center shrink-0">
                      <span className="material-symbols-outlined text-[#feb95f] text-[24px]">album</span>
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] font-mono text-[#9e8e7e]">الرول الأم المحمل على المقص</span>
                        <span className="px-1.5 py-0.2 bg-[#4edea3]/10 text-[#4edea3] font-mono text-[10px] border border-[#4edea3]/30">LOCKED &amp; READY</span>
                      </div>
                      <div className="text-lg font-bold text-[#feb95f] font-mono">{selectedJumbo?.serialNumber || 'JMB-2026-088'}</div>
                      <div className="text-xs text-[#d6c3b2]">
                        الصنف: <strong className="text-[#e1e2ec]">{selectedJumbo?.grade || 'Testliner Grade-A'}</strong> (150 GSM)
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 font-mono text-xs bg-[#0b0e15] p-3 border border-[#32353d]/40">
                    <div className="p-2 border-r-2 border-[#feb95f]">
                      <span className="text-[9px] text-[#9e8e7e] block">عرض الديكل الصافي</span>
                      <span className="text-lg font-bold text-[#feb95f]">{currentJumboWidth}</span> <span className="text-[10px] text-[#94a3b8]">مم</span>
                    </div>
                    <div className="p-2">
                      <span className="text-[9px] text-[#9e8e7e] block">الوزن الإجمالي</span>
                      <span className="text-lg font-bold text-[#e1e2ec]">{selectedJumbo ? selectedJumbo.grossWeightKg : 3002}</span> <span className="text-[10px] text-[#94a3b8]">كجم</span>
                    </div>
                    <div className="p-2">
                      <span className="text-[9px] text-[#9e8e7e] block">ماسورة اللف (Core)</span>
                      <span className="text-lg font-bold text-[#e1e2ec]">76 مم</span>
                    </div>
                    <div className="p-2 bg-[#c88a35]/10">
                      <span className="text-[9px] text-[#feb95f] block font-bold">الوزن الصافي المتاح</span>
                      <span className="text-lg font-bold text-[#4edea3]">{currentJumboNetWeight.toLocaleString()}</span> <span className="text-[10px] text-[#94a3b8]">كجم</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* VISUAL DECKLE CUT BAR */}
              <div className="bg-[#0b0e15] border border-[#32353d]/40 p-5">
                <div className="flex items-center justify-between mb-3 text-xs">
                  <div className="flex items-center gap-2">
                    <span className="material-symbols-outlined text-[#feb95f] text-[18px]">straighten</span>
                    <span className="font-bold">المخطط التفاعلي لتوزيع سكاكين التقطيع (Visual Knife Deckle Span)</span>
                  </div>
                  <div className="font-mono text-xs">
                    <span>الإجمالي المستخدم: </span>
                    <span className={`font-bold ${isWidthExceeded ? 'text-[#ffb4ab]' : 'text-[#4edea3]'}`}>{totalCutsWidth} مم</span>
                    <span className="text-[#9e8e7e]"> / {currentJumboWidth} مم</span>
                  </div>
                </div>

                {/* Progress Visual Bar */}
                <div className="w-full h-12 bg-[#1d1f27] border border-[#32353d] flex overflow-hidden p-1 gap-1">
                  {cuts.map((c, idx) => {
                    const widthPct = (c.widthMm / currentJumboWidth) * 100;
                    return (
                      <div
                        key={c.id}
                        style={{ width: `${widthPct}%` }}
                        className="bg-[#c88a35] hover:bg-[#feb95f] transition-colors flex flex-col items-center justify-center text-[#452900] font-mono text-[11px] font-bold overflow-hidden cursor-pointer shadow-sm"
                        title={`سكين ${idx + 1}: ${c.widthMm} مم (${c.netWeightKg} كجم)`}
                      >
                        <span>{c.widthMm} مم</span>
                        <span className="text-[9px] opacity-80">{c.netWeightKg} كجم</span>
                      </div>
                    );
                  })}
                  {trimWasteMm > 0 && (
                    <div
                      style={{ width: `${(trimWasteMm / currentJumboWidth) * 100}%` }}
                      className="bg-[#32353d] border-2 border-dashed border-[#9e8e7e] flex items-center justify-center text-[#9e8e7e] font-mono text-[10px] font-semibold"
                      title={`هالك التشذيب الجانبي (Trim Waste): ${trimWasteMm} مم`}
                    >
                      <span>تشذيب {trimWasteMm} مم</span>
                    </div>
                  )}
                </div>

                {isWidthExceeded && (
                  <div className="mt-3 p-3 bg-[#93000a]/20 border border-[#ffb4ab] text-[#ffb4ab] text-xs flex items-center gap-2 font-bold">
                    <span className="material-symbols-outlined text-[20px]">warning</span>
                    <span>تحذير: مجموع عرض السكاكين ({totalCutsWidth} مم) يتجاوز عرض الرول الأم ({currentJumboWidth} مم)!</span>
                  </div>
                )}
              </div>

              {/* Slit Cuts Configuration Table & Add Form */}
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                
                {/* Table of cuts (col-span 2) */}
                <div className="lg:col-span-2 bg-[#0b0e15] border border-[#32353d]/40 p-5">
                  <h3 className="text-sm font-bold text-[#e1e2ec] mb-3 flex items-center gap-2">
                    <span className="material-symbols-outlined text-[#feb95f] text-[18px]">table_rows</span>
                    <span>جدول مواصفات الرولات الفرعية الناتجة</span>
                  </h3>

                  <div className="overflow-x-auto">
                    <table className="w-full text-right text-xs">
                      <thead>
                        <tr className="border-b border-[#32353d]/60 text-[#9e8e7e] font-mono text-[11px]">
                          <th className="py-2 px-3">#</th>
                          <th className="py-2 px-3">سيريال الرول الفرعي</th>
                          <th className="py-2 px-3">العرض (مم)</th>
                          <th className="py-2 px-3">الوزن المحسوب</th>
                          <th className="py-2 px-3">الماسورة</th>
                          <th className="py-2 px-3">العميل المستهدف</th>
                          <th className="py-2 px-3">حذف</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-[#32353d]/30 font-mono">
                        {cuts.map((c, index) => (
                          <tr key={c.id} className="hover:bg-[#191b23]">
                            <td className="py-2.5 px-3 font-bold">{index + 1}</td>
                            <td className="py-2.5 px-3 text-[#feb95f] font-bold">{c.serialNumber}</td>
                            <td className="py-2.5 px-3 font-bold text-[#e1e2ec]">{c.widthMm} مم</td>
                            <td className="py-2.5 px-3 text-[#4edea3] font-bold">{c.netWeightKg} كجم</td>
                            <td className="py-2.5 px-3">{c.coreMm} مم</td>
                            <td className="py-2.5 px-3 font-sans text-[#d6c3b2]">{c.customer || 'غير محدد'}</td>
                            <td className="py-2.5 px-3">
                              <button
                                onClick={() => handleDeleteCut(c.id)}
                                className="w-7 h-7 bg-[#93000a]/20 hover:bg-[#93000a] text-[#ffb4ab] flex items-center justify-center transition-colors"
                                title="حذف السكين"
                              >
                                <span className="material-symbols-outlined text-[16px]">delete</span>
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Add Knife Cut & Mass Balance Box (col-span 1) */}
                <div className="space-y-4">
                  {/* Add knife form */}
                  <div className="bg-[#0b0e15] border border-[#32353d]/40 p-4">
                    <h3 className="text-xs font-bold text-[#feb95f] mb-3 flex items-center gap-1.5">
                      <span className="material-symbols-outlined text-[16px]">add_circle</span>
                      <span>إضافة سكين تقطيع جديدة</span>
                    </h3>

                    <div className="space-y-3">
                      <div>
                        <label className="text-[10px] text-[#9e8e7e] block mb-1">عرض الرول المقصوص (مم)</label>
                        <input
                          type="number"
                          value={newCutWidth}
                          onChange={(e) => setNewCutWidth(Number(e.target.value))}
                          className="w-full bg-[#191b23] border border-[#32353d] px-3 py-1.5 text-xs text-[#e1e2ec] font-mono focus:border-[#feb95f] outline-none"
                        />
                      </div>

                      <div>
                        <label className="text-[10px] text-[#9e8e7e] block mb-1">الوزن التقديري الصافي (كجم)</label>
                        <input
                          type="number"
                          value={newCutWeight}
                          onChange={(e) => setNewCutWeight(Number(e.target.value))}
                          className="w-full bg-[#191b23] border border-[#32353d] px-3 py-1.5 text-xs text-[#e1e2ec] font-mono focus:border-[#feb95f] outline-none"
                        />
                      </div>

                      <button
                        onClick={handleAddCut}
                        className="w-full py-2 bg-[#1d1f27] hover:bg-[#272a32] border border-[#feb95f] text-[#feb95f] text-xs font-bold transition-colors flex items-center justify-center gap-1"
                      >
                        <span className="material-symbols-outlined text-[16px]">add</span>
                        <span>إدراج في مصفوفة السكاكين</span>
                      </button>
                    </div>
                  </div>

                  {/* Mass Balance Box */}
                  <div className="bg-[#0b0e15] border border-[#32353d]/40 p-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs font-bold text-[#e1e2ec]">ميزان الكتلة الرياضي</span>
                      <span className={`px-2 py-0.5 text-[10px] font-mono border ${isMassBalanceValid ? 'bg-[#4edea3]/10 text-[#4edea3] border-[#4edea3]/30' : 'bg-[#93000a]/20 text-[#ffb4ab] border-[#ffb4ab]/40'}`}>
                        {isMassBalanceValid ? 'MATHEMATICALLY VERIFIED' : 'VARIANCE BREACH'}
                      </span>
                    </div>

                    <div className="text-[11px] font-mono space-y-1 text-[#d6c3b2]">
                      <div className="flex justify-between">
                        <span>وزن الجامبو الصافي:</span>
                        <span className="text-[#feb95f]">{currentJumboNetWeight} كجم</span>
                      </div>
                      <div className="flex justify-between">
                        <span>مجموع وزن الرولات:</span>
                        <span className="text-[#e1e2ec]">{totalChildWeight} كجم</span>
                      </div>
                      <div className="flex justify-between">
                        <span>هالك التشذيب (Trim):</span>
                        <input 
                          type="number" 
                          value={trimWasteKg} 
                          onChange={(e) => setTrimWasteKg(Number(e.target.value))}
                          className="w-16 bg-[#191b23] border border-[#32353d] px-1 text-left text-xs font-mono text-[#e1e2ec]"
                        />
                      </div>
                      <div className="flex justify-between border-t border-[#32353d]/40 pt-1 font-bold">
                        <span>فارق الميزان (Variance):</span>
                        <span className={isMassBalanceValid ? 'text-[#4edea3]' : 'text-[#ffb4ab]'}>
                          {massBalanceVarianceKg.toFixed(1)} كجم ({massBalanceVariancePct.toFixed(2)}%)
                        </span>
                      </div>
                    </div>

                    <button
                      disabled={executingSlit || isWidthExceeded}
                      onClick={handleExecuteSlitting}
                      className="w-full mt-4 py-2.5 bg-[#c88a35] hover:bg-[#feb95f] text-[#452900] text-xs font-bold flex items-center justify-center gap-2 transition-colors disabled:opacity-50"
                    >
                      <span className="material-symbols-outlined text-[18px]">verified</span>
                      <span>{executingSlit ? 'جاري تنفيذ العملية...' : 'تنفيذ أمر التقطيع وتوليد الرولات'}</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: ROLL GENEALOGY */}
          {activeTab === 'roll-genealogy' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between pb-4 border-b border-[#32353d]/40">
                <div>
                  <h1 className="text-xl font-bold text-[#e1e2ec]">شجرة نسب الرول وتتبع المصدر (Roll Genealogy Tree)</h1>
                  <span className="text-xs text-[#9e8e7e]">تتبع فيزيائي كامل من الرول الأم وحتى الرولات الفرعية والمستودعات</span>
                </div>

                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    value={genealogySearch}
                    onChange={(e) => setGenealogySearch(e.target.value)}
                    placeholder="ابحث برقم السيريال أو الباركود..."
                    className="bg-[#191b23] border border-[#32353d] px-3 py-1.5 text-xs text-[#e1e2ec] font-mono outline-none w-64"
                  />
                  <button className="px-3 py-1.5 bg-[#c88a35] text-[#452900] text-xs font-bold flex items-center gap-1">
                    <span className="material-symbols-outlined text-[16px]">search</span>
                    <span>فحص النسب</span>
                  </button>
                </div>
              </div>

              {/* Tree Diagram Visualizer */}
              <div className="bg-[#0b0e15] border border-[#32353d]/40 p-8 flex flex-col items-center">
                {/* Mother Roll Node */}
                <div className="bg-[#191b23] border-2 border-[#feb95f] p-4 w-96 text-center shadow-xl relative">
                  <div className="absolute -top-3 right-4 px-2 py-0.5 bg-[#feb95f] text-[#452900] text-[9px] font-bold font-mono">
                    MOTHER JUMBO REEL
                  </div>
                  <div className="text-lg font-bold font-mono text-[#feb95f] mt-1">{selectedJumbo?.serialNumber || 'JMB-2026-088'}</div>
                  <div className="text-xs text-[#e1e2ec] mt-1">صنف: {selectedJumbo?.grade || 'Testliner Grade-A'} (150 GSM)</div>
                  <div className="flex justify-center gap-3 text-[11px] font-mono text-[#4edea3] mt-2">
                    <span>عرض: {currentJumboWidth} مم</span>
                    <span>•</span>
                    <span>وزن: {currentJumboNetWeight} كجم</span>
                  </div>
                </div>

                {/* Connector Line */}
                <div className="w-0.5 h-10 bg-[#feb95f]"></div>

                {/* Slitting Run Station Badge */}
                <div className="px-4 py-1.5 bg-[#272a32] border border-[#32353d] text-xs font-mono text-[#feb95f] flex items-center gap-2">
                  <span className="material-symbols-outlined text-[16px]">content_cut</span>
                  <span>أمر تقطيع كهروميكانيكي SLT-RUN-2026-044 (مشغل: م. أحمد محمود)</span>
                </div>

                {/* Connector Line split */}
                <div className="w-0.5 h-10 bg-[#feb95f]"></div>

                {/* Daughter Rolls Grid */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 w-full max-w-4xl">
                  {childRolls.map((r) => (
                    <div key={r.id} className="bg-[#191b23] border border-[#32353d] p-4 flex flex-col justify-between hover:border-[#feb95f] transition-colors">
                      <div className="flex items-start justify-between mb-2">
                        <span className="text-[10px] font-mono text-[#feb95f]">رول فرعي #{r.sequence}</span>
                        <span className={`px-2 py-0.5 text-[9px] font-mono border ${r.status === 'QC_PASSED' ? 'bg-[#4edea3]/10 text-[#4edea3] border-[#4edea3]/30' : 'bg-[#feb95f]/10 text-[#feb95f] border-[#feb95f]/30'}`}>
                          {r.status}
                        </span>
                      </div>

                      <div className="text-base font-bold font-mono text-[#e1e2ec] mb-1">{r.serialNumber}</div>
                      <div className="text-xs text-[#d6c3b2] mb-3">عرض: {r.widthMm} مم • وزن: {r.netWeightKg} كجم</div>

                      <div className="text-[10px] font-mono text-[#9e8e7e] border-t border-[#32353d]/40 pt-2 flex items-center justify-between">
                        <span>الموقع: {r.location}</span>
                        <button
                          onClick={() => {
                            setSelectedLabelRollSerial(r.serialNumber);
                            setActiveTab('thermal-label-printer');
                          }}
                          className="text-[#feb95f] hover:underline flex items-center gap-0.5"
                        >
                          <span className="material-symbols-outlined text-[14px]">print</span>
                          <span>طباعة</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: QC LABORATORY GATE */}
          {activeTab === 'qc-laboratory-gate' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between pb-4 border-b border-[#32353d]/40">
                <div>
                  <h1 className="text-xl font-bold text-[#e1e2ec]">مختبر فحص الجودة وتصريح الشحن (QC Laboratory Gate)</h1>
                  <span className="text-xs text-[#9e8e7e]">اختبارات فيزيائية مخبرية دقيقة وفق معيار ISO 187 للورق والكرتون</span>
                </div>
                <div className="px-3 py-1 bg-[#4edea3]/10 text-[#4edea3] font-mono text-xs border border-[#4edea3]/30">
                  STANDARD: ISO 187 COMPLIANT
                </div>
              </div>

              {/* QC Testing Entry Form */}
              <div className="bg-[#0b0e15] border border-[#32353d]/40 p-6">
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
                  <div>
                    <label className="text-xs text-[#9e8e7e] block mb-1">اختر سيريال الرول المراد فحصه:</label>
                    <select
                      value={qcRollSerial}
                      onChange={(e) => setQcRollSerial(e.target.value)}
                      className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs font-mono text-[#feb95f] outline-none"
                    >
                      {childRolls.map((r) => (
                        <option key={r.id} value={r.serialNumber}>
                          {r.serialNumber} ({r.widthMm} مم - {r.netWeightKg} كجم)
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="text-xs text-[#9e8e7e] block mb-1">صنف الورق المستهدف:</label>
                    <input
                      type="text"
                      disabled
                      value="Testliner Grade-A (المستهدف: 150 GSM ± 2 GSM)"
                      className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs text-[#e1e2ec] outline-none"
                    />
                  </div>

                  <div>
                    <label className="text-xs text-[#9e8e7e] block mb-1">كبير مفتشي الجودة (Inspector):</label>
                    <input
                      type="text"
                      disabled
                      value={currentUser ? currentUser.fullName : 'ك. مصطفى إبراهيم'}
                      className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs text-[#e1e2ec] outline-none font-sans"
                    />
                  </div>
                </div>

                {/* Scientific Parameters Cards */}
                <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
                  <div className="bg-[#191b23] border border-[#32353d] p-3">
                    <span className="text-[10px] text-[#9e8e7e] block font-mono">الكراماج الفعلي (GSM)</span>
                    <input
                      type="number"
                      step="0.1"
                      value={qcGsm}
                      onChange={(e) => setQcGsm(Number(e.target.value))}
                      className="w-full bg-[#10131a] border border-[#32353d] px-2 py-1 mt-1 text-sm font-mono text-[#feb95f] font-bold"
                    />
                    <span className="text-[9px] text-[#4edea3] mt-1 block">ضمن النطاق المسموح</span>
                  </div>

                  <div className="bg-[#191b23] border border-[#32353d] p-3">
                    <span className="text-[10px] text-[#9e8e7e] block font-mono">المحتوى الرطوبي (%)</span>
                    <input
                      type="number"
                      step="0.1"
                      value={qcMoisture}
                      onChange={(e) => setQcMoisture(Number(e.target.value))}
                      className="w-full bg-[#10131a] border border-[#32353d] px-2 py-1 mt-1 text-sm font-mono text-[#e1e2ec] font-bold"
                    />
                    <span className="text-[9px] text-[#9e8e7e] mt-1 block">المعيار: 6.5 - 8.5%</span>
                  </div>

                  <div className="bg-[#191b23] border border-[#32353d] p-3">
                    <span className="text-[10px] text-[#9e8e7e] block font-mono">السُمك (Caliper Micron)</span>
                    <input
                      type="number"
                      value={qcCaliper}
                      onChange={(e) => setQcCaliper(Number(e.target.value))}
                      className="w-full bg-[#10131a] border border-[#32353d] px-2 py-1 mt-1 text-sm font-mono text-[#e1e2ec] font-bold"
                    />
                    <span className="text-[9px] text-[#9e8e7e] mt-1 block">المعيار: 215 - 225 µm</span>
                  </div>

                  <div className="bg-[#191b23] border border-[#32353d] p-3">
                    <span className="text-[10px] text-[#9e8e7e] block font-mono">قوة الانفجار (Burst kPa)</span>
                    <input
                      type="number"
                      value={qcBurst}
                      onChange={(e) => setQcBurst(Number(e.target.value))}
                      className="w-full bg-[#10131a] border border-[#32353d] px-2 py-1 mt-1 text-sm font-mono text-[#4edea3] font-bold"
                    />
                    <span className="text-[9px] text-[#4edea3] mt-1 block">ممتاز &gt; 380 kPa</span>
                  </div>

                  <div className="bg-[#191b23] border border-[#32353d] p-3">
                    <span className="text-[10px] text-[#9e8e7e] block font-mono">اختبار كوب (Cobb 60)</span>
                    <input
                      type="number"
                      value={qcCobb}
                      onChange={(e) => setQcCobb(Number(e.target.value))}
                      className="w-full bg-[#10131a] border border-[#32353d] px-2 py-1 mt-1 text-sm font-mono text-[#e1e2ec] font-bold"
                    />
                    <span className="text-[9px] text-[#9e8e7e] mt-1 block">المعيار: 30 - 40 g/m²</span>
                  </div>
                </div>

                {/* Disposition Decision Selection */}
                <div className="mb-6">
                  <label className="text-xs text-[#9e8e7e] block mb-2 font-bold">قرار اعتماد الرول (Quality Disposition Stamp):</label>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                    <button
                      type="button"
                      onClick={() => setQcDisposition('RELEASED')}
                      className={`p-3 border text-xs font-bold flex items-center justify-center gap-2 transition-colors ${
                        qcDisposition === 'RELEASED'
                          ? 'bg-[#00ac77] text-[#002113] border-[#4edea3]'
                          : 'bg-[#191b23] text-[#4edea3] border-[#32353d]'
                      }`}
                    >
                      <span className="material-symbols-outlined text-[18px]">verified</span>
                      <span>RELEASED (معتمد صالح للشحن)</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setQcDisposition('QUARANTINE')}
                      className={`p-3 border text-xs font-bold flex items-center justify-center gap-2 transition-colors ${
                        qcDisposition === 'QUARANTINE'
                          ? 'bg-[#c88a35] text-[#452900] border-[#feb95f]'
                          : 'bg-[#191b23] text-[#feb95f] border-[#32353d]'
                      }`}
                    >
                      <span className="material-symbols-outlined text-[18px]">gavel</span>
                      <span>QUARANTINE (حجز وإعادة فحص)</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setQcDisposition('REJECTED')}
                      className={`p-3 border text-xs font-bold flex items-center justify-center gap-2 transition-colors ${
                        qcDisposition === 'REJECTED'
                          ? 'bg-[#93000a] text-white border-[#ffb4ab]'
                          : 'bg-[#191b23] text-[#ffb4ab] border-[#32353d]'
                      }`}
                    >
                      <span className="material-symbols-outlined text-[18px]">cancel</span>
                      <span>REJECTED (مرفوض كلي تالف)</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setQcDisposition('DOWNGRADED')}
                      className={`p-3 border text-xs font-bold flex items-center justify-center gap-2 transition-colors ${
                        qcDisposition === 'DOWNGRADED'
                          ? 'bg-[#41495c] text-white border-[#bec6de]'
                          : 'bg-[#191b23] text-[#bec6de] border-[#32353d]'
                      }`}
                    >
                      <span className="material-symbols-outlined text-[18px]">low_priority</span>
                      <span>DOWNGRADED (تخفيض رتبة)</span>
                    </button>
                  </div>
                </div>

                <div className="mb-4">
                  <label className="text-xs text-[#9e8e7e] block mb-1">ملاحظات وتقرير المختبر:</label>
                  <textarea
                    rows={2}
                    value={qcNotes}
                    onChange={(e) => setQcNotes(e.target.value)}
                    className="w-full bg-[#191b23] border border-[#32353d] p-2 text-xs text-[#e1e2ec] outline-none"
                  />
                </div>

                <button
                  onClick={handleSaveQCTest}
                  className="px-6 py-2.5 bg-[#c88a35] hover:bg-[#feb95f] text-[#452900] text-xs font-bold flex items-center gap-2 transition-colors"
                >
                  <span className="material-symbols-outlined text-[18px]">save</span>
                  <span>حفظ وتوثيق الفحص في قاعدة البيانات</span>
                </button>
              </div>

              {/* Past QC Records Table */}
              <div className="bg-[#0b0e15] border border-[#32353d]/40 p-5">
                <h3 className="text-sm font-bold text-[#e1e2ec] mb-3">سجل اختبارات الجودة الموثقة</h3>
                <div className="overflow-x-auto">
                  <table className="w-full text-right text-xs">
                    <thead>
                      <tr className="border-b border-[#32353d]/60 text-[#9e8e7e] font-mono text-[11px]">
                        <th className="py-2 px-3">التاريخ</th>
                        <th className="py-2 px-3">سيريال الرول</th>
                        <th className="py-2 px-3">الكراماج</th>
                        <th className="py-2 px-3">الرطوبة</th>
                        <th className="py-2 px-3">الانفجار</th>
                        <th className="py-2 px-3">القرار</th>
                        <th className="py-2 px-3">المفتش</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#32353d]/30 font-mono">
                      {qcTests.map((t) => (
                        <tr key={t.id} className="hover:bg-[#191b23]">
                          <td className="py-2.5 px-3 text-[#9e8e7e]">{t.inspectedAt}</td>
                          <td className="py-2.5 px-3 text-[#feb95f] font-bold">{t.rollSerial}</td>
                          <td className="py-2.5 px-3">{t.actualGsm} GSM</td>
                          <td className="py-2.5 px-3">{t.moisturePct}%</td>
                          <td className="py-2.5 px-3">{t.burstKpa} kPa</td>
                          <td className="py-2.5 px-3">
                            <span className={`px-2 py-0.5 text-[10px] border ${t.disposition === 'RELEASED' ? 'bg-[#4edea3]/10 text-[#4edea3] border-[#4edea3]/30' : 'bg-[#feb95f]/10 text-[#feb95f] border-[#feb95f]/30'}`}>
                              {t.disposition}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 font-sans text-[#e1e2ec]">{t.inspector}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 5: THERMAL LABEL PRINTER */}
          {activeTab === 'thermal-label-printer' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between pb-4 border-b border-[#32353d]/40">
                <div>
                  <h1 className="text-xl font-bold text-[#e1e2ec]">محطة طباعة الباركود الحراري (Zebra Thermal Label 100x150 mm)</h1>
                  <span className="text-xs text-[#9e8e7e]">ملصق حراري موحد للصق على ماسورة وقلب رول الورق مع باركود EAN-128</span>
                </div>

                <button
                  onClick={() => window.print()}
                  className="px-5 py-2 bg-[#feb95f] hover:bg-[#c88a35] text-[#452900] text-xs font-bold flex items-center gap-2 shadow-lg transition-colors"
                >
                  <span className="material-symbols-outlined text-[18px]">print</span>
                  <span>طباعة ملصق الرول الآن</span>
                </button>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                
                {/* Print Control Settings Panel */}
                <div className="bg-[#0b0e15] border border-[#32353d]/40 p-5 space-y-4">
                  <h3 className="text-xs font-bold text-[#feb95f] uppercase font-mono tracking-wider">إعدادات الطابعة والملصق</h3>

                  <div>
                    <label className="text-xs text-[#9e8e7e] block mb-1">اختر الرول المراد طباعته:</label>
                    <select
                      value={selectedLabelRollSerial}
                      onChange={(e) => setSelectedLabelRollSerial(e.target.value)}
                      className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs font-mono text-[#feb95f] outline-none"
                    >
                      {childRolls.map((r) => (
                        <option key={r.id} value={r.serialNumber}>
                          {r.serialNumber} ({r.widthMm} مم - {r.netWeightKg} كجم)
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="text-xs text-[#9e8e7e] block mb-1">الطابعة الحرارية المحددة:</label>
                    <select
                      value={labelPrinter}
                      onChange={(e) => setLabelPrinter(e.target.value)}
                      className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs text-[#e1e2ec] outline-none font-sans"
                    >
                      <option value="Zebra ZT411 (صالة الإنتاج PM1)">Zebra ZT411 (صالة الإنتاج PM1)</option>
                      <option value="TSC TTP-244 Pro (محطة التقطيع SLT-04)">TSC TTP-244 Pro (محطة التقطيع SLT-04)</option>
                      <option value="Honeywell PM43 (مستودع A)">Honeywell PM43 (مستودع A)</option>
                    </select>
                  </div>

                  <div>
                    <label className="text-xs text-[#9e8e7e] block mb-1">عدد النسخ:</label>
                    <input
                      type="number"
                      min={1}
                      max={10}
                      value={labelCopies}
                      onChange={(e) => setLabelCopies(Number(e.target.value))}
                      className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs font-mono text-[#e1e2ec] outline-none"
                    />
                  </div>

                  <div className="p-3 bg-[#191b23] border border-[#32353d] text-[11px] text-[#d6c3b2] space-y-1">
                    <div className="font-bold text-[#feb95f]">معلومات الملصق القياسي:</div>
                    <div>• المقاس: 100 مم × 150 مم (4 × 6 بوصة)</div>
                    <div>• التشفير: Code 128 + 2D QR Code</div>
                    <div>• الورق: ملصق حراري عالي الالتصاق للكرتون</div>
                  </div>
                </div>

                {/* REALISTIC 100x150 mm LABEL CANVAS */}
                <div className="lg:col-span-2 flex justify-center bg-[#0b0e15] border border-[#32353d]/40 p-6">
                  <div 
                    id="thermal-label-canvas"
                    className="w-[100mm] min-h-[148mm] bg-white text-black p-4 border-2 border-black flex flex-col justify-between shadow-2xl font-sans"
                    style={{ direction: 'rtl' }}
                  >
                    {/* Header */}
                    <div className="border-b-2 border-black pb-2 text-center">
                      <div className="text-[14px] font-bold tracking-tight">مصنع مصر لصناعة الورق والكرتون</div>
                      <div className="text-[10px] font-mono font-semibold tracking-wider">PAPERERP ENTERPRISE • INDUSTRIAL ROLL LABEL</div>
                    </div>

                    {/* Barcode & QR Block */}
                    <div className="my-2 flex items-center justify-between gap-3 border-b-2 border-black pb-3">
                      {/* Realistic SVG Barcode 128 */}
                      <div className="flex-1 flex flex-col items-center">
                        <svg className="w-full h-14" viewBox="0 0 240 60">
                          {/* Generated barcode lines */}
                          <rect x="10" y="0" width="3" height="50" fill="black" />
                          <rect x="16" y="0" width="2" height="50" fill="black" />
                          <rect x="22" y="0" width="5" height="50" fill="black" />
                          <rect x="30" y="0" width="2" height="50" fill="black" />
                          <rect x="36" y="0" width="4" height="50" fill="black" />
                          <rect x="44" y="0" width="2" height="50" fill="black" />
                          <rect x="50" y="0" width="6" height="50" fill="black" />
                          <rect x="60" y="0" width="3" height="50" fill="black" />
                          <rect x="68" y="0" width="2" height="50" fill="black" />
                          <rect x="74" y="0" width="5" height="50" fill="black" />
                          <rect x="83" y="0" width="3" height="50" fill="black" />
                          <rect x="90" y="0" width="4" height="50" fill="black" />
                          <rect x="98" y="0" width="2" height="50" fill="black" />
                          <rect x="104" y="0" width="6" height="50" fill="black" />
                          <rect x="114" y="0" width="3" height="50" fill="black" />
                          <rect x="122" y="0" width="5" height="50" fill="black" />
                          <rect x="131" y="0" width="2" height="50" fill="black" />
                          <rect x="137" y="0" width="4" height="50" fill="black" />
                          <rect x="145" y="0" width="6" height="50" fill="black" />
                          <rect x="155" y="0" width="3" height="50" fill="black" />
                          <rect x="162" y="0" width="5" height="50" fill="black" />
                          <rect x="171" y="0" width="2" height="50" fill="black" />
                          <rect x="177" y="0" width="6" height="50" fill="black" />
                          <rect x="187" y="0" width="3" height="50" fill="black" />
                          <rect x="194" y="0" width="4" height="50" fill="black" />
                          <rect x="202" y="0" width="2" height="50" fill="black" />
                          <rect x="208" y="0" width="5" height="50" fill="black" />
                          <rect x="217" y="0" width="3" height="50" fill="black" />
                          <rect x="224" y="0" width="4" height="50" fill="black" />
                        </svg>
                        <span className="font-mono text-[12px] font-bold tracking-widest">{activeLabelRoll.serialNumber}</span>
                      </div>

                      {/* 2D QR Code SVG */}
                      <div className="w-16 h-16 border border-black p-1 flex items-center justify-center shrink-0">
                        <svg className="w-full h-full" viewBox="0 0 100 100">
                          <rect x="0" y="0" width="30" height="30" fill="black" />
                          <rect x="5" y="5" width="20" height="20" fill="white" />
                          <rect x="10" y="10" width="10" height="10" fill="black" />

                          <rect x="70" y="0" width="30" height="30" fill="black" />
                          <rect x="75" y="5" width="20" height="20" fill="white" />
                          <rect x="80" y="10" width="10" height="10" fill="black" />

                          <rect x="0" y="70" width="30" height="30" fill="black" />
                          <rect x="5" y="75" width="20" height="20" fill="white" />
                          <rect x="10" y="80" width="10" height="10" fill="black" />

                          <rect x="40" y="10" width="20" height="10" fill="black" />
                          <rect x="40" y="30" width="10" height="20" fill="black" />
                          <rect x="60" y="40" width="20" height="10" fill="black" />
                          <rect x="40" y="70" width="30" height="10" fill="black" />
                          <rect x="80" y="60" width="10" height="30" fill="black" />
                        </svg>
                      </div>
                    </div>

                    {/* Specifications Grid */}
                    <div className="grid grid-cols-2 gap-2 text-xs border-b-2 border-black pb-3">
                      <div>
                        <span className="text-[10px] block font-semibold text-gray-700">نوع الورق:</span>
                        <span className="font-bold text-sm">{activeLabelRoll.grade}</span>
                      </div>

                      <div>
                        <span className="text-[10px] block font-semibold text-gray-700">الرول الأم (Parent):</span>
                        <span className="font-mono font-bold">{activeLabelRoll.parentJumboSerial}</span>
                      </div>

                      <div className="border-t border-gray-400 pt-1">
                        <span className="text-[10px] block font-semibold text-gray-700">عرض الرول (Width):</span>
                        <span className="font-mono font-bold text-base">{activeLabelRoll.widthMm} mm</span>
                      </div>

                      <div className="border-t border-gray-400 pt-1">
                        <span className="text-[10px] block font-semibold text-gray-700">الكراماج (GSM):</span>
                        <span className="font-mono font-bold text-base">{activeLabelRoll.gsm} g/m²</span>
                      </div>

                      <div className="border-t border-gray-400 pt-1">
                        <span className="text-[10px] block font-semibold text-gray-700">الوزن الصافي (Net Wt):</span>
                        <span className="font-mono font-bold text-lg">{activeLabelRoll.netWeightKg} KG</span>
                      </div>

                      <div className="border-t border-gray-400 pt-1">
                        <span className="text-[10px] block font-semibold text-gray-700">الوزن الإجمالي (Gross):</span>
                        <span className="font-mono font-bold text-base">{activeLabelRoll.grossWeightKg} KG</span>
                      </div>

                      <div className="border-t border-gray-400 pt-1">
                        <span className="text-[10px] block font-semibold text-gray-700">قطر الماسورة:</span>
                        <span className="font-mono font-bold">76 mm (3")</span>
                      </div>

                      <div className="border-t border-gray-400 pt-1">
                        <span className="text-[10px] block font-semibold text-gray-700">القطر الخارجي:</span>
                        <span className="font-mono font-bold">{activeLabelRoll.diameterMm} mm</span>
                      </div>
                    </div>

                    {/* Footer Stamps */}
                    <div className="pt-2 flex items-center justify-between text-[10px]">
                      <div>
                        <div>المشغل: {activeLabelRoll.operatorName}</div>
                        <div className="font-mono">تاريخ: {activeLabelRoll.createdAt}</div>
                      </div>

                      <div className="border-2 border-black px-3 py-1 font-mono font-bold text-center">
                        <div>QC DISPOSITION</div>
                        <div className="text-xs">{activeLabelRoll.status}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 6: IMMUTABLE STOCK LEDGER */}
          {activeTab === 'immutable-stock-ledger' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-[#32353d]/40">
                <div>
                  <h1 className="text-xl font-bold text-[#e1e2ec]">سجل حركات المخزن المزدوج (Immutable Movement Ledger)</h1>
                  <span className="text-xs text-[#9e8e7e]">سجل غير قابل للتعديل لكافة حركات الورق مع بصمات تدقيق مشفرة SHA-256</span>
                </div>

                <button
                  onClick={() => setShowMovementModal(true)}
                  className="px-4 py-2 bg-[#c88a35] hover:bg-[#feb95f] text-[#452900] text-xs font-bold flex items-center gap-1.5 transition-colors self-start"
                >
                  <span className="material-symbols-outlined text-[16px]">add_box</span>
                  <span>تسجيل حركة مخزنية جديدة</span>
                </button>
              </div>

              {/* Filters Bar */}
              <div className="flex flex-wrap items-center gap-2 bg-[#0b0e15] p-3 border border-[#32353d]/40">
                <span className="text-xs text-[#9e8e7e] ml-2">نوع الحركة:</span>
                {['ALL', 'PRODUCTION_RECEIPT', 'STATUS_CHANGE', 'LOCATION_TRANSFER', 'DISPATCH', 'SCRAP'].map((type) => (
                  <button
                    key={type}
                    onClick={() => setMovementFilter(type)}
                    className={`px-3 py-1 text-xs font-mono transition-colors border ${
                      movementFilter === type
                        ? 'bg-[#feb95f] text-[#452900] border-[#feb95f] font-bold'
                        : 'bg-[#191b23] text-[#d6c3b2] border-[#32353d] hover:bg-[#272a32]'
                    }`}
                  >
                    {type}
                  </button>
                ))}
              </div>

              {/* Ledger Table */}
              <div className="bg-[#0b0e15] border border-[#32353d]/40 p-5">
                <div className="overflow-x-auto">
                  <table className="w-full text-right text-xs">
                    <thead>
                      <tr className="border-b border-[#32353d]/60 text-[#9e8e7e] font-mono text-[11px]">
                        <th className="py-2.5 px-3">التاريخ والوقت</th>
                        <th className="py-2.5 px-3">سيريال الرول</th>
                        <th className="py-2.5 px-3">النوع</th>
                        <th className="py-2.5 px-3">نوع الحركة</th>
                        <th className="py-2.5 px-3">الوزن الصافي</th>
                        <th className="py-2.5 px-3">من موقع</th>
                        <th className="py-2.5 px-3">إلى موقع</th>
                        <th className="py-2.5 px-3">المسؤول</th>
                        <th className="py-2.5 px-3">بصمة التدقيق</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#32353d]/30 font-mono">
                      {filteredMovements.map((m) => (
                        <tr key={m.id} className="hover:bg-[#191b23]">
                          <td className="py-3 px-3 text-[#9e8e7e]">{m.timestamp}</td>
                          <td className="py-3 px-3 text-[#feb95f] font-bold">{m.rollSerial}</td>
                          <td className="py-3 px-3 font-sans text-[#d6c3b2]">{m.rollCategory}</td>
                          <td className="py-3 px-3">
                            <span className="px-2 py-0.5 bg-[#4edea3]/10 text-[#4edea3] border border-[#4edea3]/30 text-[10px]">
                              {m.movementType}
                            </span>
                          </td>
                          <td className="py-3 px-3 text-[#e1e2ec] font-bold">{m.netWeightKg} كجم</td>
                          <td className="py-3 px-3 font-sans text-[#d6c3b2]">{m.fromLocation}</td>
                          <td className="py-3 px-3 font-sans text-[#4edea3]">{m.toLocation}</td>
                          <td className="py-3 px-3 font-sans text-[#e1e2ec]">{m.actor}</td>
                          <td className="py-3 px-3 text-[#feb95f] font-bold">{m.auditHash}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 7: RUGGED TABLET CONSOLE */}
          {activeTab === 'rugged-tablet' && (
            <div className="space-y-6">
              <div className="bg-[#191b23] border border-[#feb95f] p-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[#feb95f] text-[32px]">tablet_android</span>
                  <div>
                    <h1 className="text-lg font-bold text-[#feb95f]">شاشة التابلت الميداني للصالة (Rugged Tablet Floor Operator)</h1>
                    <span className="text-xs text-[#d6c3b2]">واجهة مكبرة ومخصصة للمشغلين بالأيدي والقفازات الصناعية</span>
                  </div>
                </div>

                <div className="px-4 py-2 bg-[#4edea3]/10 border border-[#4edea3] text-[#4edea3] font-mono text-sm font-bold">
                  TOUCH ACTIVE
                </div>
              </div>

              {/* Big Touch Controls Grid */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <button
                  onClick={() => setActiveTab('deckle-calculator')}
                  className="bg-[#0b0e15] border-2 border-[#32353d] hover:border-[#feb95f] p-6 flex flex-col items-center justify-center gap-3 transition-colors text-center group"
                >
                  <div className="w-16 h-16 bg-[#c88a35] group-hover:bg-[#feb95f] flex items-center justify-center text-[#452900] transition-colors">
                    <span className="material-symbols-outlined text-[36px]">straighten</span>
                  </div>
                  <span className="text-lg font-bold text-[#e1e2ec]">تقطيع رول جامبو جديد</span>
                  <span className="text-xs text-[#9e8e7e]">ضبط سكاكين التقطيع وميزان الكتلة</span>
                </button>

                <button
                  onClick={() => setActiveTab('qc-laboratory-gate')}
                  className="bg-[#0b0e15] border-2 border-[#32353d] hover:border-[#4edea3] p-6 flex flex-col items-center justify-center gap-3 transition-colors text-center group"
                >
                  <div className="w-16 h-16 bg-[#00ac77] group-hover:bg-[#4edea3] flex items-center justify-center text-[#002113] transition-colors">
                    <span className="material-symbols-outlined text-[36px]">biotech</span>
                  </div>
                  <span className="text-lg font-bold text-[#e1e2ec]">فحص الجودة واعتماد الرول</span>
                  <span className="text-xs text-[#9e8e7e]">إدخال الكراماج والرطوبة وقوة الشد</span>
                </button>

                <button
                  onClick={() => setActiveTab('thermal-label-printer')}
                  className="bg-[#0b0e15] border-2 border-[#32353d] hover:border-[#feb95f] p-6 flex flex-col items-center justify-center gap-3 transition-colors text-center group"
                >
                  <div className="w-16 h-16 bg-[#c88a35] group-hover:bg-[#feb95f] flex items-center justify-center text-[#452900] transition-colors">
                    <span className="material-symbols-outlined text-[36px]">print</span>
                  </div>
                  <span className="text-lg font-bold text-[#e1e2ec]">طباعة باركود الرول الحراري</span>
                  <span className="text-xs text-[#9e8e7e]">طابعة زيبرا ZT411 قياس 100x150 مم</span>
                </button>
              </div>

              {/* Big Scale Metrology Display */}
              <div className="bg-[#0b0e15] border border-[#32353d]/40 p-6 flex flex-col items-center">
                <span className="text-xs font-mono text-[#9e8e7e] mb-2 uppercase">ميزان البسكول الآلي لصالة الإنتاج (Floor Scale G-01)</span>
                <div className="text-6xl font-mono font-bold text-[#4edea3] tracking-wider mb-2">
                  1,245.00 <span className="text-2xl text-[#94a3b8]">KG</span>
                </div>
                <span className="text-xs font-mono text-[#feb95f]">STABILIZED &bull; ZERO DRIFT TOLERANCE &bull; RFID LOCKED</span>
              </div>
            </div>
          )}
        </main>
      </div>

      {/* MODAL 1: REGISTER & AUTHENTICATION */}
      {showAuthModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#0b0e15] border border-[#feb95f] w-full max-w-md p-6 shadow-2xl relative font-sans text-right">
            <button
              onClick={() => setShowAuthModal(false)}
              className="absolute top-4 left-4 text-[#9e8e7e] hover:text-white"
            >
              <span className="material-symbols-outlined text-[20px]">close</span>
            </button>

            <div className="flex items-center gap-2 mb-4">
              <div className="w-8 h-8 bg-[#c88a35] flex items-center justify-center text-[#452900]">
                <span className="material-symbols-outlined text-[18px]">badge</span>
              </div>
              <h2 className="text-base font-bold text-[#feb95f]">تسجيل حساب مستخدم جديد في المصنع</h2>
            </div>

            {/* Quick Demo Switcher */}
            <div className="mb-5 p-3 bg-[#191b23] border border-[#32353d]">
              <span className="text-[11px] text-[#9e8e7e] block mb-2 font-bold">تجربة سريعة للعميل (تبديل الصلاحية بنقرة واحدة):</span>
              <div className="grid grid-cols-3 gap-2">
                {users.map((u) => (
                  <button
                    key={u.id}
                    onClick={() => {
                      setCurrentUser(u);
                      setShowAuthModal(false);
                      showToast(`تم التبديل إلى: ${u.fullName} (${u.role})`);
                    }}
                    className={`p-2 border text-[10px] font-bold text-center transition-colors ${
                      currentUser?.id === u.id ? 'bg-[#c88a35] text-[#452900] border-[#feb95f]' : 'bg-[#10131a] text-[#e1e2ec] border-[#32353d]'
                    }`}
                  >
                    <div>{u.fullName}</div>
                    <div className="text-[8px] opacity-75 font-mono">{u.role}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* Registration Form */}
            <form onSubmit={handleRegisterUser} className="space-y-4">
              <div>
                <label className="text-xs text-[#9e8e7e] block mb-1">الاسم الكامل للمشغل / الموظف:</label>
                <input
                  type="text"
                  required
                  placeholder="مثال: م. مصطفى محمود"
                  value={regFullName}
                  onChange={(e) => setRegFullName(e.target.value)}
                  className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs text-[#e1e2ec] outline-none focus:border-[#feb95f]"
                />
              </div>

              <div>
                <label className="text-xs text-[#9e8e7e] block mb-1">البريد الإلكتروني للعمل:</label>
                <input
                  type="email"
                  required
                  placeholder="operator@cellupaper.com"
                  value={regEmail}
                  onChange={(e) => setRegEmail(e.target.value)}
                  className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs text-[#e1e2ec] font-mono outline-none focus:border-[#feb95f]"
                />
              </div>

              <div>
                <label className="text-xs text-[#9e8e7e] block mb-1">الدور الوظيفي والصلاحيات (Role):</label>
                <select
                  value={regRole}
                  onChange={(e) => setRegRole(e.target.value as any)}
                  className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs text-[#feb95f] outline-none"
                >
                  <option value="CHIEF_OPERATOR">مشغل رئيسي (Chief Operator) - تحكم بالسكاكين والتقطيع</option>
                  <option value="QC_INSPECTOR">مفتش جودة (QC Inspector) - اعتماد الفحص المخبري والشحن</option>
                  <option value="MILL_MANAGER">مدير مصنع (Mill Manager) - لوحة تحكم وتقارير تنفيذية</option>
                  <option value="WAREHOUSE_KEEPER">أمين مستودع (Warehouse Keeper) - نقل وتخزين الرولات</option>
                </select>
              </div>

              <div>
                <label className="text-xs text-[#9e8e7e] block mb-1">المصنع أو الخط الإنتاجي:</label>
                <input
                  type="text"
                  value={regPlant}
                  onChange={(e) => setRegPlant(e.target.value)}
                  className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs text-[#e1e2ec] outline-none"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-[#c88a35] hover:bg-[#feb95f] text-[#452900] text-xs font-bold transition-colors flex items-center justify-center gap-1.5"
              >
                <span className="material-symbols-outlined text-[18px]">how_to_reg</span>
                <span>تسجيل الحساب وتفعيله فوراً</span>
              </button>
            </form>
          </div>
        </div>
      )}

      {/* MODAL 2: DATABASE & SUPABASE CLOUD CONNECTION */}
      {showDbModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#0b0e15] border border-[#feb95f] w-full max-w-lg p-6 shadow-2xl relative font-sans text-right">
            <button
              onClick={() => setShowDbModal(false)}
              className="absolute top-4 left-4 text-[#9e8e7e] hover:text-white"
            >
              <span className="material-symbols-outlined text-[20px]">close</span>
            </button>

            <div className="flex items-center gap-2 mb-3">
              <span className="material-symbols-outlined text-[#feb95f] text-[24px]">database</span>
              <h2 className="text-base font-bold text-[#feb95f]">إعدادات قاعدة البيانات والسحابة (Supabase Cloud PostgreSQL)</h2>
            </div>

            <p className="text-xs text-[#d6c3b2] mb-4">
              يمكنك ربط المشروع بقاعدة بياناتك المرفوعة على <a href="https://supabase.com" target="_blank" rel="noreferrer" className="text-[#feb95f] underline">Supabase</a> أو الاستمرار بوضع التخزين المحلي التلقائي. كل العمليات تحفظ وتعمل 100%!
            </p>

            {dbStatusText && (
              <div className="mb-4 p-3 bg-[#4edea3]/10 border border-[#4edea3] text-[#4edea3] text-xs font-bold">
                {dbStatusText}
              </div>
            )}

            <div className="space-y-4">
              <div>
                <label className="text-xs text-[#9e8e7e] block mb-1">Supabase Project URL:</label>
                <input
                  type="text"
                  placeholder="https://xyzcompany.supabase.co"
                  value={dbUrl}
                  onChange={(e) => setDbUrl(e.target.value)}
                  className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs font-mono text-[#e1e2ec] outline-none focus:border-[#feb95f]"
                />
              </div>

              <div>
                <label className="text-xs text-[#9e8e7e] block mb-1">Supabase Anon Public API Key:</label>
                <input
                  type="password"
                  placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                  value={dbKey}
                  onChange={(e) => setDbKey(e.target.value)}
                  className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs font-mono text-[#e1e2ec] outline-none focus:border-[#feb95f]"
                />
              </div>

              <div className="p-3 bg-[#191b23] border border-[#32353d] text-[11px] text-[#9e8e7e] space-y-1">
                <span className="font-bold text-[#feb95f] block">مخطط الجداول (SQL Schema):</span>
                <span>تم إنشاء ملف <code className="text-[#4edea3]">frontend/supabase_schema.sql</code> جاهز في المشروع. يمكنك نسخه ولصقه في Supabase SQL Editor لإنشاء الجداول بنقرة واحدة!</span>
              </div>

              <div className="flex gap-3">
                <button
                  onClick={handleSaveDbConfig}
                  className="flex-1 py-2.5 bg-[#c88a35] hover:bg-[#feb95f] text-[#452900] text-xs font-bold transition-colors flex items-center justify-center gap-1.5"
                >
                  <span className="material-symbols-outlined text-[18px]">check_circle</span>
                  <span>حفظ وتفعيل الاتصال</span>
                </button>

                <button
                  onClick={() => {
                    setDbUrl('');
                    setDbKey('');
                    db.setSupabaseConfig('', '');
                    setDbStatusText('تم تفعيل وضع التخزين المحلي المستمر (Local Storage)');
                  }}
                  className="px-4 py-2.5 bg-[#1d1f27] hover:bg-[#272a32] border border-[#32353d] text-xs font-bold transition-colors"
                >
                  التبديل للمحلي
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 3: MANUAL MOVEMENT RECORD */}
      {showMovementModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#0b0e15] border border-[#feb95f] w-full max-w-md p-6 shadow-2xl relative font-sans text-right">
            <button
              onClick={() => setShowMovementModal(false)}
              className="absolute top-4 left-4 text-[#9e8e7e] hover:text-white"
            >
              <span className="material-symbols-outlined text-[20px]">close</span>
            </button>

            <div className="flex items-center gap-2 mb-4">
              <span className="material-symbols-outlined text-[#feb95f] text-[20px]">add_box</span>
              <h2 className="text-base font-bold text-[#feb95f]">تسجيل حركة مخزنية جديدة في السجل المزدوج</h2>
            </div>

            <form onSubmit={handleAddManualMovement} className="space-y-4">
              <div>
                <label className="text-xs text-[#9e8e7e] block mb-1">سيريال الرول:</label>
                <input
                  type="text"
                  required
                  value={newMovSerial}
                  onChange={(e) => setNewMovSerial(e.target.value)}
                  className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs font-mono text-[#feb95f] outline-none"
                />
              </div>

              <div>
                <label className="text-xs text-[#9e8e7e] block mb-1">نوع الحركة:</label>
                <select
                  value={newMovType}
                  onChange={(e) => setNewMovType(e.target.value as any)}
                  className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs text-[#e1e2ec] outline-none font-mono"
                >
                  <option value="PRODUCTION_RECEIPT">PRODUCTION_RECEIPT (ورود إنتاج جديد)</option>
                  <option value="STATUS_CHANGE">STATUS_CHANGE (تغيير حالة الرول)</option>
                  <option value="LOCATION_TRANSFER">LOCATION_TRANSFER (نقل داخلي بين المستودعات)</option>
                  <option value="DISPATCH">DISPATCH (شحن وتسليم للعميل)</option>
                  <option value="SCRAP">SCRAP (تحويل لهالك أو إعادة تدوير)</option>
                </select>
              </div>

              <div>
                <label className="text-xs text-[#9e8e7e] block mb-1">الوزن الصافي (كجم):</label>
                <input
                  type="number"
                  required
                  value={newMovWeight}
                  onChange={(e) => setNewMovWeight(Number(e.target.value))}
                  className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs font-mono text-[#e1e2ec] outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-[#9e8e7e] block mb-1">من موقع:</label>
                  <input
                    type="text"
                    required
                    value={newMovFrom}
                    onChange={(e) => setNewMovFrom(e.target.value)}
                    className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs text-[#e1e2ec] outline-none"
                  />
                </div>
                <div>
                  <label className="text-xs text-[#9e8e7e] block mb-1">إلى موقع:</label>
                  <input
                    type="text"
                    required
                    value={newMovTo}
                    onChange={(e) => setNewMovTo(e.target.value)}
                    className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs text-[#e1e2ec] outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs text-[#9e8e7e] block mb-1">السبب / إذن الصرف أو النقل:</label>
                <input
                  type="text"
                  required
                  value={newMovReason}
                  onChange={(e) => setNewMovReason(e.target.value)}
                  className="w-full bg-[#191b23] border border-[#32353d] px-3 py-2 text-xs text-[#e1e2ec] outline-none"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-[#c88a35] hover:bg-[#feb95f] text-[#452900] text-xs font-bold transition-colors flex items-center justify-center gap-1.5"
              >
                <span className="material-symbols-outlined text-[18px]">history_edu</span>
                <span>توثيق الحركة في السجل غير القابل للتعديل</span>
              </button>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
