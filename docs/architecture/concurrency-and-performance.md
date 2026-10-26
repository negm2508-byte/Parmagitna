# هندسة التزامن العالي والأداء الفائق — PaperERP Enterprise
# High Concurrency & Database Performance Architecture

**تاريخ الإصدار:** 2026-10-26  
**الإصدار:** 1.0.0-SNAPSHOT  
**الهدف:** ضمان سلامة العمليات وتماسك البيانات (ACID) عند تنفيذ مئات العمليات في نفس اللحظة دون تكرار أو تعارض.

---

## 1. التحكم في التزامن المتوازي (Concurrency Control)

في بيئة مصانع الورق، تتسابق عدة أجهزة طرفية (مقصات، رافعات شوكية، محطات فحص) لتحديث بيانات نفس الرولات والمخزون. لحماية النظام من حالات السباق (Race Conditions) وفقدان التحديثات (Lost Updates)، تم تطبيق استراتيجية مزدوجة:

### أ. القفل المتشائم الموجه (Pessimistic Write Locking)
في العمليات الحرجة التي تغير حالة الرول فيزيائياً (مثل عملية التقطيع الذري في `SlittingOperationService`):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT j FROM JumboRollEntity j WHERE j.tenantId = :tenantId AND j.id = :id")
Optional<JumboRollEntity> findWithLockByTenantIdAndId(@Param("tenantId") UUID tenantId, @Param("id") UUID id);
```
- يقوم محرك PostgreSQL بوضع قفل مستوى الصف (`SELECT ... FOR UPDATE`) على الرول الجامبو الأب.
- أي معاملة متزامنة أخرى تحاول تقطيع نفس الرول تنتظر أو تُرفض فوراً بمجرد انتقال حالته إلى `FULLY_SLIT`.

### ب. القفل المتفائل العام (Optimistic Locking via @Version)
لكافة الكيانات التشغيلية الأخرى (`JumboRollEntity`, `SlitRollEntity`, `UserEntity`):
```java
@Version
@Column(nullable = false)
private Long version = 0L;
```
- في حال تعديل رول من قبل مستخدمين متزامنين، يتم كشف التعارض ورمي `OptimisticLockingFailureException`.
- يقوم `GlobalExceptionHandler` بتحويل الخطأ إلى HTTP 409 Conflict مع رسالة إرشادية آمنة ومعرف التتبع `X-Correlation-ID`.

---

## 2. ضمان التنفيذ لمرة واحدة فقط (Idempotency Pattern)

لمنع تكرار العمليات الحساسة (مثل تكرار تقطيع رول أو خصم مخزون عند انقطاع الاتصال):
1. يرسل العميل رأس الطلب `Idempotency-Key: <unique-uuid>`.
2. يقوم `IdempotencyService` بفحص جدول `idempotency_records`:
   - إذا كانت العملية منتهية مسبقاً (`COMPLETED`)، يتم إرجاع الرد المخزن فوراً دون إعادة تنفيذ العملية.
   - إذا كانت قيد التنفيذ المتزامن (`PENDING`)، يتم رفض الطلب فوراً بكود `409 Conflict`.
   - إذا كانت جديدة، تُحفظ كـ `PENDING` ثم تُحدث إلى `COMPLETED` مع حفظ رمز الاستجابة.

---

## 3. نمط صندوق الصادر بالمعاملات (Transactional Outbox Pattern)

لضمان عدم وجود فجوة بين حفظ العملية في قاعدة البيانات ونشر الحدث إلى الأنظمة الموزعة (Outbox Event Publishing):
- عند نجاح عملية التقطيع أو الحركة المخزنية، يُكتب سجل الحدث في جدول `outbox_events` ضمن **نفس معاملة قاعدة البيانات المحلية**.
- يقوم عامل خلفي مجدول (`OutboxService.processPendingEvents`) بقراءة الأحداث ذات الحالة `PENDING` ونشرها تدريجياً مع إعادة المحاولة حتى 5 مرات وتحديث حالتها إلى `PUBLISHED`.

---

## 4. أحواض الخيوط المحدودة والتحكم في الضغط العكسي (Bounded Thread Pools & Backpressure)

لمنع انهيار الخادم أو نفاد الذاكرة عند ارتفاع عدد الطلبات الخلفية:
```java
executor.setCorePoolSize(8);
executor.setMaxPoolSize(32);
executor.setQueueCapacity(500);
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
```
- استخدام `CallerRunsPolicy` يضمن أنه عند امتلاء حوض المعالجة وطابور الانتظار (500 طلب)، يقوم الخيط الطالب بتنفيذ المهمة بنفسه، مما يبطئ استقبال الطلبات الجديدة طبيعياً دون إسقاط الخادم (Backpressure).

---

## 5. التصفح بالمفاتيح الرقمية (Keyset / Cursor Pagination)

لتجنب بطء الاستعلامات الكلاسيكية ذات الـ `OFFSET` الكبير في سجل الحركات المخزنية الضخم (`inventory_ledger`):
```java
List<InventoryLedgerEntity> findByTenantIdAndPlantIdAndMovementTimestampLessThanOrderByMovementTimestampDesc(
        UUID tenantId, UUID plantId, Instant before, Pageable pageable);
```
- يعتمد الاستعلام على الفهرس المركب `(tenant_id, created_at DESC, id DESC)`، مما يحافظ على زمن استجابة ثابت (O(log N)) مهما بلغ حجم السجلات بملايين الحركات.
