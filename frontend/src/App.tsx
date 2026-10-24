import { useState, useEffect } from 'react';
import { Layers, Database, ShieldCheck, CheckCircle2, RefreshCw, Cpu } from 'lucide-react';

interface HealthData {
  status: string;
  system: string;
  version: string;
  timestamp: string;
  databaseConnected: boolean;
  activeProfiles: string[];
}

export default function App() {
  const [health, setHealth] = useState<HealthData | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchHealth = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch('/api/v1/health');
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
      }
      const data: HealthData = await res.json();
      setHealth(data);
    } catch (err: any) {
      setError(err.message || 'تعذر الاتصال بالخادم الخلفي');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: '32px 16px' }}>
      {/* Header */}
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 32, borderBottom: '1px solid #e2e8f0', paddingBottom: 20 }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ background: '#1e3a8a', color: 'white', borderRadius: 8, padding: 8, display: 'flex' }}>
              <Layers size={28} />
            </div>
            <div>
              <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#0f172a' }}>
                PaperERP Enterprise
              </h1>
              <p style={{ color: '#64748b', fontSize: '0.95rem' }}>
                نظام إدارة مصانع وتصنيع الورق — سبرنت 1 (Sprint 1 Foundation)
              </p>
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <span className="badge badge-info">
            المصنع: مصنع تجريبي (DEMO-PLANT-01)
          </span>
          <span className="badge badge-neutral">
            الإصدار: 1.0.0-SNAPSHOT
          </span>
        </div>
      </header>

      {/* Grid of Status Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 20, marginBottom: 32 }}>
        {/* Backend & DB Status Card */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <Database color="#1e3a8a" size={24} />
              <h2 style={{ fontSize: '1.15rem', fontWeight: 700 }}>حالة الخادم وقاعدة البيانات</h2>
            </div>
            <button 
              onClick={fetchHealth} 
              disabled={loading}
              className="button-primary"
              style={{ fontSize: '0.85rem', padding: '6px 12px' }}
              title="تحديث فحص الاتصال"
            >
              <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
              فحص الاتصال
            </button>
          </div>

          {loading && <p style={{ color: '#64748b' }}>جارٍ الاتصال بالواجهة البرمجية...</p>}

          {error && (
            <div style={{ background: '#fee2e2', color: '#991b1b', padding: 12, borderRadius: 6, fontSize: '0.9rem' }}>
              ⚠️ تنبيه اتصال: {error}
              <div style={{ marginTop: 4, fontSize: '0.8rem', color: '#7f1d1d' }}>
                (تأكد من تشغيل Spring Boot على منفذ 8080: <code>./mvnw spring-boot:run</code>)
              </div>
            </div>
          )}

          {health && (
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
                <CheckCircle2 color="#15803d" size={20} />
                <span style={{ fontWeight: 600, color: '#15803d' }}>الخادم الخلفي يعمل بنجاح (UP)</span>
              </div>
              <ul style={{ listStyle: 'none', fontSize: '0.9rem', color: '#334155', display: 'flex', flexDirection: 'column', gap: 6 }}>
                <li><strong>النظام:</strong> {health.system}</li>
                <li><strong>اتصال قاعدة البيانات:</strong> {health.databaseConnected ? '✅ متصل وفعال' : '❌ غير متصل'}</li>
                <li><strong>الملفات النشطة (Profiles):</strong> <code>{health.activeProfiles.join(', ') || 'default'}</code></li>
                <li><strong>طابع الوقت:</strong> <code style={{ fontSize: '0.8rem' }}>{health.timestamp}</code></li>
              </ul>
            </div>
          )}
        </div>

        {/* Sprint 1 Architecture Card */}
        <div className="card">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
            <Cpu color="#1e3a8a" size={24} />
            <h2 style={{ fontSize: '1.15rem', fontWeight: 700 }}>جاهزية معمارية النظام الموحدة</h2>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 12, fontSize: '0.9rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #f1f5f9', paddingBottom: 8 }}>
              <span>قاعدة البيانات والتنقلات:</span>
              <span className="badge badge-success">Flyway V1 Schema ✅</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #f1f5f9', paddingBottom: 8 }}>
              <span>أمان وتعدد المصانع:</span>
              <span className="badge badge-success">Multi-Tenancy Scoped ✅</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #f1f5f9', paddingBottom: 8 }}>
              <span>واجهة المستخدم والتوطين:</span>
              <span className="badge badge-success">Arabic RTL First ✅</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>خط أنابيب التحقق الآلي:</span>
              <span className="badge badge-success">GitHub Actions CI ✅</span>
            </div>
          </div>
        </div>
      </div>

      {/* Developer Responsibilities Summary (Section H) */}
      <section className="card">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
          <ShieldCheck color="#1e3a8a" size={24} />
          <h2 style={{ fontSize: '1.15rem', fontWeight: 700 }}>حدود المسؤوليات لفريق التطوير (3-Person Ownership)</h2>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16 }}>
          <div style={{ background: '#f8fafc', padding: 16, borderRadius: 8, border: '1px solid #e2e8f0' }}>
            <h3 style={{ fontWeight: 700, color: '#1e3a8a', marginBottom: 8 }}>المطور (A) — الأساس والأمان</h3>
            <p style={{ fontSize: '0.85rem', color: '#475569' }}>
              هيكل Spring Boot، أمان الـ JWT، مرشح تعدد المستأجرين (TenantContextFilter)، وخط الـ CI/CD.
            </p>
          </div>

          <div style={{ background: '#f8fafc', padding: 16, borderRadius: 8, border: '1px solid #e2e8f0' }}>
            <h3 style={{ fontWeight: 700, color: '#1e3a8a', marginBottom: 8 }}>المطور (B) — المجال وقاعدة البيانات</h3>
            <p style={{ fontSize: '0.85rem', color: '#475569' }}>
              مخطط قاعدة البيانات و Flyway، سجل حركات المخزون غير القابل للتعديل، ومحرك تقطيع رولات الورق.
            </p>
          </div>

          <div style={{ background: '#f8fafc', padding: 16, borderRadius: 8, border: '1px solid #e2e8f0' }}>
            <h3 style={{ fontWeight: 700, color: '#1e3a8a', marginBottom: 8 }}>المطور (C) — الواجهة والتجربة RTL</h3>
            <p style={{ fontSize: '0.85rem', color: '#475569' }}>
              تطبيق React 19، الاتجاه العربي الكامل (RTL)، حاسبة التقطيع التفاعلية، وتجربة شاشات المصنع.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
