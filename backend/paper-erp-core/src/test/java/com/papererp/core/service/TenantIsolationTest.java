package com.papererp.core.service;

import com.papererp.common.context.TenantContext;
import com.papererp.common.context.TenantDetails;
import com.papererp.common.security.SystemRole;
import com.papererp.common.exception.EntityNotFoundException;
import com.papererp.core.domain.JumboRollEntity;
import com.papererp.core.repository.JumboRollRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Multi-Tenant Cross-Tenant Isolation Tests")
class TenantIsolationTest {

    @Mock
    private JumboRollRepository jumboRollRepository;

    @InjectMocks
    private JumboRollService jumboRollService;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final UUID rollIdBelongingToTenantA = UUID.randomUUID();

    @BeforeEach
    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Cross-Tenant Isolation: Tenant B cannot query or read Tenant A's Jumbo Roll even with known UUID")
    void tenantBCannotReadTenantARoll() {
        // Given: The roll belongs to Tenant A
        JumboRollEntity rollTenantA = new JumboRollEntity();
        rollTenantA.setId(rollIdBelongingToTenantA);
        rollTenantA.setTenantId(tenantA);

        // When Tenant B is authenticated
        TenantDetails tenantBDetails = new TenantDetails(
                tenantB, null, UUID.randomUUID(), "operatorB@factory-b.com",
                Set.of(SystemRole.PRODUCTION_OPERATOR)
        );
        TenantContext.set(tenantBDetails);

        // Repository filters by authenticated tenantId: findByTenantIdAndId(tenantB, rollId) returns empty!
        when(jumboRollRepository.findByTenantIdAndId(tenantB, rollIdBelongingToTenantA))
                .thenReturn(Optional.empty());

        // Then: Service throws EntityNotFoundException (non-disclosing error, does not reveal Tenant A exists)
        assertThrows(EntityNotFoundException.class, () -> {
            jumboRollService.getJumboRollById(rollIdBelongingToTenantA);
        });
    }
}
