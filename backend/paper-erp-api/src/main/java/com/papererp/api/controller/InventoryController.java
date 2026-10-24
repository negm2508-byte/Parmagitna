package com.papererp.api.controller;

import com.papererp.common.tenant.TenantContext;
import com.papererp.core.entity.InventoryMovement;
import com.papererp.core.repository.InventoryMovementRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryMovementRepository movementRepository;

    public InventoryController(InventoryMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    private UUID getTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    @GetMapping("/movements")
    public ResponseEntity<List<InventoryMovement>> listMovements() {
        return ResponseEntity.ok(movementRepository.findAllByTenantIdOrderByCreatedAtDesc(getTenantId()));
    }
}
