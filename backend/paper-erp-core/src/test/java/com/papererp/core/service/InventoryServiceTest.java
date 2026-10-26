package com.papererp.core.service;

import com.papererp.common.context.TenantContext;
import com.papererp.common.context.TenantDetails;
import com.papererp.common.domain.QualityDisposition;
import com.papererp.common.domain.RollStatus;
import com.papererp.common.domain.RollType;
import com.papererp.common.security.SystemRole;
import com.papererp.common.exception.IneligibleRollException;
import com.papererp.core.domain.InventoryLedgerEntity;
import com.papererp.core.domain.JumboRollEntity;
import com.papererp.core.domain.SlitRollEntity;
import com.papererp.core.domain.WarehouseLocationEntity;
import com.papererp.core.dto.InventoryDto;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock
    private InventoryLedgerRepository inventoryLedgerRepository;
    @Mock
    private JumboRollRepository jumboRollRepository;
    @Mock
    private SlitRollRepository slitRollRepository;
    @Mock
    private WarehouseLocationRepository warehouseLocationRepository;
    @Mock
    private QualityInspectionRepository qualityInspectionRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private InventoryService inventoryService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID plantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID rollId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantDetails details = new TenantDetails(
                tenantId, plantId, userId, "keeper@factory.com",
                Set.of(SystemRole.WAREHOUSE_KEEPER)
        );
        TenantContext.set(details);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Quality Gate: Should strictly reject dispatch of roll with PENDING quality disposition")
    void shouldRejectDispatchWhenQualityPending() {
        SlitRollEntity roll = new SlitRollEntity();
        roll.setId(rollId);
        roll.setTenantId(tenantId);
        roll.setPlantId(plantId);
        roll.setSerialNumber("SR-PENDING-001");
        roll.setStatus(RollStatus.AVAILABLE);
        roll.setQualityDisposition(QualityDisposition.PENDING); // Not released!

        when(slitRollRepository.findByTenantIdAndId(tenantId, rollId)).thenReturn(Optional.of(roll));

        InventoryDto.DispatchRollRequest request = new InventoryDto.DispatchRollRequest(
                RollType.SLIT_ROLL, null, rollId, "CUST-PO-100", "Dispatch to Cairo Client"
        );

        IneligibleRollException ex = assertThrows(
                IneligibleRollException.class,
                () -> inventoryService.dispatchRoll(request)
        );

        assertTrue(ex.getMessage().contains("cannot be dispatched"));
        assertEquals(RollStatus.AVAILABLE, roll.getStatus()); // Untouched

        // Verify zero dispatch movements written to ledger
        verify(inventoryLedgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Quality Gate: Should successfully dispatch roll with RELEASED quality disposition")
    void shouldDispatchReleasedRollSuccessfully() {
        SlitRollEntity roll = new SlitRollEntity();
        roll.setId(rollId);
        roll.setTenantId(tenantId);
        roll.setPlantId(plantId);
        roll.setSerialNumber("SR-RELEASED-001");
        roll.setStatus(RollStatus.AVAILABLE);
        roll.setQualityDisposition(QualityDisposition.RELEASED); // Approved by QC!
        roll.setNetWeightKg(new BigDecimal("1245.00"));

        when(slitRollRepository.findByTenantIdAndId(tenantId, rollId)).thenReturn(Optional.of(roll));
        when(inventoryLedgerRepository.save(any(InventoryLedgerEntity.class))).thenAnswer(i -> {
            InventoryLedgerEntity e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        InventoryDto.DispatchRollRequest request = new InventoryDto.DispatchRollRequest(
                RollType.SLIT_ROLL, null, rollId, "CUST-PO-100", "Dispatch to Cairo Client"
        );

        InventoryDto.InventoryLedgerDto result = inventoryService.dispatchRoll(request);

        assertNotNull(result);
        assertEquals(RollStatus.DISPATCHED, roll.getStatus());
        verify(slitRollRepository).save(roll);
        verify(inventoryLedgerRepository).save(any(InventoryLedgerEntity.class));
    }

    @Test
    @DisplayName("Location Transfer: Should update warehouse location and record movement in ledger")
    void shouldTransferLocationSuccessfully() {
        WarehouseLocationEntity targetLoc = new WarehouseLocationEntity(tenantId, UUID.randomUUID(), "LOC-B-02", "B", "02");
        targetLoc.setId(locationId);

        SlitRollEntity roll = new SlitRollEntity();
        roll.setId(rollId);
        roll.setTenantId(tenantId);
        roll.setPlantId(plantId);
        roll.setSerialNumber("SR-TRANS-001");
        roll.setStatus(RollStatus.AVAILABLE);
        roll.setNetWeightKg(new BigDecimal("500.00"));

        when(warehouseLocationRepository.findByTenantIdAndId(tenantId, locationId)).thenReturn(Optional.of(targetLoc));
        when(slitRollRepository.findByTenantIdAndId(tenantId, rollId)).thenReturn(Optional.of(roll));
        when(inventoryLedgerRepository.save(any(InventoryLedgerEntity.class))).thenAnswer(i -> {
            InventoryLedgerEntity e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        InventoryDto.TransferLocationRequest request = new InventoryDto.TransferLocationRequest(
                RollType.SLIT_ROLL, null, rollId, locationId, "Transfer to staging rack B"
        );

        InventoryDto.InventoryLedgerDto result = inventoryService.transferLocation(request);

        assertNotNull(result);
        assertEquals(locationId, roll.getLocationId());
        verify(slitRollRepository).save(roll);
        verify(inventoryLedgerRepository).save(any(InventoryLedgerEntity.class));
    }
}
