package com.papererp.core.aspect;

import com.papererp.common.context.TenantContext;
import com.papererp.common.context.TenantDetails;
import com.papererp.common.exception.TenantAccessDeniedException;
import com.papererp.common.security.SystemRole;
import com.papererp.core.service.JumboRollService;
import com.papererp.core.service.OutboxService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tenant RLS Aspect Fail-Closed Boundary Tests")
class TenantRlsAspectTest {

    @InjectMocks
    private TenantRlsAspect tenantRlsAspect;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @BeforeEach
    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Fail-Closed: Tenant-scoped service call without tenant context throws TenantAccessDeniedException")
    void missingTenantContext_throwsTenantAccessDeniedException() {
        JumboRollService targetService = mock(JumboRollService.class);
        when(proceedingJoinPoint.getTarget()).thenReturn(targetService);
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("registerJumboRoll");

        // TenantContext is intentionally empty
        assertThrows(TenantAccessDeniedException.class, () ->
                tenantRlsAspect.applyTenantRlsContext(proceedingJoinPoint)
        );
    }

    @Test
    @DisplayName("Established Context: Tenant-scoped service call with authenticated context succeeds")
    void establishedTenantContext_proceedsSafely() throws Throwable {
        JumboRollService targetService = mock(JumboRollService.class);
        when(proceedingJoinPoint.getTarget()).thenReturn(targetService);
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("getJumboRollById");

        TenantContext.set(new TenantDetails(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "operator@mill.com", Set.of(SystemRole.PRODUCTION_OPERATOR)
        ));

        assertDoesNotThrow(() -> tenantRlsAspect.applyTenantRlsContext(proceedingJoinPoint));
    }

    @Test
    @DisplayName("System Bypass: OutboxService background execution does not require tenant context")
    void systemBackgroundService_allowedWithoutTenantContext() throws Throwable {
        OutboxService targetService = mock(OutboxService.class);
        when(proceedingJoinPoint.getTarget()).thenReturn(targetService);
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("processPendingEvents");

        // TenantContext is empty
        assertDoesNotThrow(() -> tenantRlsAspect.applyTenantRlsContext(proceedingJoinPoint));
    }
}
