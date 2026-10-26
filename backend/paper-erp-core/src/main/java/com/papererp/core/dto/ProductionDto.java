package com.papererp.core.dto;

import com.papererp.common.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProductionDto {

    private ProductionDto() {}

    public record CreateJumboRollRequest(
            @NotNull(message = "Plant ID is required")
            UUID plantId,

            @NotNull(message = "Machine ID is required")
            UUID machineId,

            @NotNull(message = "Grade ID is required")
            UUID gradeId,

            UUID coreSpecId,
            UUID locationId,

            @NotNull(message = "Gross weight is required")
            @DecimalMin(value = "0.01", message = "Gross weight must be positive")
            BigDecimal grossWeightKg,

            @NotNull(message = "Tare weight is required")
            @DecimalMin(value = "0.00", message = "Tare weight must be non-negative")
            BigDecimal tareWeightKg,

            @NotNull(message = "Deckle width is required")
            @DecimalMin(value = "0.01", message = "Deckle width must be positive")
            BigDecimal deckleWidthMm,

            BigDecimal diameterMm
    ) {}

    public record JumboRollDto(
            UUID id,
            UUID tenantId,
            UUID plantId,
            UUID machineId,
            UUID gradeId,
            UUID coreSpecId,
            UUID locationId,
            String serialNumber,
            String barcode,
            BigDecimal grossWeightKg,
            BigDecimal tareWeightKg,
            BigDecimal netWeightKg,
            BigDecimal deckleWidthMm,
            BigDecimal diameterMm,
            RollStatus status,
            QualityDisposition qualityDisposition,
            Instant productionTimestamp,
            UUID createdBy
    ) {}

    public record SlittingCutDto(
            int cutSequenceNumber,

            @NotNull(message = "Cut width is required")
            @DecimalMin(value = "0.01", message = "Cut width must be positive")
            BigDecimal cutWidthMm,

            BigDecimal diameterMm,

            @NotNull(message = "Cut gross weight is required")
            @DecimalMin(value = "0.01", message = "Cut gross weight must be positive")
            BigDecimal grossWeightKg,

            @NotNull(message = "Cut tare weight is required")
            @DecimalMin(value = "0.00", message = "Cut tare weight must be non-negative")
            BigDecimal tareWeightKg,

            UUID coreSpecId
    ) {}

    public record ExecuteSlittingRequest(
            @NotNull(message = "Parent Jumbo Roll ID is required")
            UUID parentJumboId,

            @NotNull(message = "Slitting Machine ID is required")
            UUID machineId,

            @NotBlank(message = "Idempotency key is required")
            String idempotencyKey,

            @NotNull(message = "Recorded trim waste is required")
            @DecimalMin(value = "0.00", message = "Trim waste must be non-negative")
            BigDecimal recordedTrimWasteKg,

            WasteDisposition wasteDisposition,

            @NotEmpty(message = "At least one slit cut is required")
            @Valid
            List<SlittingCutDto> cuts
    ) {}

    public record SlittingRunDto(
            UUID id,
            UUID tenantId,
            UUID plantId,
            UUID parentJumboId,
            UUID machineId,
            UUID operatorId,
            String idempotencyKey,
            BigDecimal inputGrossWeightKg,
            BigDecimal inputTareWeightKg,
            BigDecimal inputNetWeightKg,
            BigDecimal inputDeckleWidthMm,
            BigDecimal childRollsTotalWeightKg,
            BigDecimal recordedTrimWasteKg,
            BigDecimal scaleVarianceKg,
            MassBalanceStatus massBalanceStatus,
            WasteDisposition wasteDisposition,
            SlittingRunStatus status,
            Instant startedAt,
            Instant completedAt,
            List<SlitRollDto> childRolls
    ) {}

    public record SlitRollDto(
            UUID id,
            UUID tenantId,
            UUID plantId,
            UUID parentJumboId,
            UUID slittingRunId,
            UUID gradeId,
            UUID coreSpecId,
            UUID locationId,
            String serialNumber,
            String barcode,
            int cutSequenceNumber,
            BigDecimal cutWidthMm,
            BigDecimal diameterMm,
            BigDecimal grossWeightKg,
            BigDecimal tareWeightKg,
            BigDecimal netWeightKg,
            RollStatus status,
            QualityDisposition qualityDisposition,
            Instant createdAt
    ) {}
}
