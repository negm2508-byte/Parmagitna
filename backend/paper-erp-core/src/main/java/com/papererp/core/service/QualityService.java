package com.papererp.core.service;

import com.papererp.common.context.TenantContext;
import com.papererp.common.domain.QualityDisposition;
import com.papererp.common.domain.RollType;
import com.papererp.common.exception.DomainException;
import com.papererp.common.exception.EntityNotFoundException;
import com.papererp.core.domain.JumboRollEntity;
import com.papererp.core.domain.QualityInspectionEntity;
import com.papererp.core.domain.SlitRollEntity;
import com.papererp.core.dto.QualityDto;
import com.papererp.core.repository.JumboRollRepository;
import com.papererp.core.repository.QualityInspectionRepository;
import com.papererp.core.repository.SlitRollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class QualityService {

    private final QualityInspectionRepository qualityInspectionRepository;
    private final JumboRollRepository jumboRollRepository;
    private final SlitRollRepository slitRollRepository;
    private final AuditService auditService;
    private final FeatureFlagService featureFlagService;

    public QualityService(QualityInspectionRepository qualityInspectionRepository,
                          JumboRollRepository jumboRollRepository,
                          SlitRollRepository slitRollRepository,
                          AuditService auditService,
                          FeatureFlagService featureFlagService) {
        this.qualityInspectionRepository = qualityInspectionRepository;
        this.jumboRollRepository = jumboRollRepository;
        this.slitRollRepository = slitRollRepository;
        this.auditService = auditService;
        this.featureFlagService = featureFlagService;
    }

    @Transactional
    public QualityDto.QualityInspectionDto recordInspection(QualityDto.CreateInspectionRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID inspectorId = TenantContext.getRequiredUserId();

        // Exactly one target roll must be provided
        if ((request.jumboRollId() == null && request.slitRollId() == null) ||
                (request.jumboRollId() != null && request.slitRollId() != null)) {
            throw new DomainException("INVALID_INSPECTION_TARGET",
                    "Exactly one roll target (jumboRollId XOR slitRollId) must be provided", 422);
        }

        if (request.rollType() == RollType.JUMBO_ROLL) {
            JumboRollEntity jumbo = jumboRollRepository.findByTenantIdAndId(tenantId, request.jumboRollId())
                    .orElseThrow(() -> new EntityNotFoundException("JumboRoll", request.jumboRollId()));
            if (featureFlagService.isMakerCheckerEnforced() && inspectorId.equals(jumbo.getCreatedBy())) {
                throw new DomainException("MAKER_CHECKER_VIOLATION",
                        "Separation of duties: Inspector cannot inspect a roll they created.", 403);
            }
            jumbo.setQualityDisposition(request.disposition());
            jumboRollRepository.save(jumbo);
        } else {
            SlitRollEntity slit = slitRollRepository.findByTenantIdAndId(tenantId, request.slitRollId())
                    .orElseThrow(() -> new EntityNotFoundException("SlitRoll", request.slitRollId()));
            if (featureFlagService.isMakerCheckerEnforced() && inspectorId.equals(slit.getCreatedBy())) {
                throw new DomainException("MAKER_CHECKER_VIOLATION",
                        "Separation of duties: Inspector cannot inspect a roll they created.", 403);
            }
            slit.setQualityDisposition(request.disposition());
            slitRollRepository.save(slit);
        }

        QualityInspectionEntity inspection = new QualityInspectionEntity(
                tenantId, request.plantId(), request.rollType(), request.jumboRollId(),
                request.slitRollId(), inspectorId, request.measuredGsm(), request.measuredMoisturePct(),
                request.measuredCaliperUm(), request.measuredBurstStrength(), request.disposition(),
                request.notes()
        );
        QualityInspectionEntity saved = qualityInspectionRepository.save(inspection);

        auditService.recordActionInTransaction(
                tenantId, inspectorId, "QUALITY_INSPECTION", "QUALITY_INSPECTION",
                saved.getId(), null,
                String.format("{\"disposition\":\"%s\",\"rollType\":\"%s\"}", request.disposition(), request.rollType())
        );

        return mapToDto(saved);
    }

    @Transactional
    public QualityDto.QualityInspectionDto updateDisposition(UUID inspectionId, QualityDto.UpdateDispositionRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID userId = TenantContext.getRequiredUserId();

        QualityInspectionEntity inspection = qualityInspectionRepository.findByTenantIdAndId(tenantId, inspectionId)
                .orElseThrow(() -> new EntityNotFoundException("QualityInspection", inspectionId));

        inspection.setDisposition(request.disposition());
        inspection.setNotes(inspection.getNotes() != null
                ? inspection.getNotes() + " | Updated: " + request.reason()
                : "Updated: " + request.reason());
        qualityInspectionRepository.save(inspection);

        if (inspection.getRollType() == RollType.JUMBO_ROLL && inspection.getJumboRollId() != null) {
            jumboRollRepository.findByTenantIdAndId(tenantId, inspection.getJumboRollId()).ifPresent(j -> {
                j.setQualityDisposition(request.disposition());
                jumboRollRepository.save(j);
            });
        } else if (inspection.getSlitRollId() != null) {
            slitRollRepository.findByTenantIdAndId(tenantId, inspection.getSlitRollId()).ifPresent(s -> {
                s.setQualityDisposition(request.disposition());
                slitRollRepository.save(s);
            });
        }

        auditService.recordActionInTransaction(
                tenantId, userId, "UPDATE_DISPOSITION", "QUALITY_INSPECTION",
                inspection.getId(), null,
                String.format("{\"newDisposition\":\"%s\",\"reason\":\"%s\"}", request.disposition(), request.reason())
        );

        return mapToDto(inspection);
    }

    @Transactional(readOnly = true)
    public QualityDto.QualityInspectionDto getInspectionById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return qualityInspectionRepository.findByTenantIdAndId(tenantId, id)
                .map(this::mapToDto)
                .orElseThrow(() -> new EntityNotFoundException("QualityInspection", id));
    }

    public QualityDto.QualityInspectionDto mapToDto(QualityInspectionEntity q) {
        return new QualityDto.QualityInspectionDto(
                q.getId(), q.getTenantId(), q.getPlantId(), q.getRollType(), q.getJumboRollId(),
                q.getSlitRollId(), q.getInspectorId(), q.getMeasuredGsm(), q.getMeasuredMoisturePct(),
                q.getMeasuredCaliperUm(), q.getMeasuredBurstStrength(), q.getDisposition(),
                q.getNotes(), q.getInspectionTimestamp()
        );
    }
}
