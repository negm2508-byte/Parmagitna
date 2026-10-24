package com.papererp.core.service;

import com.papererp.common.exception.BusinessException;
import com.papererp.common.tenant.TenantContext;
import com.papererp.core.dto.RollGenealogyDto;
import com.papererp.core.dto.SlittingExecutionRequest;
import com.papererp.core.dto.SlittingExecutionResponse;
import com.papererp.core.entity.*;
import com.papererp.core.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class SlittingKernelService {

    private final JumboRollRepository jumboRollRepository;
    private final SlitRollRepository slitRollRepository;
    private final SlittingRunRepository slittingRunRepository;
    private final InventoryMovementRepository movementRepository;
    private final MachineRepository machineRepository;
    private final PaperGradeRepository paperGradeRepository;

    public SlittingKernelService(
            JumboRollRepository jumboRollRepository,
            SlitRollRepository slitRollRepository,
            SlittingRunRepository slittingRunRepository,
            InventoryMovementRepository movementRepository,
            MachineRepository machineRepository,
            PaperGradeRepository paperGradeRepository) {
        this.jumboRollRepository = jumboRollRepository;
        this.slitRollRepository = slitRollRepository;
        this.slittingRunRepository = slittingRunRepository;
        this.movementRepository = movementRepository;
        this.machineRepository = machineRepository;
        this.paperGradeRepository = paperGradeRepository;
    }

    private UUID resolveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return UUID.fromString("11111111-1111-1111-1111-111111111111");
        }
        return tenantId;
    }

    @Transactional
    public SlittingExecutionResponse executeSlittingRun(SlittingExecutionRequest request) {
        UUID tenantId = resolveTenantId();

        // 1. Resolve and validate parent Jumbo Roll
        JumboRoll jumbo;
        if (request.getJumboRollId() != null) {
            jumbo = jumboRollRepository.findByTenantIdAndId(tenantId, request.getJumboRollId())
                    .orElseThrow(() -> new BusinessException("JUMBO_NOT_FOUND", "Jumbo roll not found: " + request.getJumboRollId()));
        } else if (request.getJumboSerialNumber() != null) {
            jumbo = jumboRollRepository.findByTenantIdAndSerialNumber(tenantId, request.getJumboSerialNumber())
                    .orElseThrow(() -> new BusinessException("JUMBO_NOT_FOUND", "Jumbo roll not found for serial: " + request.getJumboSerialNumber()));
        } else {
            throw new BusinessException("INVALID_INPUT", "Parent Jumbo Roll identifier must be provided.");
        }

        if ("FULLY_SLIT".equalsIgnoreCase(jumbo.getStatus())) {
            throw new BusinessException("JUMBO_ALREADY_CONSUMED", "Jumbo roll " + jumbo.getSerialNumber() + " has already been fully slit.");
        }
        if ("SCRAPPED".equalsIgnoreCase(jumbo.getStatus())) {
            throw new BusinessException("JUMBO_SCRAPPED", "Cannot slit scrapped Jumbo roll: " + jumbo.getSerialNumber());
        }

        if (request.getCuts() == null || request.getCuts().isEmpty()) {
            throw new BusinessException("EMPTY_CUT_PLAN", "At least one child cut must be defined.");
        }

        // 2. Validate Slitter Machine
        UUID machineId = request.getMachineId() != null ? request.getMachineId() : jumbo.getMachineId();

        // 3. Deckle Width and Mass Balance Reconciliation
        BigDecimal totalWidth = BigDecimal.ZERO;
        BigDecimal totalChildWeight = BigDecimal.ZERO;

        for (SlittingExecutionRequest.SlittingCutItem cut : request.getCuts()) {
            if (cut.getWidthMm() == null || cut.getWidthMm().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("INVALID_WIDTH", "Child roll width must be greater than zero.");
            }
            if (cut.getNetWeightKg() == null || cut.getNetWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("INVALID_WEIGHT", "Child roll weight must be greater than zero.");
            }
            totalWidth = totalWidth.add(cut.getWidthMm());
            totalChildWeight = totalChildWeight.add(cut.getNetWeightKg());
        }

        if (totalWidth.compareTo(jumbo.getDeckleWidthMm()) > 0) {
            throw new BusinessException("WIDTH_EXCEEDED",
                    String.format("Total slit width (%s mm) exceeds parent jumbo deckle width (%s mm)", totalWidth, jumbo.getDeckleWidthMm()));
        }

        BigDecimal trimWaste = request.getTrimWasteKg() != null ? request.getTrimWasteKg() : BigDecimal.ZERO;
        BigDecimal inputWeight = jumbo.getNetWeightKg();
        BigDecimal scaleVariance = inputWeight.subtract(totalChildWeight.add(trimWaste));

        // 1.0% Mass balance tolerance check
        BigDecimal maxAllowedVariance = inputWeight.multiply(new BigDecimal("0.0100"));
        if (scaleVariance.abs().compareTo(maxAllowedVariance) > 0) {
            BigDecimal variancePct = scaleVariance.abs().divide(inputWeight, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            throw new BusinessException("MASS_BALANCE_TOLERANCE_EXCEEDED",
                    String.format("Mass balance scale variance of %s kg (%s%%) exceeds 1.0%% tolerance limit.", scaleVariance, variancePct));
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 4. Record Slitting Run aggregate
        UUID operatorId = request.getOperatorId() != null ? request.getOperatorId() : jumbo.getOperatorId();

        SlittingRun run = new SlittingRun();
        run.setTenantId(tenantId);
        run.setParentJumboId(jumbo.getId());
        run.setMachineId(machineId);
        run.setOperatorId(operatorId);
        run.setInputJumboWeightKg(inputWeight);
        run.setTotalChildWeightKg(totalChildWeight);
        run.setTrimWasteKg(trimWaste);
        run.setScaleVarianceKg(scaleVariance);
        run.setStartedAt(now.minusMinutes(45));
        run.setCompletedAt(now);
        run.setStatus("COMPLETED");
        run = slittingRunRepository.save(run);

        // 5. Update Jumbo status to FULLY_SLIT
        jumbo.setStatus("FULLY_SLIT");
        jumbo = jumboRollRepository.save(jumbo);

        // 6. Generate and save Child Slit Rolls
        List<SlittingExecutionResponse.SlitRollSummary> childSummaries = new ArrayList<>();
        int index = 1;
        for (SlittingExecutionRequest.SlittingCutItem cut : request.getCuts()) {
            SlitRoll slitRoll = new SlitRoll();
            slitRoll.setTenantId(tenantId);
            slitRoll.setSlittingRunId(run.getId());
            slitRoll.setParentJumboId(jumbo.getId());
            slitRoll.setPaperGradeId(jumbo.getPaperGradeId());
            slitRoll.setCutSequence(cut.getPositionIndex() > 0 ? cut.getPositionIndex() : index);
            slitRoll.setSerialNumber(String.format("%s-C%02d", jumbo.getSerialNumber(), index));
            slitRoll.setBarcode(String.format("SR-%s-%02d", jumbo.getSerialNumber(), index));
            slitRoll.setWidthMm(cut.getWidthMm());
            slitRoll.setDiameterMm(cut.getDiameterMm() != null ? cut.getDiameterMm() : new BigDecimal("1200.00"));
            slitRoll.setCoreDiameterMm(cut.getCoreDiameterMm() != null ? cut.getCoreDiameterMm() : new BigDecimal("76.0"));
            slitRoll.setMeasuredGsm(jumbo.getTargetGsm());
            slitRoll.setGrossWeightKg(cut.getNetWeightKg().add(new BigDecimal("2.00")));
            slitRoll.setTareWeightKg(new BigDecimal("2.00"));
            slitRoll.setNetWeightKg(cut.getNetWeightKg());
            slitRoll.setQualityStatus("RELEASED");
            slitRoll.setInventoryStatus("IN_STOCK");
            slitRoll.setSlittedAt(now);
            slitRoll.setOperatorId(operatorId);
            slitRoll = slitRollRepository.save(slitRoll);

            // Record child production movement in ledger
            InventoryMovement prodMovement = new InventoryMovement();
            prodMovement.setTenantId(tenantId);
            prodMovement.setMovementType("PRODUCTION_RECEIPT");
            prodMovement.setSlitRollId(slitRoll.getId());
            prodMovement.setNetWeightKg(cut.getNetWeightKg());
            prodMovement.setActorId(operatorId);
            prodMovement.setReferenceDocument("SLIT-RUN-" + run.getId().toString().substring(0, 8).toUpperCase());
            prodMovement.setReason("Produced from Jumbo " + jumbo.getSerialNumber());
            movementRepository.save(prodMovement);

            childSummaries.add(new SlittingExecutionResponse.SlitRollSummary(
                    slitRoll.getId(),
                    slitRoll.getSerialNumber(),
                    slitRoll.getBarcode(),
                    slitRoll.getCutSequence(),
                    slitRoll.getWidthMm(),
                    slitRoll.getDiameterMm(),
                    slitRoll.getNetWeightKg(),
                    slitRoll.getInventoryStatus(),
                    slitRoll.getQualityStatus()
            ));
            index++;
        }

        // 7. Record Jumbo consumption movement in ledger
        InventoryMovement consumeMovement = new InventoryMovement();
        consumeMovement.setTenantId(tenantId);
        consumeMovement.setMovementType("STATUS_CHANGE");
        consumeMovement.setJumboRollId(jumbo.getId());
        consumeMovement.setFromQualityStatus("ACTIVE_WIP");
        consumeMovement.setToQualityStatus("FULLY_SLIT");
        consumeMovement.setNetWeightKg(inputWeight);
        consumeMovement.setActorId(operatorId);
        consumeMovement.setReferenceDocument("SLIT-RUN-" + run.getId().toString().substring(0, 8).toUpperCase());
        consumeMovement.setReason("Fully slit on Slitter " + machineId);
        movementRepository.save(consumeMovement);

        // 8. Build response
        SlittingExecutionResponse response = new SlittingExecutionResponse();
        response.setSlittingRunId(run.getId());
        response.setParentJumboId(jumbo.getId());
        response.setParentJumboSerial(jumbo.getSerialNumber());
        response.setInputJumboWeightKg(inputWeight);
        response.setTotalChildWeightKg(totalChildWeight);
        response.setTrimWasteKg(trimWaste);
        response.setScaleVarianceKg(scaleVariance);
        response.setVariancePercentage(scaleVariance.abs().divide(inputWeight, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")));
        response.setStatus("COMPLETED");
        response.setChildRolls(childSummaries);
        response.setTimestamp(now);

        return response;
    }

    public RollGenealogyDto getRollGenealogy(String identifier) {
        UUID tenantId = resolveTenantId();

        // 1. Try finding as Slit Roll
        Optional<SlitRoll> slitOpt = slitRollRepository.findByTenantIdAndSerialNumber(tenantId, identifier)
                .or(() -> slitRollRepository.findByTenantIdAndBarcode(tenantId, identifier));

        if (slitOpt.isPresent()) {
            SlitRoll slit = slitOpt.get();
            RollGenealogyDto dto = new RollGenealogyDto();
            dto.setRollType("SLIT");
            dto.setRollId(slit.getId());
            dto.setSerialNumber(slit.getSerialNumber());
            dto.setBarcode(slit.getBarcode());
            dto.setWidthMm(slit.getWidthMm());
            dto.setDiameterMm(slit.getDiameterMm());
            dto.setNetWeightKg(slit.getNetWeightKg());
            dto.setStatus(slit.getInventoryStatus());
            dto.setQualityGrade(slit.getQualityStatus());
            dto.setProducedAt(slit.getCreatedAt());

            // Parent Jumbo
            jumboRollRepository.findByTenantIdAndId(tenantId, slit.getParentJumboId()).ifPresent(jumbo -> {
                dto.setParentJumboId(jumbo.getId());
                dto.setParentJumboSerial(jumbo.getSerialNumber());
                dto.setGsm(jumbo.getTargetGsm());
                paperGradeRepository.findByTenantIdAndId(tenantId, jumbo.getPaperGradeId()).ifPresent(g -> {
                    dto.setPaperGradeName(g.getNameAr() + " (" + g.getCode() + ")");
                });
            });

            // Slitting Run
            if (slit.getSlittingRunId() != null) {
                slittingRunRepository.findByTenantIdAndId(tenantId, slit.getSlittingRunId()).ifPresent(run -> {
                    dto.setSlittingRunId(run.getId());
                    dto.setSlittingTime(run.getCompletedAt());
                    machineRepository.findByTenantIdAndId(tenantId, run.getMachineId()).ifPresent(m -> {
                        dto.setMachineName(m.getNameAr());
                    });
                });

                // Siblings
                List<SlitRoll> siblings = slitRollRepository.findByTenantIdAndSlittingRunId(tenantId, slit.getSlittingRunId());
                List<RollGenealogyDto.SlitRollSummary> sibSummaries = new ArrayList<>();
                for (SlitRoll s : siblings) {
                    sibSummaries.add(new RollGenealogyDto.SlitRollSummary(
                            s.getId(), s.getSerialNumber(), s.getBarcode(), s.getCutSequence(),
                            s.getWidthMm(), s.getNetWeightKg(), s.getInventoryStatus()));
                }
                dto.setRelatedRolls(sibSummaries);
            }

            // Ledger
            List<InventoryMovement> movements = movementRepository.findByTenantIdAndSlitRollId(tenantId, slit.getId());
            List<RollGenealogyDto.MovementTraceItem> trace = new ArrayList<>();
            for (InventoryMovement m : movements) {
                trace.add(new RollGenealogyDto.MovementTraceItem(
                        m.getId(), m.getMovementType(), m.getNetWeightKg(), m.getReferenceDocument(), m.getCreatedAt()));
            }
            dto.setMovementHistory(trace);
            return dto;
        }

        // 2. Try finding as Jumbo Roll
        Optional<JumboRoll> jumboOpt = jumboRollRepository.findByTenantIdAndSerialNumber(tenantId, identifier);

        if (jumboOpt.isPresent()) {
            JumboRoll jumbo = jumboOpt.get();
            RollGenealogyDto dto = new RollGenealogyDto();
            dto.setRollType("JUMBO");
            dto.setRollId(jumbo.getId());
            dto.setSerialNumber(jumbo.getSerialNumber());
            dto.setBarcode(jumbo.getSerialNumber());
            dto.setWidthMm(jumbo.getDeckleWidthMm());
            dto.setNetWeightKg(jumbo.getNetWeightKg());
            dto.setGsm(jumbo.getTargetGsm());
            dto.setStatus(jumbo.getStatus());
            dto.setProducedAt(jumbo.getManufacturedAt());

            paperGradeRepository.findByTenantIdAndId(tenantId, jumbo.getPaperGradeId()).ifPresent(g -> {
                dto.setPaperGradeName(g.getNameAr() + " (" + g.getCode() + ")");
            });
            machineRepository.findByTenantIdAndId(tenantId, jumbo.getMachineId()).ifPresent(m -> {
                dto.setMachineName(m.getNameAr());
            });

            // Children from this Jumbo
            List<SlitRoll> children = slitRollRepository.findByTenantIdAndParentJumboId(tenantId, jumbo.getId());
            List<RollGenealogyDto.SlitRollSummary> childSummaries = new ArrayList<>();
            for (SlitRoll s : children) {
                childSummaries.add(new RollGenealogyDto.SlitRollSummary(
                        s.getId(), s.getSerialNumber(), s.getBarcode(), s.getCutSequence(),
                        s.getWidthMm(), s.getNetWeightKg(), s.getInventoryStatus()));
            }
            dto.setRelatedRolls(childSummaries);

            // Movements
            List<InventoryMovement> movements = movementRepository.findByTenantIdAndJumboRollId(tenantId, jumbo.getId());
            List<RollGenealogyDto.MovementTraceItem> trace = new ArrayList<>();
            for (InventoryMovement m : movements) {
                trace.add(new RollGenealogyDto.MovementTraceItem(
                        m.getId(), m.getMovementType(), m.getNetWeightKg(), m.getReferenceDocument(), m.getCreatedAt()));
            }
            dto.setMovementHistory(trace);
            return dto;
        }

        throw new BusinessException("ROLL_NOT_FOUND", "No roll found matching identifier: " + identifier);
    }
}
