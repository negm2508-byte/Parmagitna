package com.papererp.api.controller;

import com.papererp.common.dto.ApiResponse;
import com.papererp.core.dto.InventoryDto;
import com.papererp.core.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rolls")
@Tag(name = "Roll Traceability", description = "تتبع البكر: البحث الشامل بواسطة الرقم التسلسلي أو الباركود واستعراض شجرة الأنساب وسجل الحركات وفحوصات الجودة")
public class RollTraceabilityController {

    private final InventoryService inventoryService;

    public RollTraceabilityController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/lookup/{serialOrBarcode}")
    @Operation(summary = "البحث وتتبع البكرة", description = "البحث الفوري عن بكرة جامبو أو بكرة مقطعة بواسطة الباركود أو السيريال واسترجاع شجرة النسب وسجل حركات المخزن وتاريخ الفحص المعملي")
    public ResponseEntity<ApiResponse<InventoryDto.RollTraceabilityDto>> lookupRoll(
            @PathVariable String serialOrBarcode) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.lookupRoll(serialOrBarcode)));
    }
}
