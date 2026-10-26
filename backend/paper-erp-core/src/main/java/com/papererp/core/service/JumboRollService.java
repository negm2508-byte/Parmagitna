package com.papererp.core.service;

import com.papererp.common.context.TenantContext;
import com.papererp.common.domain.InventoryMovementType;
import com.papererp.common.domain.RollStatus;
import com.papererp.common.dto.PageResponse;
import com.papererp.common.exception.DomainException;
import com.papererp.common.exception.EntityNotFoundException;
import com.papererp.core.domain.InventoryLedgerEntity;
import com.papererp.core.domain.JumboRollEntity;
import com.papererp.core.dto.ProductionDto;
import com.papererp.core.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class JumboRollService {

    private final JumboRollRepository jumboRollRepository;
    private final PlantRepository plantRepository;
    private final MachineRepository machineRepository;
    private final PaperGradeRepository paperGradeRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final AuditService auditService;

    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    public JumboRollService(JumboRollRepository jumboRollRepository,
                            PlantRepository plantRepository,
                            MachineRepository machineRepository,
                            PaperGradeRepository paperGradeRepository,
                            InventoryLedgerRepository inventoryLedgerRepository,
                            AuditService auditService) {
        this.jumboRollRepository = jumboRollRepository;
        this.plantRepository = plantRepository;
        this.machineRepository = machineRepository;
        this.paperGradeRepository = paperGradeRepository;
        this.inventoryLedgerRepository = inventoryLedgerRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ProductionDto.JumboRollDto registerJumboRoll(ProductionDto.CreateJumboRollRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID userId = TenantContext.getRequiredUserId();

        // 1. Cross-Tenant validation: Plant, Machine, Grade must belong to authenticated tenant
        if (!plantRepository.existsByTenantIdAndId(tenantId, request.plantId())) {
            throw new EntityNotFoundException("Plant", request.plantId());
        }
        var machine = machineRepository.findByTenantIdAndPlantIdAndId(tenantId, request.plantId(), request.machineId())
                .orElseThrow(() -> new EntityNotFoundException("Machine in Plant", request.machineId()));
        if (!paperGradeRepository.existsByTenantIdAndId(tenantId, request.gradeId())) {
            throw new EntityNotFoundException("PaperGrade", request.gradeId());
        }

        // 2. Validate machine deckle width constraints
        if (request.deckleWidthMm().compareTo(machine.getMaxDeckleWidthMm()) > 0) {
            throw new DomainException("DECKLE_WIDTH_EXCEEDED",
                    String.format("Deckle width %s mm exceeds machine maximum %s mm",
                            request.deckleWidthMm(), machine.getMaxDeckleWidthMm()), 422);
        }

        // 3. Precision weight calculation (No fabricated zero defaults)
        BigDecimal gross = request.grossWeightKg().setScale(2, RoundingMode.HALF_UP);
        BigDecimal tare = request.tareWeightKg().setScale(2, RoundingMode.HALF_UP);
        if (gross.compareTo(tare) <= 0) {
            throw new DomainException("INVALID_WEIGHTS", "Gross weight must be strictly greater than tare weight", 422);
        }
        BigDecimal net = gross.subtract(tare).setScale(2, RoundingMode.HALF_UP);

        // 4. Generate unique serial number & barcode
        String datePart = DATE_PREFIX.format(Instant.now());
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String serialNumber = String.format("JR-%s-%s", datePart, randomSuffix);
        String barcode = String.format("BC%s%s", datePart, randomSuffix);

        // 5. Persist parent jumbo roll
        JumboRollEntity jumbo = new JumboRollEntity(
                tenantId, request.plantId(), request.machineId(), request.gradeId(),
                request.coreSpecId(), serialNumber, barcode, gross, tare, net,
                request.deckleWidthMm().setScale(2, RoundingMode.HALF_UP),
                request.diameterMm() != null ? request.diameterMm().setScale(2, RoundingMode.HALF_UP) : null,
                userId
        );
        jumbo.setLocationId(request.locationId());
        JumboRollEntity savedJumbo = jumboRollRepository.save(jumbo);

        // 6. Record in immutable inventory ledger
        InventoryLedgerEntity ledgerEntry = InventoryLedgerEntity.forJumbo(
                tenantId, request.plantId(), InventoryMovementType.PRODUCTION_RECEIPT,
                savedJumbo.getId(), null, request.locationId(),
                BigDecimal.ONE, net, userId, "PRODUCTION", savedJumbo.getId(),
                "Initial Jumbo Roll Manufacturing Receipt"
        );
        inventoryLedgerRepository.save(ledgerEntry);

        // 7. Record Audit Trail in same transaction
        auditService.recordActionInTransaction(
                tenantId, userId, "REGISTER_JUMBO_ROLL", "JUMBO_ROLL",
                savedJumbo.getId(), null,
                String.format("{\"serial\":\"%s\",\"netWeightKg\":%s}", serialNumber, net)
        );

        return mapToDto(savedJumbo);
    }

    @Transactional(readOnly = true)
    public ProductionDto.JumboRollDto getJumboRollById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return jumboRollRepository.findByTenantIdAndId(tenantId, id)
                .map(this::mapToDto)
                .orElseThrow(() -> new EntityNotFoundException("JumboRoll", id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductionDto.JumboRollDto> listJumboRolls(UUID plantId, RollStatus status, int page, int size) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("productionTimestamp").descending());

        Page<JumboRollEntity> pageResult;
        if (plantId != null && status != null) {
            pageResult = jumboRollRepository.findAllByTenantIdAndPlantIdAndStatus(tenantId, plantId, status, pageable);
        } else if (plantId != null) {
            pageResult = jumboRollRepository.findAllByTenantIdAndPlantId(tenantId, plantId, pageable);
        } else {
            pageResult = jumboRollRepository.findAllByTenantId(tenantId, pageable);
        }

        var dtoList = pageResult.getContent().stream().map(this::mapToDto).toList();
        return PageResponse.of(dtoList, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }

    private ProductionDto.JumboRollDto mapToDto(JumboRollEntity j) {
        return new ProductionDto.JumboRollDto(
                j.getId(), j.getTenantId(), j.getPlantId(), j.getMachineId(), j.getGradeId(),
                j.getCoreSpecId(), j.getLocationId(), j.getSerialNumber(), j.getBarcode(),
                j.getGrossWeightKg(), j.getTareWeightKg(), j.getNetWeightKg(), j.getDeckleWidthMm(),
                j.getDiameterMm(), j.getStatus(), j.getQualityDisposition(), j.getProductionTimestamp(),
                j.getCreatedBy()
        );
    }
}
