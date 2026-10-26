package com.papererp.api.controller;

import com.papererp.common.domain.RollStatus;
import com.papererp.common.dto.ApiResponse;
import com.papererp.common.dto.PageResponse;
import com.papererp.core.dto.ProductionDto;
import com.papererp.core.service.JumboRollService;
import com.papererp.core.service.SlittingOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/production")
@Tag(name = "Production", description = "عمليات التصنيع: تسجيل بكر الجامبو، والتقطيع الذري مع توازن الكتلة وتتبع شجرة الأنساب")
public class ProductionController {

    private final JumboRollService jumboRollService;
    private final SlittingOperationService slittingOperationService;

    public ProductionController(JumboRollService jumboRollService,
                                SlittingOperationService slittingOperationService) {
        this.jumboRollService = jumboRollService;
        this.slittingOperationService = slittingOperationService;
    }

    @PostMapping("/jumbo-rolls")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER', 'PRODUCTION_OPERATOR')")
    @com.papererp.common.annotation.IdempotentOperation
    @Operation(summary = "تسجيل بكرة جامبو جديدة", description = "تسجيل بكرة جامبو منتجة من ماكينة الورق واحتساب الوزن الصافي وحفظ حركة الاستلام بالمخزن")
    public ResponseEntity<ApiResponse<ProductionDto.JumboRollDto>> createJumboRoll(
            @Valid @RequestBody ProductionDto.CreateJumboRollRequest request) {
        return ResponseEntity.ok(ApiResponse.created("تم تسجيل بكرة الجامبو بنجاح", jumboRollService.registerJumboRoll(request)));
    }

    @GetMapping("/jumbo-rolls/{id}")
    @Operation(summary = "عرض بكرة جامبو", description = "استرجاع بيانات بكرة الجامبو بواسطة المعرف الفريد")
    public ResponseEntity<ApiResponse<ProductionDto.JumboRollDto>> getJumboRoll(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(jumboRollService.getJumboRollById(id)));
    }

    @GetMapping("/jumbo-rolls")
    @Operation(summary = "قائمة بكر الجامبو", description = "عرض قائمة بكر الجامبو مفلترة بحسب المصنع وحالة التشغيل")
    public ResponseEntity<ApiResponse<PageResponse<ProductionDto.JumboRollDto>>> listJumboRolls(
            @RequestParam(required = false) UUID plantId,
            @RequestParam(required = false) RollStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(jumboRollService.listJumboRolls(plantId, status, page, size)));
    }

    @PostMapping("/slitting-operations")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER', 'PRODUCTION_OPERATOR')")
    @com.papererp.common.annotation.IdempotentOperation
    @Operation(summary = "تنفيذ عملية تقطيع ذرية", description = "قفل بكرة الجامبو، والتحقق من حدود الديكل، وحساب توازن الكتلة وفروق الموازين، وتوليد بكر التقطيع، وترحيل حركات الاستهلاك والاستلام بدفتر الأستاذ")
    public ResponseEntity<ApiResponse<ProductionDto.SlittingRunDto>> executeSlitting(
            @Valid @RequestBody ProductionDto.ExecuteSlittingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("تم تنفيذ عملية التقطيع بنجاح واعتماد توازن الكتلة", slittingOperationService.executeSlitting(request)));
    }

    @GetMapping("/slitting-runs/{id}")
    @Operation(summary = "عرض أمر التقطيع المنفذ", description = "استرجاع تفاصيل أمر التقطيع وفروق الميزان المسجلة وبكر التقطيع الناتج")
    public ResponseEntity<ApiResponse<ProductionDto.SlittingRunDto>> getSlittingRun(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(slittingOperationService.getSlittingRunById(id)));
    }
}
