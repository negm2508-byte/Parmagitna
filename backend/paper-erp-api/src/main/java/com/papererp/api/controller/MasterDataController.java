package com.papererp.api.controller;

import com.papererp.common.dto.ApiResponse;
import com.papererp.core.dto.MasterDataDto;
import com.papererp.core.service.MasterDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/masterdata")
@Tag(name = "Master Data", description = "إدارة البيانات الأساسية: المصانع والماكينات وأصناف الورق ومحددات التشغيل")
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    // --- Plants ---
    @GetMapping("/plants")
    @Operation(summary = "قائمة المصانع", description = "عرض كافة المصانع التابعة للمؤسسة الموثقة")
    public ResponseEntity<ApiResponse<List<MasterDataDto.PlantDto>>> getPlants() {
        return ResponseEntity.ok(ApiResponse.ok(masterDataService.getAllPlants()));
    }

    @PostMapping("/plants")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER')")
    @Operation(summary = "إضافة مصنع جديد", description = "إنشاء مصنع جديد ضمن المؤسسة")
    public ResponseEntity<ApiResponse<MasterDataDto.PlantDto>> createPlant(
            @Valid @RequestBody MasterDataDto.CreatePlantRequest request) {
        return ResponseEntity.ok(ApiResponse.created("تم إنشاء المصنع بنجاح", masterDataService.createPlant(request)));
    }

    // --- Machines ---
    @GetMapping("/machines")
    @Operation(summary = "قائمة الماكينات", description = "عرض ماكينات الورق وماكينات التقطيع بحسب المصنع")
    public ResponseEntity<ApiResponse<List<MasterDataDto.MachineDto>>> getMachines(
            @RequestParam(required = false) UUID plantId) {
        return ResponseEntity.ok(ApiResponse.ok(masterDataService.getAllMachines(plantId)));
    }

    @PostMapping("/machines")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER')")
    @Operation(summary = "إضافة ماكينة جديدة", description = "تسجيل ماكينة تصنيع أو تقطيع مع تحديد أقصى عرض ديكل والحد الأدنى للهالك")
    public ResponseEntity<ApiResponse<MasterDataDto.MachineDto>> createMachine(
            @Valid @RequestBody MasterDataDto.CreateMachineRequest request) {
        return ResponseEntity.ok(ApiResponse.created("تم تسجيل الماكينة بنجاح", masterDataService.createMachine(request)));
    }

    // --- Paper Grades ---
    @GetMapping("/grades")
    @Operation(summary = "قائمة أصناف الورق", description = "عرض مواصفات أصناف الورق والجراماج وسماكة الورق المستهدفة")
    public ResponseEntity<ApiResponse<List<MasterDataDto.PaperGradeDto>>> getGrades() {
        return ResponseEntity.ok(ApiResponse.ok(masterDataService.getAllGrades()));
    }

    @PostMapping("/grades")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER')")
    @Operation(summary = "إضافة صنف ورق جديد", description = "تعريف صنف ورق جديد ومحددات الجودة المعملية المسموحة")
    public ResponseEntity<ApiResponse<MasterDataDto.PaperGradeDto>> createGrade(
            @Valid @RequestBody MasterDataDto.CreatePaperGradeRequest request) {
        return ResponseEntity.ok(ApiResponse.created("تم تعريف صنف الورق بنجاح", masterDataService.createGrade(request)));
    }
}
