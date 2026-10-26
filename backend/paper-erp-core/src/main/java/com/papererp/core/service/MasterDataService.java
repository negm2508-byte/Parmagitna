package com.papererp.core.service;

import com.papererp.common.context.TenantContext;
import com.papererp.common.exception.DomainException;
import com.papererp.common.exception.EntityNotFoundException;
import com.papererp.core.domain.*;
import com.papererp.core.dto.MasterDataDto;
import com.papererp.core.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class MasterDataService {

    private final PlantRepository plantRepository;
    private final MachineRepository machineRepository;
    private final PaperGradeRepository paperGradeRepository;
    private final CoreSpecificationRepository coreSpecificationRepository;
    private final ManufacturingRuleRepository manufacturingRuleRepository;

    public MasterDataService(PlantRepository plantRepository,
                             MachineRepository machineRepository,
                             PaperGradeRepository paperGradeRepository,
                             CoreSpecificationRepository coreSpecificationRepository,
                             ManufacturingRuleRepository manufacturingRuleRepository) {
        this.plantRepository = plantRepository;
        this.machineRepository = machineRepository;
        this.paperGradeRepository = paperGradeRepository;
        this.coreSpecificationRepository = coreSpecificationRepository;
        this.manufacturingRuleRepository = manufacturingRuleRepository;
    }

    // --- Plants ---
    @Transactional(readOnly = true)
    public List<MasterDataDto.PlantDto> getAllPlants() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return plantRepository.findAllByTenantId(tenantId).stream()
                .map(this::mapPlantToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MasterDataDto.PlantDto getPlantById(UUID plantId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return plantRepository.findByTenantIdAndId(tenantId, plantId)
                .map(this::mapPlantToDto)
                .orElseThrow(() -> new EntityNotFoundException("Plant", plantId));
    }

    @Transactional
    public MasterDataDto.PlantDto createPlant(MasterDataDto.CreatePlantRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        if (plantRepository.findByTenantIdAndCode(tenantId, request.code()).isPresent()) {
            throw new DomainException("PLANT_CODE_EXISTS", "Plant code already exists", 409);
        }
        PlantEntity plant = new PlantEntity(tenantId, request.code(), request.nameAr(), request.nameEn());
        return mapPlantToDto(plantRepository.save(plant));
    }

    // --- Machines ---
    @Transactional(readOnly = true)
    public List<MasterDataDto.MachineDto> getAllMachines(UUID plantId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        List<MachineEntity> machines = (plantId != null)
                ? machineRepository.findAllByTenantIdAndPlantId(tenantId, plantId)
                : machineRepository.findAllByTenantId(tenantId);
        return machines.stream().map(this::mapMachineToDto).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "masterDataMachines", key = "T(com.papererp.common.context.TenantContext).getRequiredTenantId().toString() + ':' + #machineId")
    public MasterDataDto.MachineDto getMachineById(UUID machineId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return machineRepository.findByTenantIdAndId(tenantId, machineId)
                .map(this::mapMachineToDto)
                .orElseThrow(() -> new EntityNotFoundException("Machine", machineId));
    }

    @Transactional
    @CacheEvict(value = "masterDataMachines", allEntries = true)
    public MasterDataDto.MachineDto createMachine(MasterDataDto.CreateMachineRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        if (!plantRepository.existsByTenantIdAndId(tenantId, request.plantId())) {
            throw new EntityNotFoundException("Plant", request.plantId());
        }
        if (machineRepository.findByTenantIdAndCode(tenantId, request.code()).isPresent()) {
            throw new DomainException("MACHINE_CODE_EXISTS", "Machine code already exists", 409);
        }
        MachineEntity machine = new MachineEntity(
                tenantId, request.plantId(), request.code(), request.nameAr(),
                request.machineType(), request.maxDeckleWidthMm(), request.minTrimWidthMm()
        );
        return mapMachineToDto(machineRepository.save(machine));
    }

    // --- Paper Grades ---
    @Transactional(readOnly = true)
    public List<MasterDataDto.PaperGradeDto> getAllGrades() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return paperGradeRepository.findAllByTenantId(tenantId).stream()
                .map(this::mapGradeToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "masterDataGrades", key = "T(com.papererp.common.context.TenantContext).getRequiredTenantId().toString() + ':' + #gradeId")
    public MasterDataDto.PaperGradeDto getGradeById(UUID gradeId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return paperGradeRepository.findByTenantIdAndId(tenantId, gradeId)
                .map(this::mapGradeToDto)
                .orElseThrow(() -> new EntityNotFoundException("PaperGrade", gradeId));
    }

    @Transactional
    @CacheEvict(value = "masterDataGrades", allEntries = true)
    public MasterDataDto.PaperGradeDto createGrade(MasterDataDto.CreatePaperGradeRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        if (paperGradeRepository.findByTenantIdAndCode(tenantId, request.code()).isPresent()) {
            throw new DomainException("GRADE_CODE_EXISTS", "Grade code already exists", 409);
        }
        if (request.gsmMin().compareTo(request.gsmTarget()) > 0 || request.gsmTarget().compareTo(request.gsmMax()) > 0) {
            throw new DomainException("INVALID_GSM_RANGE", "GSM min must be <= target <= max", 422);
        }
        PaperGradeEntity grade = new PaperGradeEntity(
                tenantId, request.code(), request.nameAr(), request.gsmTarget(),
                request.gsmMin(), request.gsmMax(), request.caliperTargetUm(),
                request.moistureTargetPct(), request.burstIndexTarget()
        );
        return mapGradeToDto(paperGradeRepository.save(grade));
    }

    // --- Core Specifications & Rules Lookup ---
    @Transactional(readOnly = true)
    @Cacheable(value = "manufacturingRules", key = "#tenantId.toString() + ':' + (#plantId != null ? #plantId.toString() : 'global')")
    public BigDecimal getTolerancePercentageOrDefault(UUID tenantId, UUID plantId) {
        if (plantId != null) {
            var plantRule = manufacturingRuleRepository.findByTenantIdAndPlantIdAndRuleKey(tenantId, plantId, "SLITTING_VARIANCE_TOLERANCE_PCT");
            if (plantRule.isPresent() && plantRule.get().getRuleValueNumeric() != null) {
                return plantRule.get().getRuleValueNumeric();
            }
        }
        var tenantRule = manufacturingRuleRepository.findByTenantIdAndPlantIdIsNullAndRuleKey(tenantId, "SLITTING_VARIANCE_TOLERANCE_PCT");
        if (tenantRule.isPresent() && tenantRule.get().getRuleValueNumeric() != null) {
            return tenantRule.get().getRuleValueNumeric();
        }
        // Configurable factory default standard: 0.50%
        return new BigDecimal("0.50");
    }

    private MasterDataDto.PlantDto mapPlantToDto(PlantEntity p) {
        return new MasterDataDto.PlantDto(p.getId(), p.getTenantId(), p.getCode(), p.getNameAr(), p.getNameEn(), p.isActive());
    }

    private MasterDataDto.MachineDto mapMachineToDto(MachineEntity m) {
        return new MasterDataDto.MachineDto(
                m.getId(), m.getTenantId(), m.getPlantId(), m.getCode(), m.getNameAr(),
                m.getMachineType(), m.getMaxDeckleWidthMm(), m.getMinTrimWidthMm(), m.isActive()
        );
    }

    private MasterDataDto.PaperGradeDto mapGradeToDto(PaperGradeEntity g) {
        return new MasterDataDto.PaperGradeDto(
                g.getId(), g.getTenantId(), g.getCode(), g.getNameAr(), g.getGsmTarget(),
                g.getGsmMin(), g.getGsmMax(), g.getCaliperTargetUm(), g.getMoistureTargetPct(),
                g.getBurstIndexTarget(), g.isActive()
        );
    }
}
