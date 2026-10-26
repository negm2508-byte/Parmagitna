package com.papererp.core.repository;

import com.papererp.core.domain.SlittingRunEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlittingRunRepository extends JpaRepository<SlittingRunEntity, UUID> {
    Optional<SlittingRunEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<SlittingRunEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    boolean existsByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    Page<SlittingRunEntity> findAllByTenantIdAndPlantId(UUID tenantId, UUID plantId, Pageable pageable);
}
