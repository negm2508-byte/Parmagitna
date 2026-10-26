package com.papererp.core.dto;

import com.papererp.common.domain.InventoryMovementType;
import com.papererp.common.domain.QualityDisposition;
import com.papererp.common.domain.RollStatus;
import com.papererp.common.domain.RollType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class InventoryDto {

    private InventoryDto() {}

    public record TransferLocationRequest(
            @NotNull(message = "Roll type is required")
            RollType rollType,

            UUID jumboRollId,
            UUID slitRollId,

            @NotNull(message = "Destination location ID is required")
            UUID destinationLocationId,

            String reason
    ) {}

    public record DispatchRollRequest(
            @NotNull(message = "Roll type is required")
            RollType rollType,

            UUID jumboRollId,
            UUID slitRollId,

            String customerReference,
            String dispatchNotes
    ) {}

    public record InventoryLedgerDto(
            UUID id,
            UUID tenantId,
            UUID plantId,
            InventoryMovementType movementType,
            RollType rollType,
            UUID jumboRollId,
            UUID slitRollId,
            UUID sourceLocationId,
            UUID destinationLocationId,
            BigDecimal quantityChange,
            BigDecimal weightKg,
            UUID actorId,
            String referenceType,
            UUID referenceId,
            String reason,
            Instant movementTimestamp
    ) {}

    public record RollTraceabilityDto(
            RollType rollType,
            UUID rollId,
            String serialNumber,
            String barcode,
            RollStatus status,
            QualityDisposition qualityDisposition,
            UUID plantId,
            UUID gradeId,
            BigDecimal grossWeightKg,
            BigDecimal tareWeightKg,
            BigDecimal netWeightKg,
            UUID parentJumboId,
            UUID slittingRunId,
            List<InventoryLedgerDto> movements,
            List<QualityDto.QualityInspectionDto> inspections
    ) {}

    public record RollReconciliationDto(
            RollType rollType,
            UUID rollId,
            String serialNumber,
            RollStatus currentStatus,
            BigDecimal currentNetWeightKg,
            BigDecimal ledgerSumQuantity,
            boolean balanced,
            String reconciliationNotes
    ) {}
}
