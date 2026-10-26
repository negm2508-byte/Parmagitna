# تقرير التدقيق المستقل للجاهزية الإنتاجية — PaperERP Enterprise
# Independent Production Readiness & Security Audit Report

**تاريخ التدقيق:** 2026-10-26  
**الإصدار الخاضع للتدقيق:** Commit `7bced46fca6cc2d2a2ad72859389c4ee6b55d49f`  
**المدقق:** Principal Backend Architect, Application Security Engineer, PostgreSQL Specialist & QA Lead  
**بيئة التدقيق:** Windows 10/11 | Java 17 LTS (Eclipse Adoptium 17.0.20.1) | Maven 3.9.6 | Spring Boot 3.4.3 | PostgreSQL 16.15  

---

## 1. ملخص الموقف والقرار التنفيذي (Executive Verdict)

بناءً على الفحص المصدري المباشر، وتشغيل حزم الاختبارات الفعلية، وتشغيل الحزمة التنفيذية (`paper-erp-api-1.0.0-SNAPSHOT.jar`) ومراقبة سلوكها الحي عبر الشبكة المحلية:

| البيئة المستهدفة | حالة الجاهزية | الخلاصة والقرار |
| :--- | :---: | :--- |
| **بيئة التطوير المحلي (Local Development)** | **PASS** | الكود متماسك، يترجم بنجاح، كافة الاختبارات (22/22) ناجحة، والـ JAR يقلع بسلاسة. |
| **بيئة الاختبار والاعتماد (Staging Environment)** | **PARTIAL** | توجد نواقص معمارية تتطلب تصحيحاً قبل النشر (ربط جلسة RLS، تفعيل الـ Idempotency، وتصحيح فهرس الـ Keyset). |
| **بيئة الإنتاج الحي (Live Production)** | **NOT READY** | **غير جاهز للإنتاج فوراً** حتى يتم حل الثغرات المعمارية الموثقة (خصوصاً ثغرة انفصال الـ RLS عن جلسات Hibernate، وغياب القفل المتوزع في الـ Outbox). |

---

## 2. جدول تصنيف ضوابط التدقيق (Audit Controls Matrix)

