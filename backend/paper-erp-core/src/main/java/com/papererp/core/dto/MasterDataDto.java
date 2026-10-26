package com.papererp.core.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public final class MasterDataDto {

    private MasterDataDto() {}

    public record PlantDto(
            UUID id,
            UUID tenantId,
            String code,
            String nameAr,
            String nameEn,
            boolean active
    ) {}

    public record CreatePlantRequest(
            @NotBlank(message = "Plant code is required")
            String code,

            @NotBlank(message = "Arabic name is required")
            String nameAr,

            @NotBlank(message = "English name is required")
            String nameEn
    ) {}

    public record MachineDto(
            UUID id,
            UUID tenantId,
            UUID plantId,
            String code,
            String nameAr,
            String machineType,
            BigDecimal maxDeckleWidthMm,
            BigDecimal minTrimWidthMm,
            boolean active
    ) {}

    public record CreateMachineRequest(
            @NotNull(message = "Plant ID is required")
            UUID plantId,

            @NotBlank(message = "Machine code is required")
            String code,

            @NotBlank(message = "Arabic name is required")
            String nameAr,

            @NotBlank(message = "Machine type is required")
            String machineType,

            @NotNull(message = "Max deckle width is required")
            @DecimalMin(value = "0.01", message = "Max deckle width must be positive")
            BigDecimal maxDeckleWidthMm,

            @NotNull(message = "Min trim width is required")
            @DecimalMin(value = "0.00", message = "Min trim width must be non-negative")
            BigDecimal minTrimWidthMm
    ) {}

    public record PaperGradeDto(
            UUID id,
            UUID tenantId,
            String code,
            String nameAr,
            BigDecimal gsmTarget,
            BigDecimal gsmMin,
            BigDecimal gsmMax,
            BigDecimal caliperTargetUm,
            BigDecimal moistureTargetPct,
            BigDecimal burstIndexTarget,
            boolean active
    ) {}

    public record CreatePaperGradeRequest(
            @NotBlank(message = "Grade code is required")
            String code,

            @NotBlank(message = "Arabic name is required")
            String nameAr,

            @NotNull(message = "Target GSM is required")
            @DecimalMin(value = "1.00", message = "Target GSM must be positive")
            BigDecimal gsmTarget,

            @NotNull(message = "Min GSM is required")
            @DecimalMin(value = "1.00", message = "Min GSM must be positive")
            BigDecimal gsmMin,

            @NotNull(message = "Max GSM is required")
            @DecimalMin(value = "1.00", message = "Max GSM must be positive")
            BigDecimal gsmMax,

            @NotNull(message = "Caliper target is required")
            BigDecimal caliperTargetUm,

            @NotNull(message = "Moisture target is required")
            BigDecimal moistureTargetPct,

            @NotNull(message = "Burst index target is required")
            BigDecimal burstIndexTarget
    ) {}
}
