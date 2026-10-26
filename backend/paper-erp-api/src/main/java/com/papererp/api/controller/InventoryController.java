package com.papererp.api.controller;

import com.papererp.common.domain.RollType;
import com.papererp.common.dto.ApiResponse;
import com.papererp.common.dto.PageResponse;
import com.papererp.core.dto.InventoryDto;
import com.papererp.core.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory & Logistics", description = "المستودعات واللوجستيات: دفتر أستاذ الحركات، نقل المواقع، والصرف المقيد باعتماد الجودة")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/ledger")
    @Operation(summary = "دفتر أستاذ حركات المخزون", description = "استعراض حركات المخزون غير القابلة للتعديل مفلترة بحسب المصنع مع الترقيم")
    public ResponseEntity<ApiResponse<PageResponse<InventoryDto.InventoryLedgerDto>>> getLedger(
            @RequestParam UUID plantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLedger(plantId, page, size)));
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER', 'WAREHOUSE_KEEPER')")
    @com.papererp.common.annotation.IdempotentOperation
    @Operation(summary = "نقل موقع بكرة بالمخزن", description = "تحديث موقع تخزين البكرة مع تسجيل حركة نقل موقع بدفتر الأستاذ")
    public ResponseEntity<ApiResponse<InventoryDto.InventoryLedgerDto>> transferLocation(
            @Valid @RequestBody InventoryDto.TransferLocationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("تم نقل موقع البكرة بنجاح وتسجيل الحركة بدفتر الأستاذ", inventoryService.transferLocation(request)));
    }

    @PostMapping("/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANT_MANAGER', 'WAREHOUSE_KEEPER')")
    @com.papererp.common.annotation.IdempotentOperation
    @Operation(summary = "صرف وتحميل بكرة للعميل", description = "التحقق الصارم من بوابة الجودة (يجب أن تكون البكرة مفرج عنها RELEASED)، وتغيير حالتها إلى منصرفة وتسجيل حركة الصرف بدفتر الأستاذ")
    public ResponseEntity<ApiResponse<InventoryDto.InventoryLedgerDto>> dispatchRoll(
            @Valid @RequestBody InventoryDto.DispatchRollRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("تم صرف البكرة بنجاح وتسجيل حركة الصرف بدفتر الأستاذ", inventoryService.dispatchRoll(request)));
    }

    @GetMapping("/reconciliation")
    @Operation(summary = "مطابقة رصيد البكرة مع دفتر الأستاذ", description = "مقارنة الحالة الفيزيائية والوزن الحالي للبكرة مع مجموع حركات دفتر الأستاذ للتأكد من انعدام أي انحراف مخزني")
    public ResponseEntity<ApiResponse<InventoryDto.RollReconciliationDto>> reconcileRoll(
            @RequestParam RollType rollType,
            @RequestParam UUID rollId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.reconcileRoll(rollType, rollId)));
    }
}
