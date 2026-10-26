package com.papererp.core.service;

import com.papererp.common.context.TenantContext;
import com.papererp.common.context.TenantDetails;
import com.papererp.common.domain.MassBalanceStatus;
import com.papererp.common.domain.RollStatus;
import com.papererp.common.domain.WasteDisposition;
import com.papererp.common.security.SystemRole;
import com.papererp.common.exception.DomainException;
import com.papererp.common.exception.IneligibleRollException;
import com.papererp.common.exception.MassBalanceExceededToleranceException;
import com.papererp.core.domain.*;
import com.papererp.core.dto.ProductionDto;
import com.papererp.core.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlittingOperationService Unit Tests")
class SlittingOperationServiceTest {

    @Mock
    private JumboRollRepository jumboRollRepository;
    @Mock
    private SlittingRunRepository slittingRunRepository;
    @Mock
    private SlitRollRepository slitRollRepository;
    @Mock
    private MachineRepository machineRepository;
    @Mock
    private InventoryLedgerRepository inventoryLedgerRepository;
    @Mock
    private MasterDataService masterDataService;
    @Mock
    private AuditService auditService;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private SlittingOperationService slittingOperationService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID plantId = UUID.randomUUID();
    private final UUID machineId = UUID.randomUUID();
    private final UUID gradeId = UUID.randomUUID();
    private final UUID operatorId = UUID.randomUUID();
    private final UUID parentJumboId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantDetails details = new TenantDetails(
                tenantId, plantId, operatorId, "operator@factory.com",
                Set.of(SystemRole.PRODUCTION_OPERATOR)
        );
        TenantContext.set(details);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Factory Benchmark: Input 3000kg, Outputs 1245+1245+435=2925kg, Waste 65kg -> -10kg Variance (Within 0.5% Tolerance)")
    void shouldExecuteSlittingSuccessfullyWithinTolerance() {
        // Arrange
        JumboRollEntity parentJumbo = new JumboRollEntity(
                tenantId, plantId, machineId, gradeId, null,
                "JR-20261026-0001", "BC-JR0001",
                new BigDecimal("3050.00"), new BigDecimal("50.00"), new BigDecimal("3000.00"),
                new BigDecimal("2800.00"), new BigDecimal("1200.00"), operatorId
        );
        parentJumbo.setId(parentJumboId);

        MachineEntity machine = new MachineEntity(
                tenantId, plantId, "SLIT-01", "ماكينة تقطيع 1", "SLITTER_REWINDER",
                new BigDecimal("3000.00"), new BigDecimal("40.00")
        );
        machine.setId(machineId);

        when(slittingRunRepository.existsByTenantIdAndIdempotencyKey(tenantId, "IDEMP-001")).thenReturn(false);
        when(jumboRollRepository.findWithLockByTenantIdAndId(tenantId, parentJumboId)).thenReturn(Optional.of(parentJumbo));
        when(machineRepository.findByTenantIdAndPlantIdAndId(tenantId, plantId, machineId)).thenReturn(Optional.of(machine));
        when(masterDataService.getTolerancePercentageOrDefault(tenantId, plantId)).thenReturn(new BigDecimal("0.50"));

        when(slittingRunRepository.save(any(SlittingRunEntity.class))).thenAnswer(i -> {
            SlittingRunEntity e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        when(slitRollRepository.save(any(SlitRollEntity.class))).thenAnswer(i -> {
            SlitRollEntity e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        var cuts = List.of(
                new ProductionDto.SlittingCutDto(1, new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("1255.00"), new BigDecimal("10.00"), null), // net 1245
                new ProductionDto.SlittingCutDto(2, new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("1255.00"), new BigDecimal("10.00"), null), // net 1245
                new ProductionDto.SlittingCutDto(3, new BigDecimal("700.00"), new BigDecimal("1000.00"), new BigDecimal("442.00"), new BigDecimal("7.00"), null)     // net 435
        );

        ProductionDto.ExecuteSlittingRequest request = new ProductionDto.ExecuteSlittingRequest(
                parentJumboId, machineId, "IDEMP-001",
                new BigDecimal("65.00"), // trim waste
                WasteDisposition.BALED_FOR_RECYCLE,
                cuts
        );

        // Act
        ProductionDto.SlittingRunDto result = slittingOperationService.executeSlitting(request);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("-10.00"), result.scaleVarianceKg());
        assertEquals(MassBalanceStatus.BALANCED_WITHIN_TOLERANCE, result.massBalanceStatus());
        assertEquals(new BigDecimal("2925.00"), result.childRollsTotalWeightKg());
        assertEquals(new BigDecimal("65.00"), result.recordedTrimWasteKg());
        assertEquals(new BigDecimal("3000.00"), result.inputNetWeightKg());
        assertEquals(3, result.childRolls().size());

        // Verify parent Jumbo Roll transitioned to FULLY_SLIT
        assertEquals(RollStatus.FULLY_SLIT, parentJumbo.getStatus());
        verify(jumboRollRepository).save(parentJumbo);

        // Verify ledger entries: 3 receipts + 1 consumption = 4 ledger entries
        verify(inventoryLedgerRepository, times(4)).save(any(InventoryLedgerEntity.class));

        // Verify audit log
        verify(auditService).recordActionInTransaction(eq(tenantId), eq(operatorId), eq("EXECUTE_SLITTING"), eq("SLITTING_RUN"), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject slitting when mass balance variance exceeds allowed tolerance limit")
    void shouldRejectWhenMassBalanceExceedsTolerance() {
        // Arrange: 3000kg input, but child outputs = 2800kg + 50kg waste = 2850kg (-150kg variance, 5% error)
        JumboRollEntity parentJumbo = new JumboRollEntity(
                tenantId, plantId, machineId, gradeId, null,
                "JR-20261026-0002", "BC-JR0002",
                new BigDecimal("3050.00"), new BigDecimal("50.00"), new BigDecimal("3000.00"),
                new BigDecimal("2800.00"), new BigDecimal("1200.00"), operatorId
        );
        parentJumbo.setId(parentJumboId);

        MachineEntity machine = new MachineEntity(
                tenantId, plantId, "SLIT-01", "ماكينة تقطيع 1", "SLITTER_REWINDER",
                new BigDecimal("3000.00"), new BigDecimal("40.00")
        );
        machine.setId(machineId);

        when(slittingRunRepository.existsByTenantIdAndIdempotencyKey(tenantId, "IDEMP-002")).thenReturn(false);
        when(jumboRollRepository.findWithLockByTenantIdAndId(tenantId, parentJumboId)).thenReturn(Optional.of(parentJumbo));
        when(machineRepository.findByTenantIdAndPlantIdAndId(tenantId, plantId, machineId)).thenReturn(Optional.of(machine));
        when(masterDataService.getTolerancePercentageOrDefault(tenantId, plantId)).thenReturn(new BigDecimal("0.50"));

        var cuts = List.of(
                new ProductionDto.SlittingCutDto(1, new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("2810.00"), new BigDecimal("10.00"), null) // net 2800
        );

        ProductionDto.ExecuteSlittingRequest request = new ProductionDto.ExecuteSlittingRequest(
                parentJumboId, machineId, "IDEMP-002",
                new BigDecimal("50.00"), // trim waste
                WasteDisposition.BALED_FOR_RECYCLE,
                cuts
        );

        // Act & Assert
        MassBalanceExceededToleranceException ex = assertThrows(
                MassBalanceExceededToleranceException.class,
                () -> slittingOperationService.executeSlitting(request)
        );

        assertEquals(new BigDecimal("-150.00"), ex.getVarianceKg());
        assertEquals(new BigDecimal("15.00"), ex.getMaxAllowedVarianceKg()); // 0.5% of 3000kg
        assertEquals(RollStatus.AVAILABLE, parentJumbo.getStatus()); // Parent untouched

        verify(slitRollRepository, never()).save(any());
        verify(inventoryLedgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject slitting when cut widths exceed parent deckle width")
    void shouldRejectWhenCutWidthExceedsDeckle() {
        // Parent deckle width = 2000 mm, cuts = 1200 + 900 = 2100 mm
        JumboRollEntity parentJumbo = new JumboRollEntity(
                tenantId, plantId, machineId, gradeId, null,
                "JR-20261026-0003", "BC-JR0003",
                new BigDecimal("2050.00"), new BigDecimal("50.00"), new BigDecimal("2000.00"),
                new BigDecimal("2000.00"), new BigDecimal("1200.00"), operatorId
        );
        parentJumbo.setId(parentJumboId);

        MachineEntity machine = new MachineEntity(
                tenantId, plantId, "SLIT-01", "ماكينة تقطيع 1", "SLITTER_REWINDER",
                new BigDecimal("3000.00"), new BigDecimal("40.00")
        );
        machine.setId(machineId);

        when(slittingRunRepository.existsByTenantIdAndIdempotencyKey(tenantId, "IDEMP-003")).thenReturn(false);
        when(jumboRollRepository.findWithLockByTenantIdAndId(tenantId, parentJumboId)).thenReturn(Optional.of(parentJumbo));
        when(machineRepository.findByTenantIdAndPlantIdAndId(tenantId, plantId, machineId)).thenReturn(Optional.of(machine));

        var cuts = List.of(
                new ProductionDto.SlittingCutDto(1, new BigDecimal("1200.00"), new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("10.00"), null),
                new ProductionDto.SlittingCutDto(2, new BigDecimal("900.00"), new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("10.00"), null)
        );

        ProductionDto.ExecuteSlittingRequest request = new ProductionDto.ExecuteSlittingRequest(
                parentJumboId, machineId, "IDEMP-003",
                new BigDecimal("50.00"),
                WasteDisposition.BALED_FOR_RECYCLE,
                cuts
        );

        assertThrows(DomainException.class, () -> slittingOperationService.executeSlitting(request));
    }

    @Test
    @DisplayName("Should reject slitting when parent Jumbo Roll is already slit or ineligible")
    void shouldRejectIneligibleParentRoll() {
        JumboRollEntity parentJumbo = new JumboRollEntity(
                tenantId, plantId, machineId, gradeId, null,
                "JR-20261026-0004", "BC-JR0004",
                new BigDecimal("2050.00"), new BigDecimal("50.00"), new BigDecimal("2000.00"),
                new BigDecimal("2000.00"), new BigDecimal("1200.00"), operatorId
        );
        parentJumbo.setId(parentJumboId);
        parentJumbo.setStatus(RollStatus.FULLY_SLIT); // Already slit!

        when(slittingRunRepository.existsByTenantIdAndIdempotencyKey(tenantId, "IDEMP-004")).thenReturn(false);
        when(jumboRollRepository.findWithLockByTenantIdAndId(tenantId, parentJumboId)).thenReturn(Optional.of(parentJumbo));

        var cuts = List.of(
                new ProductionDto.SlittingCutDto(1, new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("10.00"), null)
        );

        ProductionDto.ExecuteSlittingRequest request = new ProductionDto.ExecuteSlittingRequest(
                parentJumboId, machineId, "IDEMP-004",
                new BigDecimal("20.00"),
                WasteDisposition.BALED_FOR_RECYCLE,
                cuts
        );

        assertThrows(IneligibleRollException.class, () -> slittingOperationService.executeSlitting(request));
    }
}
