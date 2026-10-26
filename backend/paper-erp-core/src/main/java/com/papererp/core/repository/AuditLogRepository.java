package com.papererp.core.repository;

import com.papererp.core.domain.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    Optional<AuditLogEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Page<AuditLogEntity> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<AuditLogEntity> findAllByTenantIdAndEntityTypeOrderByCreatedAtDesc(UUID tenantId, String entityType, Pageable pageable);
}
