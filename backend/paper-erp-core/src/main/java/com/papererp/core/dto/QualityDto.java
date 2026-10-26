package com.papererp.core.dto;

import com.papererp.common.domain.QualityDisposition;
import com.papererp.common.domain.RollType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class QualityDto {

    private QualityDto() {}

    public record CreateInspectionRequest(
            @NotNull(message = "Plant ID is required")
            UUID plantId,

            @NotNull(message = "Roll type is required")
            RollType rollType,

            UUID jumboRollId,
            UUID slitRollId,

            BigDecimal measuredGsm,
            BigDecimal measuredMoisturePct,
            BigDecimal measuredCaliperUm,
            BigDecimal measuredBurstStrength,

            @NotNull(message = "Disposition is required")
            QualityDisposition disposition,

            String notes
    ) {}

    public record UpdateDispositionRequest(
            @NotNull(message = "New disposition is required")
            QualityDisposition disposition,

            String reason
    ) {}

    public record QualityInspectionDto(
            UUID id,
            UUID tenantId,
            UUID plantId,
            RollType rollType,
            UUID jumboRollId,
            UUID slitRollId,
            UUID inspectorId,
            BigDecimal measuredGsm,
            BigDecimal measuredMoisturePct,
            BigDecimal measuredCaliperUm,
            BigDecimal measuredBurstStrength,
            QualityDisposition disposition,
            String notes,
            Instant inspectionTimestamp
    ) {}
}
