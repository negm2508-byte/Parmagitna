package com.papererp.core.repository;

import com.papererp.core.domain.CoreSpecificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoreSpecificationRepository extends JpaRepository<CoreSpecificationEntity, UUID> {
    List<CoreSpecificationEntity> findAllByTenantId(UUID tenantId);
    Optional<CoreSpecificationEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<CoreSpecificationEntity> findByTenantIdAndCode(UUID tenantId, String code);
}
