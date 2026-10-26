# التحصين السيبراني وأمن التطبيق — PaperERP Enterprise
# Cybersecurity Hardening & Application Security Architecture

**تاريخ الإصدار:** 2026-10-26  
**الإصدار:** 1.0.0-SNAPSHOT  
**المعايير المعتمدة:** OWASP Top 10 | OWASP ASVS | CIS Benchmarks | Zero Trust Architecture

---

## 1. نموذج الدفاع المتعدد (Defense-in-Depth Model)

تم تصميم النظام ليعمل بمبدأ انعدام الثقة (Zero Trust)، بحيث لا تعتمد أي طبقة على سابقتها كضمان كافٍ:
1. **طبقة سياق الطلب (TenantContext):** استخراج معرّف المستأجر والمصنع حصرياً من التوكن الموثق خادمياً، وتطهير الـ ThreadLocal إجبارياً بعد كل طلب.
2. **المفاتيح الفريدة المركبة (Composite Unique Keys):** `UNIQUE (tenant_id, id)` لمنع تشابه أو تداخل المعرفات بين الشركات.
3. **العلاقات الأجنبية المركبة (Composite Foreign Keys):** `FOREIGN KEY (tenant_id, parent_id) REFERENCES parent(tenant_id, id)` لمنع هجمات IDOR/BOLA حتى عند التخمين العشوائي للـ UUIDs.
4. **أمان مستوى الصفوف (PostgreSQL Row-Level Security):** حماية أمنية مستقلة في نواة قاعدة البيانات تمنع قراءة أو تعديل بيانات خارج المستأجر النشط.

---

## 2. إدارة الجلسات ومكافحة هجمات التخمين (Authentication & Brute-Force Protection)

### أ. حماية منافذ الدخول من هجمات القوة الغاشمة (Brute Force):
- تطبيق خدمة `RateLimitingService` لتتبع محاولات الدخول الفاشلة لكل زوج من (IP + اسم المستخدم).
- بعد **5 محاولات فاشلة**، يتم تفعيل إغلاق مؤقت وتدريجي (Progressive Lockout) لمدة **15 دقيقة**.
- إرجاع رد آمن بكود HTTP 429 Too Many Requests دون كشف ما إذا كان المستخدم موجوداً أصلاً.

### ب. نموذج التوكن المزدوج (Dual-Token Pattern):
- **Access Token:** JWT موقع بخوارزمية HMAC-SHA256، مدة صلاحيته 15 دقيقة فقط، ويحمل المعرفات وصلاحيات الـ RBAC.
- **Refresh Token:** عشوائي مشفر بالكامل في قاعدة البيانات، مخزن في كوكي متصفح آمن (`HttpOnly`, `SameSite=Lax`, `Secure` في الإنتاج).
- **كشف إعادة الاستخدام (Token Reuse Detection):** في حال استدعاء رمز تحديث سبق استخدامه، يفترض النظام تعرض الجلسة للسرقة ويقوم بإلغاء عائلة التوكن بالكامل (Session Revocation).

---

## 3. حماية الـ CSRF وأمان الرؤوس (CSRF & Security Headers)

### أ. حماية الـ CSRF:
- تفعيل حماية Double Submit Cookie عبر `CookieCsrfTokenRepository.withHttpOnlyFalse()` للمتصفحات.
- إلزام المتصفحات بإرسال رأس `X-XSRF-TOKEN` في كافة طلبات التعديل (`POST`, `PUT`, `DELETE`).

### ب. رؤوس الأمان المشددة (HTTP Security Headers):
تم تفعيل الحماية في `SecurityConfig`:
- **HSTS (Strict-Transport-Security):** فرض بروتوكول HTTPS لمدة عام كامل مع تضمين النطاقات الفرعية.
- **Content-Security-Policy (CSP):** `default-src 'self'; frame-ancestors 'none'; object-src 'none';` لمنع هجمات Clickjacking وتضمين النصوص الخبيثة.
- **X-Content-Type-Options:** `nosniff` لمنع متصفحات الويب من تخمين أنواع MIME.
- **X-Frame-Options:** `DENY` لحظر فتح لوحة تحكم المصنع داخل أي iframe خارجي.

---

## 4. فصل المسؤوليات والتحقق المزدوج (Separation of Duties / Maker-Checker)

لحماية العمليات الصناعية الحساسة من التحيز أو الاحتيال الداخلي:
- تم فرض قاعدة برمجية في `QualityService`: المشغل الذي قام بإنشاء الرول الجامبو أو المشرح **يُحظر عليه نظاماً** أن يكون هو نفسه مفتش الجودة الذي يصدر قرار الاعتماد أو الإفراج (`RELEASED`).
- أي محاولة للاعتماد الذاتي تُقابل بخطأ `MAKER_CHECKER_VIOLATION` وتُسجل في سجل التدقيق الأمني الفوري.
