package com.papererp.core.service;

import com.papererp.common.context.TenantContext;
import com.papererp.core.domain.AuditLogEntity;
import com.papererp.core.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordActionInTransaction(UUID tenantId, UUID actorId, String action, String entityType,
                                          UUID entityId, String clientIp, String detailsJson) {
        AuditLogEntity log = new AuditLogEntity(tenantId, actorId, action, entityType, entityId, clientIp, detailsJson);
        auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void recordAction(String action, String entityType, UUID entityId, String clientIp, String detailsJson) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UUID actorId = TenantContext.getRequiredUserId();
        AuditLogEntity log = new AuditLogEntity(tenantId, actorId, action, entityType, entityId, clientIp, detailsJson);
        auditLogRepository.save(log);
    }
}
