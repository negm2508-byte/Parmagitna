package com.papererp.core.aspect;

import com.papererp.common.context.TenantContext;
import com.papererp.common.exception.TenantAccessDeniedException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;
import java.util.UUID;

/**
 * Aspect that integrates Spring Boot transactions with PostgreSQL Row Level Security (RLS).
 * Sets PostgreSQL session variable 'app.current_tenant_id' transaction-locally using set_config(..., true).
 * Runs with high precedence inside the active transaction boundary.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class TenantRlsAspect {

    private static final Logger log = LoggerFactory.getLogger(TenantRlsAspect.class);
    private static final String RLS_SYNC_KEY = "POSTGRESQL_RLS_TENANT_ID";

    // Non-tenant-scoped services (system background tasks or pre-auth flows)
    private static final Set<String> NON_TENANT_SERVICES = Set.of(
            "IdentityService",
            "OutboxService"
    );

    @PersistenceContext
    private EntityManager entityManager;

    private volatile Boolean isPostgres = null;

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || @within(org.springframework.transaction.annotation.Transactional)")
    public Object applyTenantRlsContext(ProceedingJoinPoint pjp) throws Throwable {
        String serviceName = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();

        boolean isSystemOrPreAuth = NON_TENANT_SERVICES.stream().anyMatch(serviceName::contains);

        if (isSystemOrPreAuth) {
            // System background worker or authentication service
            if (serviceName.contains("OutboxService")) {
                applyPostgresRlsSetting("SYSTEM");
            }
            return pjp.proceed();
        }

        // 1. Enforce that tenant context is present for all tenant-scoped operations
        var tenantOpt = TenantContext.get();
        if (tenantOpt.isEmpty()) {
            throw new TenantAccessDeniedException(
                    "Tenant context is required but not established for operational service: " + serviceName + "." + methodName
            );
        }

        UUID tenantId = tenantOpt.get().tenantId();
        applyPostgresRlsSetting(tenantId.toString());

        return pjp.proceed();
    }

    private void applyPostgresRlsSetting(String tenantIdStr) {
        if (!isPostgreSqlDatabase()) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            String boundTenant = (String) TransactionSynchronizationManager.getResource(RLS_SYNC_KEY);
            if (tenantIdStr.equals(boundTenant)) {
                // Already established on this transaction
                return;
            }

            executeSetConfig(tenantIdStr);
            TransactionSynchronizationManager.bindResource(RLS_SYNC_KEY, tenantIdStr);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    TransactionSynchronizationManager.unbindResourceIfPossible(RLS_SYNC_KEY);
                }
            });
        } else {
            executeSetConfig(tenantIdStr);
        }
    }

    private void executeSetConfig(String tenantIdStr) {
        try {
            entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tenantId, true)")
                    .setParameter("tenantId", tenantIdStr)
                    .getSingleResult();
            log.trace("Set PostgreSQL RLS app.current_tenant_id = '{}' (is_local=true)", tenantIdStr);
        } catch (Exception ex) {
            log.error("Failed to set PostgreSQL RLS tenant context: {}", ex.getMessage());
            throw new TenantAccessDeniedException("Failed to establish secure tenant RLS context: " + ex.getMessage());
        }
    }

    private boolean isPostgreSqlDatabase() {
        if (isPostgres == null) {
            synchronized (this) {
                if (isPostgres == null) {
                    try {
                        Session session = entityManager.unwrap(Session.class);
                        session.doWork(connection -> {
                            String dbName = connection.getMetaData().getDatabaseProductName();
                            isPostgres = dbName != null && dbName.toLowerCase().contains("postgres");
                        });
                    } catch (Exception ex) {
                        log.debug("Database metadata check error: {}", ex.getMessage());
                        isPostgres = false;
                    }
                }
            }
        }
        return Boolean.TRUE.equals(isPostgres);
    }
}
