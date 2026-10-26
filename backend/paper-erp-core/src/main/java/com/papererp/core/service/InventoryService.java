package com.papererp.core.service;

import com.papererp.common.context.TenantContext;
import com.papererp.common.domain.*;
import com.papererp.common.dto.PageResponse;
import com.papererp.common.exception.DomainException;
import com.papererp.common.exception.EntityNotFoundException;
import com.papererp.common.exception.IneligibleRollException;
import com.papererp.core.domain.InventoryLedgerEntity;
import com.papererp.core.domain.JumboRollEntity;
import com.papererp.core.domain.SlitRollEntity;
import com.papererp.core.dto.InventoryDto;
import com.papererp.core.dto.QualityDto;
import com.papererp.core.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final JumboRollRepository jumboRollRepository;
    private final SlitRollRepository slitRollRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final QualityInspectionRepository qualityInspectionRepository;
    private final AuditService auditService;

    public InventoryService(InventoryLedgerRepository inventoryLedgerRepository,
                            JumboRollRepository jumboRollRepository,
                            SlitRollRepository slitRollRepository,
                            WarehouseLocationRepository warehouseLocationRepository,
                            QualityInspectionRepository qualityInspectionRepository,
                            AuditService auditService) {
        this.inventoryLedgerRepository = inventoryLedgerRepository;
        this.jumboRollRepository = jumboRollRepository;
        this.slitRollRepository = slitRollRepository;
        this.warehouseLocationRepository = warehouseLocationRepository;
        this.qualityInspectionRepository = qualityInspectionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public InventoryDto.InventoryLedgerDto transferLocation(InventoryDto.TransferLocationRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID actorId = TenantContext.getRequiredUserId();

        // Validate destination location exists within tenant
        if (!warehouseLocationRepository.findByTenantIdAndId(tenantId, request.destinationLocationId()).isPresent()) {
            throw new EntityNotFoundException("WarehouseLocation", request.destinationLocationId());
        }

        InventoryLedgerEntity ledgerEntry;

        if (request.rollType() == RollType.JUMBO_ROLL) {
            if (request.jumboRollId() == null) {
                throw new DomainException("MISSING_TARGET_ID", "jumboRollId must be specified for JUMBO_ROLL transfer", 422);
            }
            JumboRollEntity jumbo = jumboRollRepository.findByTenantIdAndId(tenantId, request.jumboRollId())
                    .orElseThrow(() -> new EntityNotFoundException("JumboRoll", request.jumboRollId()));

            UUID sourceLocation = jumbo.getLocationId();
            jumbo.setLocationId(request.destinationLocationId());
            jumboRollRepository.save(jumbo);

            ledgerEntry = InventoryLedgerEntity.forJumbo(
                    tenantId, jumbo.getPlantId(), InventoryMovementType.LOCATION_TRANSFER,
                    jumbo.getId(), sourceLocation, request.destinationLocationId(),
                    BigDecimal.ZERO, jumbo.getNetWeightKg(), actorId, "TRANSFER", null, request.reason()
            );
        } else {
            if (request.slitRollId() == null) {
                throw new DomainException("MISSING_TARGET_ID", "slitRollId must be specified for SLIT_ROLL transfer", 422);
            }
            SlitRollEntity slit = slitRollRepository.findByTenantIdAndId(tenantId, request.slitRollId())
                    .orElseThrow(() -> new EntityNotFoundException("SlitRoll", request.slitRollId()));

            UUID sourceLocation = slit.getLocationId();
            slit.setLocationId(request.destinationLocationId());
            slitRollRepository.save(slit);

            ledgerEntry = InventoryLedgerEntity.forSlit(
                    tenantId, slit.getPlantId(), InventoryMovementType.LOCATION_TRANSFER,
                    slit.getId(), sourceLocation, request.destinationLocationId(),
                    BigDecimal.ZERO, slit.getNetWeightKg(), actorId, "TRANSFER", null, request.reason()
            );
        }

        InventoryLedgerEntity saved = inventoryLedgerRepository.save(ledgerEntry);

        auditService.recordActionInTransaction(
                tenantId, actorId, "LOCATION_TRANSFER", "INVENTORY_LEDGER",
                saved.getId(), null, String.format("{\"destination\":\"%s\"}", request.destinationLocationId())
        );

        return mapLedgerToDto(saved);
    }

    @Transactional
    public InventoryDto.InventoryLedgerDto dispatchRoll(InventoryDto.DispatchRollRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID actorId = TenantContext.getRequiredUserId();

        InventoryLedgerEntity ledgerEntry;

        if (request.rollType() == RollType.JUMBO_ROLL) {
            if (request.jumboRollId() == null) {
                throw new DomainException("MISSING_TARGET_ID", "jumboRollId must be specified for JUMBO_ROLL dispatch", 422);
            }
            JumboRollEntity jumbo = jumboRollRepository.findByTenantIdAndId(tenantId, request.jumboRollId())
                    .orElseThrow(() -> new EntityNotFoundException("JumboRoll", request.jumboRollId()));

            // Quality Gate Check: Roll must be RELEASED
            if (!jumbo.getQualityDisposition().isEligibleForDispatch()) {
                throw new IneligibleRollException(String.format(
                        "Jumbo Roll %s with quality disposition '%s' cannot be dispatched. Must be RELEASED or DOWNGRADED.",
                        jumbo.getSerialNumber(), jumbo.getQualityDisposition()
                ));
            }

            if (!jumbo.getStatus().isEligibleForDispatch()) {
                throw new IneligibleRollException(String.format(
                        "Jumbo Roll %s in state '%s' cannot be dispatched. Must be AVAILABLE.",
                        jumbo.getSerialNumber(), jumbo.getStatus()
                ));
            }

            UUID sourceLoc = jumbo.getLocationId();
            jumbo.setStatus(RollStatus.DISPATCHED);
            jumboRollRepository.save(jumbo);

            ledgerEntry = InventoryLedgerEntity.forJumbo(
                    tenantId, jumbo.getPlantId(), InventoryMovementType.DISPATCH,
                    jumbo.getId(), sourceLoc, null,
                    new BigDecimal("-1.00"), jumbo.getNetWeightKg(), actorId, "DISPATCH", null,
                    request.customerReference() != null ? "Customer Ref: " + request.customerReference() : "Dispatch"
            );
        } else {
            if (request.slitRollId() == null) {
                throw new DomainException("MISSING_TARGET_ID", "slitRollId must be specified for SLIT_ROLL dispatch", 422);
            }
            SlitRollEntity slit = slitRollRepository.findByTenantIdAndId(tenantId, request.slitRollId())
                    .orElseThrow(() -> new EntityNotFoundException("SlitRoll", request.slitRollId()));

            // Quality Gate Check: Roll must be RELEASED
            if (!slit.getQualityDisposition().isEligibleForDispatch()) {
                throw new IneligibleRollException(String.format(
                        "Slit Roll %s with quality disposition '%s' cannot be dispatched. Must be RELEASED or DOWNGRADED.",
                        slit.getSerialNumber(), slit.getQualityDisposition()
                ));
            }

            if (!slit.getStatus().isEligibleForDispatch()) {
                throw new IneligibleRollException(String.format(
                        "Slit Roll %s in state '%s' cannot be dispatched. Must be AVAILABLE.",
                        slit.getSerialNumber(), slit.getStatus()
                ));
            }

            UUID sourceLoc = slit.getLocationId();
            slit.setStatus(RollStatus.DISPATCHED);
            slitRollRepository.save(slit);

            ledgerEntry = InventoryLedgerEntity.forSlit(
                    tenantId, slit.getPlantId(), InventoryMovementType.DISPATCH,
                    slit.getId(), sourceLoc, null,
                    new BigDecimal("-1.00"), slit.getNetWeightKg(), actorId, "DISPATCH", null,
                    request.customerReference() != null ? "Customer Ref: " + request.customerReference() : "Dispatch"
            );
        }

        InventoryLedgerEntity saved = inventoryLedgerRepository.save(ledgerEntry);

        auditService.recordActionInTransaction(
                tenantId, actorId, "DISPATCH_ROLL", "INVENTORY_LEDGER",
                saved.getId(), null, String.format("{\"customerRef\":\"%s\"}", request.customerReference())
        );

        return mapLedgerToDto(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryDto.InventoryLedgerDto> getLedger(UUID plantId, int page, int size) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Pageable pageable = PageRequest.of(page, size);
        Page<InventoryLedgerEntity> pageResult = inventoryLedgerRepository.findAllByTenantIdAndPlantIdOrderByMovementTimestampDesc(
                tenantId, plantId, pageable
        );
        var dtoList = pageResult.getContent().stream().map(this::mapLedgerToDto).toList();
        return PageResponse.of(dtoList, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public InventoryDto.RollTraceabilityDto lookupRoll(String serialOrBarcode) {
        UUID tenantId = TenantContext.getRequiredTenantId();

        // Search Jumbo Rolls first
        Optional<JumboRollEntity> jumboOpt = jumboRollRepository.findByTenantIdAndSerialNumber(tenantId, serialOrBarcode)
                .or(() -> jumboRollRepository.findByTenantIdAndBarcode(tenantId, serialOrBarcode));

        if (jumboOpt.isPresent()) {
            JumboRollEntity j = jumboOpt.get();
            var movements = inventoryLedgerRepository.findAllByTenantIdAndJumboRollIdOrderByMovementTimestampDesc(tenantId, j.getId())
                    .stream().map(this::mapLedgerToDto).toList();
            var inspections = qualityInspectionRepository.findAllByTenantIdAndJumboRollIdOrderByInspectionTimestampDesc(tenantId, j.getId())
                    .stream().map(this::mapInspectionToDto).toList();

            return new InventoryDto.RollTraceabilityDto(
                    RollType.JUMBO_ROLL, j.getId(), j.getSerialNumber(), j.getBarcode(),
                    j.getStatus(), j.getQualityDisposition(), j.getPlantId(), j.getGradeId(),
                    j.getGrossWeightKg(), j.getTareWeightKg(), j.getNetWeightKg(), null, null,
                    movements, inspections
            );
        }

        // Search Slit Rolls
        Optional<SlitRollEntity> slitOpt = slitRollRepository.findByTenantIdAndSerialNumber(tenantId, serialOrBarcode)
                .or(() -> slitRollRepository.findByTenantIdAndBarcode(tenantId, serialOrBarcode));

        if (slitOpt.isPresent()) {
            SlitRollEntity s = slitOpt.get();
            var movements = inventoryLedgerRepository.findAllByTenantIdAndSlitRollIdOrderByMovementTimestampDesc(tenantId, s.getId())
                    .stream().map(this::mapLedgerToDto).toList();
            var inspections = qualityInspectionRepository.findAllByTenantIdAndSlitRollIdOrderByInspectionTimestampDesc(tenantId, s.getId())
                    .stream().map(this::mapInspectionToDto).toList();

            return new InventoryDto.RollTraceabilityDto(
                    RollType.SLIT_ROLL, s.getId(), s.getSerialNumber(), s.getBarcode(),
                    s.getStatus(), s.getQualityDisposition(), s.getPlantId(), s.getGradeId(),
                    s.getGrossWeightKg(), s.getTareWeightKg(), s.getNetWeightKg(),
                    s.getParentJumboId(), s.getSlittingRunId(), movements, inspections
            );
        }

        throw new EntityNotFoundException("Roll with serial or barcode", serialOrBarcode);
    }

    @Transactional(readOnly = true)
    public InventoryDto.RollReconciliationDto reconcileRoll(RollType rollType, UUID rollId) {
        UUID tenantId = TenantContext.getRequiredTenantId();

        if (rollType == RollType.JUMBO_ROLL) {
            JumboRollEntity j = jumboRollRepository.findByTenantIdAndId(tenantId, rollId)
                    .orElseThrow(() -> new EntityNotFoundException("JumboRoll", rollId));
            BigDecimal ledgerSum = inventoryLedgerRepository.sumQuantityByJumboRollId(tenantId, rollId);

            // An AVAILABLE jumbo has net ledger balance of +1. FULLY_SLIT or DISPATCHED has balance 0.
            boolean balanced;
            if (j.getStatus() == RollStatus.AVAILABLE) {
                balanced = ledgerSum.compareTo(BigDecimal.ONE) == 0;
            } else {
                balanced = ledgerSum.compareTo(BigDecimal.ZERO) == 0;
            }

            return new InventoryDto.RollReconciliationDto(
                    RollType.JUMBO_ROLL, j.getId(), j.getSerialNumber(), j.getStatus(),
                    j.getNetWeightKg(), ledgerSum, balanced,
                    balanced ? "Ledger matches current roll state" : "Discrepancy detected between ledger movements and roll status"
            );
        } else {
            SlitRollEntity s = slitRollRepository.findByTenantIdAndId(tenantId, rollId)
                    .orElseThrow(() -> new EntityNotFoundException("SlitRoll", rollId));
            BigDecimal ledgerSum = inventoryLedgerRepository.sumQuantityBySlitRollId(tenantId, rollId);

            boolean balanced;
            if (s.getStatus() == RollStatus.AVAILABLE) {
                balanced = ledgerSum.compareTo(BigDecimal.ONE) == 0;
            } else {
                balanced = ledgerSum.compareTo(BigDecimal.ZERO) == 0;
            }

            return new InventoryDto.RollReconciliationDto(
                    RollType.SLIT_ROLL, s.getId(), s.getSerialNumber(), s.getStatus(),
                    s.getNetWeightKg(), ledgerSum, balanced,
                    balanced ? "Ledger matches current roll state" : "Discrepancy detected between ledger movements and roll status"
            );
        }
    }

    private InventoryDto.InventoryLedgerDto mapLedgerToDto(InventoryLedgerEntity l) {
        return new InventoryDto.InventoryLedgerDto(
                l.getId(), l.getTenantId(), l.getPlantId(), l.getMovementType(), l.getRollType(),
                l.getJumboRollId(), l.getSlitRollId(), l.getSourceLocationId(), l.getDestinationLocationId(),
                l.getQuantityChange(), l.getWeightKg(), l.getActorId(), l.getReferenceType(),
                l.getReferenceId(), l.getReason(), l.getMovementTimestamp()
        );
    }

    private QualityDto.QualityInspectionDto mapInspectionToDto(com.papererp.core.domain.QualityInspectionEntity q) {
        return new QualityDto.QualityInspectionDto(
                q.getId(), q.getTenantId(), q.getPlantId(), q.getRollType(), q.getJumboRollId(),
                q.getSlitRollId(), q.getInspectorId(), q.getMeasuredGsm(), q.getMeasuredMoisturePct(),
                q.getMeasuredCaliperUm(), q.getMeasuredBurstStrength(), q.getDisposition(),
                q.getNotes(), q.getInspectionTimestamp()
        );
    }
}
