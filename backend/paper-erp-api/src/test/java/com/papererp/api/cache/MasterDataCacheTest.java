package com.papererp.api.cache;

import com.papererp.api.config.CacheConfig;
import com.papererp.common.context.TenantContext;
import com.papererp.common.context.TenantDetails;
import com.papererp.common.security.SystemRole;
import com.papererp.core.domain.MachineEntity;
import com.papererp.core.domain.PaperGradeEntity;
import com.papererp.core.dto.MasterDataDto;
import com.papererp.core.repository.*;
import com.papererp.core.service.MasterDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({CacheConfig.class, MasterDataService.class})
@DisplayName("Master Data Caching & Invalidation Correctness Tests")
class MasterDataCacheTest {

    @Autowired
    private MasterDataService masterDataService;

    @MockBean
    private PlantRepository plantRepository;

    @MockBean
    private MachineRepository machineRepository;

    @MockBean
    private PaperGradeRepository paperGradeRepository;

    @MockBean
    private CoreSpecificationRepository coreSpecificationRepository;

    @MockBean
    private ManufacturingRuleRepository manufacturingRuleRepository;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final UUID plantId = UUID.randomUUID();
    private final UUID machineId = UUID.randomUUID();
    private final UUID gradeId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        TenantContext.set(new TenantDetails(tenantA, plantId, UUID.randomUUID(), "admin@factory.com", Set.of(SystemRole.ADMIN)));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Cache Hit: Repeated getMachineById hits cache and queries repository only once")
    void getMachineById_repeatedCalls_hitsCache() {
        MachineEntity machine = new MachineEntity(
                tenantA, plantId, "PM-01", "Paper Machine 1", "PAPER_MACHINE",
                new BigDecimal("2800.00"), new BigDecimal("100.00")
        );
        machine.setId(machineId);

        when(machineRepository.findByTenantIdAndId(tenantA, machineId)).thenReturn(Optional.of(machine));

        // First call: Should hit repository
        MasterDataDto.MachineDto first = masterDataService.getMachineById(machineId);
        assertNotNull(first);
        assertEquals("PM-01", first.code());

        // Second call: Should hit Caffeine cache directly (repository not called again)
        MasterDataDto.MachineDto second = masterDataService.getMachineById(machineId);
        assertNotNull(second);
        assertEquals("PM-01", second.code());

        verify(machineRepository, times(1)).findByTenantIdAndId(tenantA, machineId);
    }

    @Test
    @DisplayName("Cache Eviction: createMachine evicts machine cache, triggering repository re-query")
    void createMachine_evictsCache() {
        MachineEntity machine = new MachineEntity(
                tenantA, plantId, "PM-02", "Paper Machine 2", "PAPER_MACHINE",
                new BigDecimal("3200.00"), new BigDecimal("100.00")
        );
        machine.setId(machineId);

        when(machineRepository.findByTenantIdAndId(tenantA, machineId)).thenReturn(Optional.of(machine));
        when(plantRepository.existsByTenantIdAndId(tenantA, plantId)).thenReturn(true);
        when(machineRepository.findByTenantIdAndCode(tenantA, "PM-02")).thenReturn(Optional.empty());
        when(machineRepository.save(any(MachineEntity.class))).thenReturn(machine);

        // 1. Warm cache
        masterDataService.getMachineById(machineId);
        verify(machineRepository, times(1)).findByTenantIdAndId(tenantA, machineId);

        // 2. Create machine (evicts cache)
        masterDataService.createMachine(new MasterDataDto.CreateMachineRequest(
                plantId, "PM-02", "New Machine", "PAPER_MACHINE",
                new BigDecimal("3200.00"), new BigDecimal("100.00")
        ));

        // 3. Next query should miss cache and hit repository a 2nd time
        masterDataService.getMachineById(machineId);
        verify(machineRepository, times(2)).findByTenantIdAndId(tenantA, machineId);
    }

    @Test
    @DisplayName("Multi-Tenant Cache Key Isolation: Tenant B cannot access Tenant A's cached entries")
    void tenantIsolationInCache() {
        MachineEntity machineA = new MachineEntity(
                tenantA, plantId, "PM-A", "Machine A", "PAPER_MACHINE",
                new BigDecimal("2500.00"), new BigDecimal("100.00")
        );
        machineA.setId(machineId);

        MachineEntity machineB = new MachineEntity(
                tenantB, plantId, "PM-B", "Machine B", "PAPER_MACHINE",
                new BigDecimal("2500.00"), new BigDecimal("100.00")
        );
        machineB.setId(machineId);

        when(machineRepository.findByTenantIdAndId(tenantA, machineId)).thenReturn(Optional.of(machineA));
        when(machineRepository.findByTenantIdAndId(tenantB, machineId)).thenReturn(Optional.of(machineB));

        // Query as Tenant A
        MasterDataDto.MachineDto resultA = masterDataService.getMachineById(machineId);
        assertEquals("PM-A", resultA.code());

        // Switch to Tenant B
        TenantContext.set(new TenantDetails(tenantB, plantId, UUID.randomUUID(), "admin@factory-b.com", Set.of(SystemRole.ADMIN)));

        MasterDataDto.MachineDto resultB = masterDataService.getMachineById(machineId);
        assertEquals("PM-B", resultB.code());

        verify(machineRepository, times(1)).findByTenantIdAndId(tenantA, machineId);
        verify(machineRepository, times(1)).findByTenantIdAndId(tenantB, machineId);
    }
}
