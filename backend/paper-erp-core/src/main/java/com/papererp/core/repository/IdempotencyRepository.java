package com.papererp.core.repository;

import com.papererp.core.domain.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {
    Optional<IdempotencyRecordEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    void deleteByExpiresAtBefore(Instant now);
}
