package com.papererp.core.repository;

import com.papererp.core.entity.SlittingRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlittingRunRepository extends JpaRepository<SlittingRun, UUID> {
    Optional<SlittingRun> findByTenantIdAndId(UUID tenantId, UUID id);
    List<SlittingRun> findByTenantIdAndParentJumboId(UUID tenantId, UUID parentJumboId);
    List<SlittingRun> findAllByTenantIdOrderByStartedAtDesc(UUID tenantId);
}
