package com.papererp.core.service;

import com.papererp.common.context.TenantContext;
import com.papererp.common.context.TenantDetails;
import com.papererp.common.domain.QualityDisposition;
import com.papererp.common.domain.RollType;
import com.papererp.common.exception.DomainException;
import com.papererp.common.security.SystemRole;
import com.papererp.core.domain.JumboRollEntity;
import com.papererp.core.domain.QualityInspectionEntity;
import com.papererp.core.dto.QualityDto;
import com.papererp.core.repository.JumboRollRepository;
import com.papererp.core.repository.QualityInspectionRepository;
import com.papererp.core.repository.SlitRollRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QualityService Unit Tests - Inspection & Separation of Duties")
class QualityServiceTest {

    @Mock
    private QualityInspectionRepository qualityInspectionRepository;
    @Mock
    private JumboRollRepository jumboRollRepository;
    @Mock
    private SlitRollRepository slitRollRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private FeatureFlagService featureFlagService;

    @InjectMocks
    private QualityService qualityService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID plantId = UUID.randomUUID();
    private final UUID inspectorId = UUID.randomUUID();
    private final UUID creatorId = UUID.randomUUID();
    private final UUID jumboRollId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantDetails details = new TenantDetails(
                tenantId, plantId, inspectorId, "qc@papermill.com",
                Set.of(SystemRole.QC_INSPECTOR)
        );
        TenantContext.set(details);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Quality Inspection: Successfully inspect and release a Jumbo Roll")
    void testRecordInspection_Success() {
        JumboRollEntity jumbo = new JumboRollEntity();
        jumbo.setId(jumboRollId);
        jumbo.setTenantId(tenantId);
        jumbo.setPlantId(plantId);
        jumbo.setCreatedBy(creatorId);
        jumbo.setQualityDisposition(QualityDisposition.PENDING);

        when(jumboRollRepository.findByTenantIdAndId(tenantId, jumboRollId)).thenReturn(Optional.of(jumbo));
        when(featureFlagService.isMakerCheckerEnforced()).thenReturn(true);
        when(qualityInspectionRepository.save(any(QualityInspectionEntity.class))).thenAnswer(inv -> {
            QualityInspectionEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        QualityDto.CreateInspectionRequest request = new QualityDto.CreateInspectionRequest(
                plantId, RollType.JUMBO_ROLL, jumboRollId, null,
                new BigDecimal("80.0"), new BigDecimal("6.5"), new BigDecimal("105.0"),
                new BigDecimal("2.5"), QualityDisposition.RELEASED, "Passed laboratory burst and moisture tests"
        );

        var result = qualityService.recordInspection(request);

        assertNotNull(result);
        assertEquals(QualityDisposition.RELEASED, result.disposition());
        assertEquals(QualityDisposition.RELEASED, jumbo.getQualityDisposition());
        verify(jumboRollRepository).save(jumbo);
        verify(auditService).recordActionInTransaction(eq(tenantId), eq(inspectorId), eq("QUALITY_INSPECTION"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Separation of Duties (Maker-Checker): Reject inspection if inspector is the creator")
    void testRecordInspection_MakerCheckerViolation_ThrowsException() {
        JumboRollEntity jumbo = new JumboRollEntity();
        jumbo.setId(jumboRollId);
        jumbo.setTenantId(tenantId);
        jumbo.setPlantId(plantId);
        jumbo.setCreatedBy(inspectorId); // Inspector is the creator!

        when(jumboRollRepository.findByTenantIdAndId(tenantId, jumboRollId)).thenReturn(Optional.of(jumbo));
        when(featureFlagService.isMakerCheckerEnforced()).thenReturn(true);

        QualityDto.CreateInspectionRequest request = new QualityDto.CreateInspectionRequest(
                plantId, RollType.JUMBO_ROLL, jumboRollId, null,
                new BigDecimal("80.0"), new BigDecimal("6.5"), new BigDecimal("105.0"),
                new BigDecimal("2.5"), QualityDisposition.RELEASED, "Self-approved"
        );

        DomainException ex = assertThrows(DomainException.class, () -> qualityService.recordInspection(request));
        assertEquals("MAKER_CHECKER_VIOLATION", ex.getErrorCode());
        assertEquals(403, ex.getHttpStatus());
    }
}
