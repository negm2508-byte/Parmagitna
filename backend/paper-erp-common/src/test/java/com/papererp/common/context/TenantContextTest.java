package com.papererp.common.context;

import com.papererp.common.exception.TenantAccessDeniedException;
import com.papererp.common.security.SystemRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantContext Unit Tests")
class TenantContextTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID plantId = UUID.randomUUID();

    @BeforeEach
    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should throw TenantAccessDeniedException when tenant context is empty")
    void shouldThrowWhenEmpty() {
        assertFalse(TenantContext.get().isPresent());
        assertThrows(TenantAccessDeniedException.class, TenantContext::getRequiredTenantId);
        assertThrows(TenantAccessDeniedException.class, TenantContext::getRequiredUserId);
    }

    @Test
    @DisplayName("Should store and retrieve tenant details correctly")
    void shouldStoreAndRetrieve() {
        TenantDetails details = new TenantDetails(
                tenantId,
                plantId,
                userId,
                "operator@factory.com",
                Set.of(SystemRole.PRODUCTION_OPERATOR)
        );

        TenantContext.set(details);

        assertTrue(TenantContext.get().isPresent());
        assertEquals(tenantId, TenantContext.getRequiredTenantId());
        assertEquals(userId, TenantContext.getRequiredUserId());
        assertEquals(plantId, TenantContext.getDefaultPlantId().orElseThrow());
        assertTrue(TenantContext.get().get().hasRole(SystemRole.PRODUCTION_OPERATOR));
        assertFalse(TenantContext.get().get().hasRole(SystemRole.ADMIN));
    }

    @Test
    @DisplayName("Should clear context cleanly without leaking")
    void shouldClearCleanly() {
        TenantDetails details = new TenantDetails(
                tenantId,
                null,
                userId,
                "admin@factory.com",
                Set.of(SystemRole.ADMIN)
        );

        TenantContext.set(details);
        TenantContext.clear();

        assertFalse(TenantContext.get().isPresent());
    }
}