| الضابط (Control Name) | الحالة | الملفات ومواضع الكود | الأمر المنفذ والدليل | الأثر الأمني / التشغيلي |
| :--- | :---: | :--- | :--- | :--- |
| **بناء الحزم واستقلالية الوحدات (Build & Modular Monolith)** | **PASS** | [backend/pom.xml](file:///g:/ERP_BackEnd/backend/pom.xml)<br>[ArchUnitTest.java](file:///g:/ERP_BackEnd/backend/paper-erp-api/src/test/java/com/papererp/api/arch/ArchUnitTest.java) | `mvnw.cmd clean package`<br>Exit Code: 0، إنتاج Fat JAR | حوكمة معمارية تمنع التبعيات العشوائية والدائرية بنجاح. |
| **تشغيل الحزمة الإنتاجية الحية (Live Runtime Bootstrap)** | **PASS** | [PaperErpApplication.java](file:///g:/ERP_BackEnd/backend/paper-erp-api/src/main/java/com/papererp/PaperErpApplication.java) | `java -jar paper-erp-api-1.0.0-SNAPSHOT.jar --server.port=8089` | إقلاع ناجح لـ Tomcat في 19.8 ثانية واستجابة كاملة لـ `/api/v1/health` و `/v3/api-docs`. |
| **رؤوس الأمان وحماية المتصفح (Security Headers & CSRF)** | **PASS** | [SecurityConfig.java](file:///g:/ERP_BackEnd/backend/paper-erp-api/src/main/java/com/papererp/api/config/SecurityConfig.java#L123-L128) | تم التحقق حياً عبر `Invoke-WebRequest` | توفير `HSTS`, `CSP`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, وكوكيز `XSRF-TOKEN`. |
| **سياق المستأجر والعزل البرمجي (Tenant Context Boundary)** | **PASS** | [TenantContext.java](file:///g:/ERP_BackEnd/backend/paper-erp-common/src/main/java/com/papererp/common/context/TenantContext.java)<br>[JwtAuthenticationFilter.java](file:///g:/ERP_BackEnd/backend/paper-erp-api/src/main/java/com/papererp/api/filter/JwtAuthenticationFilter.java#L40-L43) | [TenantContextTest.java](file:///g:/ERP_BackEnd/backend/paper-erp-common/src/test/java/com/papererp/common/context/TenantContextTest.java) | استخراج المعرف من التوكن فقط، وتنظيف إجباري للـ ThreadLocal في كتلة `finally`. |
| **معادلة التوازن الكتلي للتقطيع (Slitting Mass Balance)** | **PASS** | [SlittingOperationService.java](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/main/java/com/papererp/core/service/SlittingOperationService.java#L110-L145) | [SlittingOperationServiceTest.java](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/test/java/com/papererp/core/service/SlittingOperationServiceTest.java) | حساب دقيق بالـ `BigDecimal` للفارق ($-10.00\text{ kg}$) وتوثيقه كـ `BALANCED_WITHIN_TOLERANCE`. |
| **فصل المسؤوليات والتحقق المزدوج (Separation of Duties)** | **PASS** | [QualityService.java](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/main/java/com/papererp/core/service/QualityService.java#L50-L65) | [QualityServiceTest.java](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/test/java/com/papererp/core/service/QualityServiceTest.java) | حظر فحص الرول أو اعتماده من قبل نفس المشغل الذي أنتجه (`MAKER_CHECKER_VIOLATION`). |
| **عزل PostgreSQL RLS عبر الجلسات (PostgreSQL RLS Hook)** | **FAIL** | [V1__baseline_paper_erp_schema.sql](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/main/resources/db/migration/V1__baseline_paper_erp_schema.sql#L348)<br>[V2__high_concurrency_outbox_idempotency.sql](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/main/resources/db/migration/V2__high_concurrency_outbox_idempotency.sql#L50) | فحص الكود المصدري بالـ ripgrep بحثاً عن `app.current_tenant_id` | **حرج (Critical):** الـ RLS مبني في SQL لكن **لا يوجد كود Java يضبط `SET LOCAL app.current_tenant_id`**! تحت دور تشغيلي غير المالك، تعيد كل الاستعلامات صفوفاً فارغة. |
| **محرك الـ Idempotency العام (Idempotency Filter & Hashing)** | **FAIL** | [IdempotencyService.java](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/main/java/com/papererp/core/service/IdempotencyService.java) | فحص استخدام الخدمة في الحزم ومراجعة فحص التجزئة | **متوسط (Medium):** الخدمة غير موصولة بأي Controller أو Filter، كما أنها لا تتحقق من تطابق حمولة الطلب عند تكرار نفس المفتاح بحمولة مختلفة. |
| **فهرس التصفح السريع للدفتر (Keyset Pagination Index)** | **FAIL** | [InventoryLedgerRepository.java](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/main/java/com/papererp/core/repository/InventoryLedgerRepository.java#L33)<br>[V2__high_concurrency_outbox_idempotency.sql](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/main/resources/db/migration/V2__high_concurrency_outbox_idempotency.sql#L37) | مقارنة أعمدة الاستعلام بأعمدة الفهرس | **متوسط (Medium):** الاستعلام يفلتر بـ `movement_timestamp` بينما الفهرس أنشئ على `created_at`، مما يبطل ميزة التصفح السريع للدفتر في PostgreSQL. |
| **تفعيل التخزين المؤقت وإبطاله (Cache Invalidation & Eviction)** | **FAIL** | [CacheConfig.java](file:///g:/ERP_BackEnd/backend/paper-erp-api/src/main/java/com/papererp/api/config/CacheConfig.java)<br>[MasterDataService.java](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/main/java/com/papererp/core/service/MasterDataService.java) | فحص وجود وسوم `@Cacheable` أو `@CacheEvict` | **متوسط (Medium):** الكاش مهيأ كـ Bean ولكن لا توجد وسوم `@Cacheable` على الخدمات، وبالتالي نسبة الـ Hit هي 0% ولا يوجد إبطال للكاش. |
| **مكافحة التخمين في التوزيع الموزع (Rate Limiting Distribution)** | **PARTIAL** | [RateLimitingService.java](file:///g:/ERP_BackEnd/backend/paper-erp-api/src/main/java/com/papererp/api/security/RateLimitingService.java) | فحص نوع التخزين وهيدر الـ IP | **متوسط (Medium):** يعمل محلياً بذاكرة `ConcurrentHashMap` لحالة الخادم الفردي، لكنه غير موزع (يحتاج Redis في العنقود) ولا يدعم `X-Forwarded-For`. |
| **صندوق الصادر المتزامن الموزع (Transactional Outbox Scaling)** | **PARTIAL** | [OutboxService.java](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/main/java/com/papererp/core/service/OutboxService.java) | فحص استعلام الجدولة `processPendingEvents` | **متوسط (Medium):** حفظ الحدث ذري مع المعاملة، ولكن عامل الجدولة لا يستخدم `FOR UPDATE SKIP LOCKED`؛ نشر مثيلات متعددة سيسبب تكرار نشر الأحداث. |
| **حظر المفاتيح الضعيفة في JWT (JWT Key Length Enforcement)** | **PARTIAL** | [JwtTokenService.java](file:///g:/ERP_BackEnd/backend/paper-erp-api/src/main/java/com/papererp/api/security/JwtTokenService.java#L28-L33) | فحص تهيئة المفتاح | **منخفض (Low):** النظام يكمل المفاتيح الأقل من 32 بايت بأصفار بدلاً من رفضها ورمي استثناء تهيئة صارم، ولا يفحص `iss` أو `aud`. |
| **اختبارات التزامن ضد PostgreSQL فعلية (Real DB Concurrency)** | **BLOCKED** | [HighConcurrencySlittingTest.java](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/test/java/com/papererp/core/service/HighConcurrencySlittingTest.java) | فحص طبيعة الاختبار | **محظور بيئياً (Blocked):** الاختبار أثبت تزامن الذاكرة في Java (10 خيوط)، لكنه لم يجرِ ضد قاعدة PostgreSQL حية لعدم توفر Docker/Testcontainers على الجهاز. |
| **الاتصال بمشروع Supabase الإنتاجي (Supabase Cloud Tests)** | **BLOCKED** | [SupabaseConnectionSmokeTest.java](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/test/java/com/papererp/core/supabase/SupabaseConnectionSmokeTest.java) | فحص المتغير البيئي `SUPABASE_DB_URL` | **محظور أمنياً (Blocked):** محجوب عمداً لحماية بيانات العميل وقاعدة البيانات الحية من أي مساس قبل أخذ نسخة احتياطية معتمدة. |
| **خطة التعافي من الكوارث (RPO < 5m / RTO < 15m Drill)** | **BLOCKED** | [resilience-and-disaster-recovery.md](file:///g:/ERP_BackEnd/docs/operations/resilience-and-disaster-recovery.md) | فحص سجلات التشغيل واختبارات الاستعادة | **أهداف موثقة فقط (Documented Targets Only):** لم يتم إجراء تجربة استعادة فعلية محددة بالوقت لإثبات تحقيق الـ RPO والـ RTO تجريبياً. |

---

## 3. تفاصيل العيوب المؤكدة والإجراءات التصحيحية الموصى بها (Confirmed Defects & Fixes)

### 🚨 العيب الأول (حرج جداً): انفصال جلسة الـ RLS في قاعدة البيانات عن سياق Spring Boot
- **الوصف:** تفرض ترحيلات SQL سياسات RLS باستخدام الشرط:
  ```sql
  USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
  ```
  ولكن لا يوجد أي مكون في Spring Data JPA / Hibernate يقوم بتنفيذ:
  ```sql
  SET LOCAL app.current_tenant_id = '<authenticated-tenant-id>';
  ```
- **الخطر:** إذا اتصل التطبيق بمستخدم ذي صلاحيات محدودة (كما يجب إنتاجياً)، فإن دالة `current_setting` تعيد `NULL` دائماً، وبالتالي **ترفض قاعدة البيانات إرجاع أي صف في كافة الجداول**! وإذا اتصل بدور `postgres` الأعلى، يُلغى تفعيل الـ RLS تماماً.
- **الحل الأدنى:** إنشاء `HibernateConnectionInterceptor` أو `TenantAwareDataSourceAspect` يستمع لفتح الاتصال وينفذ `SET LOCAL app.current_tenant_id` داخل نفس معاملة الـ JDBC.

---

### ⚠️ العيب الثاني (متوسط): عدم ربط خدمة الـ Idempotency العامة
- **الوصف:** تم بناء خدمة [IdempotencyService.java](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/main/java/com/papererp/core/service/IdempotencyService.java) وجدولها، ولكن لا توجد طبقة Filter تعترض طلبات الـ HTTP تلقائياً وتفحص رأس `Idempotency-Key`. كما أن الخدمة الحالية لا تتحقق من تطابق حمولة الطلب وتسمح بتجاوز الفحص إذا تكرر المفتاح بحمولة مختلفة.
- **الحل الأدنى:**
  1. إضافة تحقق:
     ```java
     if (!record.getRequestHash().equals(requestHash)) {
         throw new ValidationException("Idempotency key reused with different request payload");
     }
     ```
  2. إنشاء `IdempotencyFilter` يغلف الـ POST requests في مسارات الإنتاج والمخازن.

---

### ⚠️ العيب الثالث (متوسط): عدم تطابق أعمدة فهرس التصفح السريع للدفتر
- **الوصف:** استعلام `InventoryLedgerRepository`:
  `findByTenantIdAndPlantIdAndMovementTimestampLessThanOrderByMovementTimestampDesc`
  بينما الفهرس في `V2`:
  `CREATE INDEX idx_ledger_keyset ON inventory_ledger (tenant_id, created_at DESC, id DESC);`
- **الحل الأدنى:** تعديل الفهرس في ترحيل تصحيحي `V3__fix_ledger_keyset_index.sql`:
  ```sql
  CREATE INDEX idx_ledger_keyset_v2 ON inventory_ledger (tenant_id, plant_id, movement_timestamp DESC, id DESC);
  ```

---

### ⚠️ العيب الرابع (متوسط): غياب وسوم الكاش على الخدمات
- **الوصف:** تم تهيئة `CaffeineCacheManager` في [CacheConfig.java](file:///g:/ERP_BackEnd/backend/paper-erp-api/src/main/java/com/papererp/api/config/CacheConfig.java)، ولكن لا يوجد وسم `@Cacheable` واحد في [MasterDataService.java](file:///g:/ERP_BackEnd/backend/paper-erp-core/src/main/java/com/papererp/core/service/MasterDataService.java) ولا توجد وسوم `@CacheEvict` عند تحديث البيانات.
- **الحل الأدنى:** إضافة `@Cacheable(value = "masterDataGrades", key = "#tenantId.toString() + ':' + #code")` على دوال الاستعلام، و `@CacheEvict` على دوال الحفظ والتعديل.

---

### ⚠️ العيب الخامس (متوسط): قفل أحداث الـ Outbox في المعمارية الموزعة
- **الوصف:** استعلام الـ `OutboxService.processPendingEvents()` يقرأ الأحداث دون قفل صفي:
  `SELECT o FROM OutboxEventEntity o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC`
  مما يؤدي في حال تشغيل أكثر من مثيل خادم (Horizontally Scaled Instances) إلى سحب نفس الأحداث ونشرها مرتين.
- **الحل الأدنى:** استخدام استعلام مقفل:
  ```sql
  SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 50 FOR UPDATE SKIP LOCKED
  ```
  أو استخدام مكتبة قفل موزع مثل ShedLock.

---

## 4. إثباتات الاختبار والتحقق بالأرقام الفعلية (Empirical Evidence)

### أ. نتائج دورة اختبارات Maven المستقلة:
- **الأمر:** `mvnw.cmd clean package`
- **كود الخروج (Exit Code):** `0` (BUILD SUCCESS)
- **الوحدات المنفذة:**
  - `paper-erp-parent`: SUCCESS
  - `paper-erp-common`: 3 tests, 0 failures, 0 errors, 0 skipped
  - `paper-erp-core`: 12 tests, 0 failures, 0 errors, 0 skipped
  - `paper-erp-api`: 7 tests, 0 failures, 0 errors, 0 skipped
- **إجمالي الاختبارات:** **22 اختباراً فعلياً** مسجلة في ملفات Surefire TXT/XML المستقلة.

### ب. تشغيل الحزمة التنفيذية الحية (Live Smoke Evidence):
- **الأمر:** `java -jar backend\paper-erp-api\target\paper-erp-api-1.0.0-SNAPSHOT.jar --server.port=8089`
- **زمن الإقلاع:** 19.885 ثانية.
- **المنفذ الحي:** `8089`.
- **فحص الاستجابة الحية (`Invoke-RestMethod`):**
  - استجابة `GET http://localhost:8089/api/v1/health`:
    `{"success":true,"message":"System is healthy and operational","data":{"version":"1.0.0","status":"UP"}}`
  - استجابة `GET http://localhost:8089/actuator/health`:
    `{"status":"UP"}`
  - استجابة `GET http://localhost:8089/v3/api-docs`:
    OpenAPI 3.1.0 JSON كامل بمواصفات المصنع والتصنيع.

---

## 5. مصفوفة المخاطر المتبقية والأولويات العاجلة (Residual Risk Matrix)

```
[حرج]  - غياب كود ربط جلسة RLS (يؤدي لتعطل الاستعلامات تحت مستخدم مقيد الصلاحيات)
[مرتفع] - عدم استخدام FOR UPDATE SKIP LOCKED في معالجة أحداث الـ Outbox
[متوسط] - عدم فحص تكرار حمولة الطلب في الـ Idempotency وعدم تفعيل الـ Filter
[متوسط] - عدم تطابق أعمدة الفهرس المركب مع استعلام تصفح الدفتر
[متوسط] - عدم توزيع الـ Rate Limiting واعتماده على ذاكرة التطبيق المحلية المفردة
[منخفض] - التوثيق فقط لأهداف RPO < 5m و RTO < 15m دون تجربة استعادة فعلية
```

---

## 6. الأوامر المستقلة لإعادة إنتاج التدقيق (Reproduction Commands)

```powershell
# 1. ضبط بيئة الجافا
$env:JAVA_HOME = "C:\Users\it2_specialist\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.20.101-hotspot"

# 2. تشغيل البناء الكامل والتحقق من 22 اختباراً
cd g:\ERP_BackEnd\backend
.\mvnw.cmd clean package

# 3. التحقق من تقارير Surefire المستقلة
Get-Content -Path g:\ERP_BackEnd\backend\*\target\surefire-reports\*.txt

# 4. تشغيل الحزمة والتحقق من نقاط الفحص الحية
java -jar g:\ERP_BackEnd\backend\paper-erp-api\target\paper-erp-api-1.0.0-SNAPSHOT.jar --spring.datasource.url=jdbc:postgresql://localhost:5432/postgres --spring.flyway.enabled=false --server.port=8089
Invoke-RestMethod -Uri "http://localhost:8089/api/v1/health"
```
