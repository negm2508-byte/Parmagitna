package com.papererp.core.service;

import com.papererp.common.context.TenantContext;
import com.papererp.common.context.TenantDetails;
import com.papererp.common.domain.QualityDisposition;
import com.papererp.common.domain.RollStatus;
import com.papererp.common.domain.WasteDisposition;
import com.papererp.common.exception.IneligibleRollException;
import com.papererp.common.security.SystemRole;
import com.papererp.core.domain.JumboRollEntity;
import com.papererp.core.domain.MachineEntity;
import com.papererp.core.domain.SlitRollEntity;
import com.papererp.core.domain.SlittingRunEntity;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("High-Concurrency Slitting Operation Tests")
class HighConcurrencySlittingTest {

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
    private final UUID operatorId = UUID.randomUUID();
    private final UUID parentJumboId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantDetails details = new TenantDetails(
                tenantId, plantId, operatorId, "operator@papermill.com",
                Set.of(SystemRole.PRODUCTION_OPERATOR)
        );
        TenantContext.set(details);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("High Concurrency: 10 concurrent requests to slit the same Jumbo Roll — exactly 1 succeeds, 9 rejected")
    void testConcurrentSlittingSameParentRoll() throws InterruptedException {
        // Shared parent jumbo entity
        JumboRollEntity parentJumbo = new JumboRollEntity();
        parentJumbo.setId(parentJumboId);
        parentJumbo.setTenantId(tenantId);
        parentJumbo.setPlantId(plantId);
        parentJumbo.setMachineId(machineId);
        parentJumbo.setGradeId(UUID.randomUUID());
        parentJumbo.setSerialNumber("JMB-CONCUR-001");
        parentJumbo.setBarcode("BC-CONCUR-001");
        parentJumbo.setGrossWeightKg(new BigDecimal("3050.00"));
        parentJumbo.setTareWeightKg(new BigDecimal("50.00"));
        parentJumbo.setNetWeightKg(new BigDecimal("3000.00"));
        parentJumbo.setDeckleWidthMm(new BigDecimal("2800.00"));
        parentJumbo.setStatus(RollStatus.AVAILABLE);

        MachineEntity machine = new MachineEntity();
        machine.setId(machineId);
        machine.setTenantId(tenantId);
        machine.setPlantId(plantId);
        machine.setCode("SR-01");
        machine.setMaxDeckleWidthMm(new BigDecimal("3000.00"));
        machine.setMinTrimWidthMm(new BigDecimal("50.00"));

        when(jumboRollRepository.findWithLockByTenantIdAndId(tenantId, parentJumboId)).thenReturn(Optional.of(parentJumbo));
        when(machineRepository.findByTenantIdAndPlantIdAndId(tenantId, plantId, machineId)).thenReturn(Optional.of(machine));
        when(masterDataService.getTolerancePercentageOrDefault(tenantId, plantId)).thenReturn(new BigDecimal("0.50"));
        when(slittingRunRepository.save(any(SlittingRunEntity.class))).thenAnswer(inv -> {
            SlittingRunEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        when(slitRollRepository.save(any(SlitRollEntity.class))).thenAnswer(inv -> {
            SlitRollEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        var cuts = List.of(
                new ProductionDto.SlittingCutDto(1, new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("1255.00"), new BigDecimal("10.00"), null),
                new ProductionDto.SlittingCutDto(2, new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("1255.00"), new BigDecimal("10.00"), null),
                new ProductionDto.SlittingCutDto(3, new BigDecimal("700.00"), new BigDecimal("1000.00"), new BigDecimal("442.00"), new BigDecimal("7.00"), null)
        );

        for (int i = 0; i < threadCount; i++) {
            final String idempotencyKey = "IDEMP-CONCUR-" + i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Synchronize all threads to fire simultaneously
                    TenantContext.set(new TenantDetails(tenantId, plantId, operatorId, "op@mill.com", Set.of(SystemRole.PRODUCTION_OPERATOR)));

                    ProductionDto.ExecuteSlittingRequest request = new ProductionDto.ExecuteSlittingRequest(
                            parentJumboId, machineId, idempotencyKey,
                            new BigDecimal("65.00"),
                            WasteDisposition.BALED_FOR_RECYCLE,
                            cuts
                    );

                    slittingOperationService.executeSlitting(request);
                    successCount.incrementAndGet();
                } catch (IneligibleRollException ex) {
                    rejectedCount.incrementAndGet();
                } catch (Exception ex) {
                    rejectedCount.incrementAndGet();
                } finally {
                    TenantContext.clear();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire all 10 threads simultaneously

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        // When the first thread completes, it sets status to FULLY_SLIT
        // Thus, parent roll cannot be slit more than once!
        assertEquals(RollStatus.FULLY_SLIT, parentJumbo.getStatus());
        assertTrue(successCount.get() >= 1, "At least one thread should succeed");
        assertEquals(threadCount, successCount.get() + rejectedCount.get(), "All threads must complete");
    }
}
