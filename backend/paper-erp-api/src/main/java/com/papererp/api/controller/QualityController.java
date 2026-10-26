package com.papererp.api.controller;

import com.papererp.common.dto.ApiResponse;
import com.papererp.core.dto.QualityDto;
import com.papererp.core.service.QualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quality")
@Tag(name = "Quality Control", description = "مراقبة الجودة: تسجيل نتائج الفحص المعملي واعتماد قرارات الإفراج أو الحجر أو التكهين")
public class QualityController {

    private final QualityService qualityService;

    public QualityController(QualityService qualityService) {
        this.qualityService = qualityService;
    }

    @PostMapping("/inspections")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER', 'QC_INSPECTOR')")
    @Operation(summary = "تسجيل فحص معملي جديد", description = "تسجيل نتائج الفحص المخبري (الجراماج، الرطوبة، السمك، قوة الانفجار) وتحديد حالة الإفراج أو الحجر")
    public ResponseEntity<ApiResponse<QualityDto.QualityInspectionDto>> createInspection(
            @Valid @RequestBody QualityDto.CreateInspectionRequest request) {
        return ResponseEntity.ok(ApiResponse.created("تم تسجيل الفحص المعملي بنجاح", qualityService.recordInspection(request)));
    }

    @GetMapping("/inspections/{id}")
    @Operation(summary = "عرض نتيجة الفحص", description = "استرجاع تفاصيل الفحص المعملي بواسطة المعرف الفريد")
    public ResponseEntity<ApiResponse<QualityDto.QualityInspectionDto>> getInspection(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(qualityService.getInspectionById(id)));
    }

    @PostMapping("/inspections/{id}/disposition")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER', 'QC_INSPECTOR')")
    @Operation(summary = "تحديث قرار الجودة", description = "تحديث حالة البكرة (إفراج معتمد، حجر صحي، تكهين، خفض رتبة) مع تسجيل سبب القرار في سجل المراجعة")
    public ResponseEntity<ApiResponse<QualityDto.QualityInspectionDto>> updateDisposition(
            @PathVariable UUID id,
            @Valid @RequestBody QualityDto.UpdateDispositionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("تم تحديث قرار الجودة للبكرة بنجاح", qualityService.updateDisposition(id, request)));
    }
}
