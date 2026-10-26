package com.papererp.core.service;

import com.papererp.common.context.TenantContext;
import com.papererp.common.domain.*;
import com.papererp.common.exception.*;
import com.papererp.core.domain.*;
import com.papererp.core.dto.ProductionDto;
import com.papererp.core.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SlittingOperationService {

    private final JumboRollRepository jumboRollRepository;
    private final SlittingRunRepository slittingRunRepository;
    private final SlitRollRepository slitRollRepository;
    private final MachineRepository machineRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final MasterDataService masterDataService;
    private final AuditService auditService;
    private final OutboxService outboxService;

    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    public SlittingOperationService(JumboRollRepository jumboRollRepository,
                                   SlittingRunRepository slittingRunRepository,
                                   SlitRollRepository slitRollRepository,
                                   MachineRepository machineRepository,
                                   InventoryLedgerRepository inventoryLedgerRepository,
                                   MasterDataService masterDataService,
                                   AuditService auditService,
                                   OutboxService outboxService) {
        this.jumboRollRepository = jumboRollRepository;
        this.slittingRunRepository = slittingRunRepository;
        this.slitRollRepository = slitRollRepository;
        this.machineRepository = machineRepository;
        this.inventoryLedgerRepository = inventoryLedgerRepository;
        this.masterDataService = masterDataService;
        this.auditService = auditService;
        this.outboxService = outboxService;
    }

    @Transactional
    public ProductionDto.SlittingRunDto executeSlitting(ProductionDto.ExecuteSlittingRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID operatorId = TenantContext.getRequiredUserId();

        // 1. Idempotency Check: Protect against duplicate concurrent submission
        if (slittingRunRepository.existsByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey())) {
            var existingRun = slittingRunRepository.findByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey())
                    .orElseThrow();
            return mapRunToDto(existingRun, slitRollRepository.findAllByTenantIdAndSlittingRunId(tenantId, existingRun.getId()));
        }

        // 2. Lock Parent Jumbo Roll with Pessimistic Write Lock
        JumboRollEntity parentJumbo = jumboRollRepository.findWithLockByTenantIdAndId(tenantId, request.parentJumboId())
                .orElseThrow(() -> new EntityNotFoundException("JumboRoll", request.parentJumboId()));

        // 3. Roll State Validation
        if (!parentJumbo.getStatus().isEligibleForSlitting()) {
            throw new IneligibleRollException(String.format(
                    "Parent Jumbo Roll %s is in state '%s' and is ineligible for slitting. Must be AVAILABLE.",
                    parentJumbo.getSerialNumber(), parentJumbo.getStatus()
            ));
        }

        // 4. Machine Validation
        MachineEntity slittingMachine = machineRepository.findByTenantIdAndPlantIdAndId(
                tenantId, parentJumbo.getPlantId(), request.machineId()
        ).orElseThrow(() -> new EntityNotFoundException("Slitting Machine in Plant", request.machineId()));

        // 5. Deckle Width Reconciliation
        BigDecimal sumCutWidths = BigDecimal.ZERO;
        for (var cut : request.cuts()) {
            sumCutWidths = sumCutWidths.add(cut.cutWidthMm());
        }
        sumCutWidths = sumCutWidths.setScale(2, RoundingMode.HALF_UP);

        BigDecimal minRequiredTrim = slittingMachine.getMinTrimWidthMm() != null
                ? slittingMachine.getMinTrimWidthMm()
                : BigDecimal.ZERO;

        BigDecimal requiredTotalWidth = sumCutWidths.add(minRequiredTrim);
        if (requiredTotalWidth.compareTo(parentJumbo.getDeckleWidthMm()) > 0) {
            throw new DomainException("DECKLE_WIDTH_EXCEEDED", String.format(
                    "Total cut width (%s mm) + minimum required trim (%s mm) exceeds parent deckle width (%s mm)",
                    sumCutWidths, minRequiredTrim, parentJumbo.getDeckleWidthMm()
            ), 422);
        }

        // 6. Mass Balance Accounting
        BigDecimal inputNetMass = parentJumbo.getNetWeightKg().setScale(2, RoundingMode.HALF_UP);
        BigDecimal childRollsTotalNet = BigDecimal.ZERO;

        List<PreparedChildRoll> preparedRolls = new ArrayList<>();
        String datePart = DATE_PREFIX.format(Instant.now());

        for (var cut : request.cuts()) {
            BigDecimal cutGross = cut.grossWeightKg().setScale(2, RoundingMode.HALF_UP);
            BigDecimal cutTare = cut.tareWeightKg().setScale(2, RoundingMode.HALF_UP);
            if (cutGross.compareTo(cutTare) <= 0) {
                throw new DomainException("INVALID_CUT_WEIGHTS", "Child roll gross weight must be strictly greater than tare weight", 422);
            }
            BigDecimal cutNet = cutGross.subtract(cutTare).setScale(2, RoundingMode.HALF_UP);
            childRollsTotalNet = childRollsTotalNet.add(cutNet);

            String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String childSerial = String.format("SR-%s-%02d-%s", datePart, cut.cutSequenceNumber(), randomSuffix);
            String childBarcode = String.format("BC-SR%s%s", datePart, randomSuffix);

            preparedRolls.add(new PreparedChildRoll(
                    cut.cutSequenceNumber(), cut.cutWidthMm(), cut.diameterMm(),
                    cutGross, cutTare, cutNet, cut.coreSpecId(), childSerial, childBarcode
            ));
        }
        childRollsTotalNet = childRollsTotalNet.setScale(2, RoundingMode.HALF_UP);

        BigDecimal recordedWaste = request.recordedTrimWasteKg().setScale(2, RoundingMode.HALF_UP);
        BigDecimal accountedMass = childRollsTotalNet.add(recordedWaste).setScale(2, RoundingMode.HALF_UP);

        // Scale Variance Equation: Accounted Mass - Input Net Mass
        BigDecimal scaleVarianceKg = accountedMass.subtract(inputNetMass).setScale(2, RoundingMode.HALF_UP);

        // 7. Scale Variance Tolerance Check
        BigDecimal tolerancePct = masterDataService.getTolerancePercentageOrDefault(tenantId, parentJumbo.getPlantId());
        BigDecimal maxAllowedVarianceKg = inputNetMass.multiply(tolerancePct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal absVariance = scaleVarianceKg.abs();
        if (absVariance.compareTo(maxAllowedVarianceKg) > 0) {
            throw new MassBalanceExceededToleranceException(
                    inputNetMass, accountedMass, scaleVarianceKg, maxAllowedVarianceKg,
                    String.format("Mass balance variance of %s kg exceeds maximum permitted tolerance of ±%s kg (%s%% of input mass %s kg)",
                            scaleVarianceKg, maxAllowedVarianceKg, tolerancePct, inputNetMass)
            );
        }

        MassBalanceStatus balanceStatus = (scaleVarianceKg.compareTo(BigDecimal.ZERO) == 0)
                ? MassBalanceStatus.BALANCED_EXACT
                : MassBalanceStatus.BALANCED_WITHIN_TOLERANCE;

        WasteDisposition wasteDisposition = request.wasteDisposition() != null
                ? request.wasteDisposition()
                : WasteDisposition.BALED_FOR_RECYCLE;

        // 8. Create and Persist Slitting Run Aggregate
        SlittingRunEntity run = new SlittingRunEntity(
                tenantId, parentJumbo.getPlantId(), parentJumbo.getId(), slittingMachine.getId(),
                operatorId, request.idempotencyKey(), parentJumbo.getGrossWeightKg(),
                parentJumbo.getTareWeightKg(), inputNetMass, parentJumbo.getDeckleWidthMm(),
                childRollsTotalNet, recordedWaste, scaleVarianceKg, balanceStatus, wasteDisposition
        );
        SlittingRunEntity savedRun = slittingRunRepository.save(run);

        // 9. Persist Child Slit Rolls
        List<SlitRollEntity> savedChildRolls = new ArrayList<>();
        for (PreparedChildRoll prepared : preparedRolls) {
            SlitRollEntity slitRoll = new SlitRollEntity(
                    tenantId, parentJumbo.getPlantId(), parentJumbo.getId(), savedRun.getId(),
                    parentJumbo.getGradeId(), prepared.coreSpecId() != null ? prepared.coreSpecId() : parentJumbo.getCoreSpecId(),
                    prepared.serial(), prepared.barcode(), prepared.sequence(), prepared.cutWidthMm(),
                    prepared.diameterMm(), prepared.gross(), prepared.tare(), prepared.net(),
                    operatorId
            );
            slitRoll.setLocationId(parentJumbo.getLocationId());
            SlitRollEntity savedRoll = slitRollRepository.save(slitRoll);
            savedChildRolls.add(savedRoll);

            // Record child roll production receipt in inventory ledger
            InventoryLedgerEntity receiptLedger = InventoryLedgerEntity.forSlit(
                    tenantId, parentJumbo.getPlantId(), InventoryMovementType.PRODUCTION_RECEIPT,
                    savedRoll.getId(), null, parentJumbo.getLocationId(),
                    BigDecimal.ONE, prepared.net(), operatorId, "SLITTING_RUN", savedRun.getId(),
                    String.format("Produced from parent %s in slitting run %s", parentJumbo.getSerialNumber(), savedRun.getId())
            );
            inventoryLedgerRepository.save(receiptLedger);
        }

        // 10. Record Parent Jumbo Consumption in Inventory Ledger
        InventoryLedgerEntity consumptionLedger = InventoryLedgerEntity.forJumbo(
                tenantId, parentJumbo.getPlantId(), InventoryMovementType.CONSUMPTION,
                parentJumbo.getId(), parentJumbo.getLocationId(), null,
                new BigDecimal("-1.00"), inputNetMass, operatorId, "SLITTING_RUN", savedRun.getId(),
                String.format("Consumed during slitting into %d child rolls", savedChildRolls.size())
        );
        inventoryLedgerRepository.save(consumptionLedger);

        // 11. Transition Parent Jumbo State to FULLY_SLIT
        parentJumbo.setStatus(RollStatus.FULLY_SLIT);
        jumboRollRepository.save(parentJumbo);

        // 12. Transactional Audit Log
        auditService.recordActionInTransaction(
                tenantId, operatorId, "EXECUTE_SLITTING", "SLITTING_RUN",
                savedRun.getId(), null,
                String.format("{\"parentRoll\":\"%s\",\"varianceKg\":%s,\"childrenCount\":%d}",
                        parentJumbo.getSerialNumber(), scaleVarianceKg, savedChildRolls.size())
        );

        // 13. Transactional Outbox Event for Asynchronous / Distributed Integration
        outboxService.recordEvent(
                tenantId, "SLITTING_RUN", savedRun.getId(), "PRODUCTION_SLITTING_COMPLETED",
                String.format("{\"runId\":\"%s\",\"parentRoll\":\"%s\",\"varianceKg\":%s,\"children\":%d}",
                        savedRun.getId(), parentJumbo.getSerialNumber(), scaleVarianceKg, savedChildRolls.size())
        );

        return mapRunToDto(savedRun, savedChildRolls);
    }

    @Transactional(readOnly = true)
    public ProductionDto.SlittingRunDto getSlittingRunById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        SlittingRunEntity run = slittingRunRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("SlittingRun", id));
        List<SlitRollEntity> rolls = slitRollRepository.findAllByTenantIdAndSlittingRunId(tenantId, id);
        return mapRunToDto(run, rolls);
    }

    private record PreparedChildRoll(
            int sequence, BigDecimal cutWidthMm, BigDecimal diameterMm,
            BigDecimal gross, BigDecimal tare, BigDecimal net,
            UUID coreSpecId, String serial, String barcode
    ) {}

    private ProductionDto.SlittingRunDto mapRunToDto(SlittingRunEntity run, List<SlitRollEntity> childRolls) {
        var rollDtos = childRolls.stream().map(this::mapSlitToDto).toList();
        return new ProductionDto.SlittingRunDto(
                run.getId(), run.getTenantId(), run.getPlantId(), run.getParentJumboId(),
                run.getMachineId(), run.getOperatorId(), run.getIdempotencyKey(),
                run.getInputGrossWeightKg(), run.getInputTareWeightKg(), run.getInputNetWeightKg(),
                run.getInputDeckleWidthMm(), run.getChildRollsTotalWeightKg(), run.getRecordedTrimWasteKg(),
                run.getScaleVarianceKg(), run.getMassBalanceStatus(), run.getWasteDisposition(),
                run.getStatus(), run.getStartedAt(), run.getCompletedAt(), rollDtos
        );
    }

    private ProductionDto.SlitRollDto mapSlitToDto(SlitRollEntity s) {
        return new ProductionDto.SlitRollDto(
                s.getId(), s.getTenantId(), s.getPlantId(), s.getParentJumboId(), s.getSlittingRunId(),
                s.getGradeId(), s.getCoreSpecId(), s.getLocationId(), s.getSerialNumber(), s.getBarcode(),
                s.getCutSequenceNumber(), s.getCutWidthMm(), s.getDiameterMm(), s.getGrossWeightKg(),
                s.getTareWeightKg(), s.getNetWeightKg(), s.getStatus(), s.getQualityDisposition(),
                s.getCreatedAt()
        );
    }
}
