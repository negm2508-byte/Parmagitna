package com.papererp.core.repository;

import com.papererp.core.domain.PlantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlantRepository extends JpaRepository<PlantEntity, UUID> {
    List<PlantEntity> findAllByTenantId(UUID tenantId);
    Optional<PlantEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<PlantEntity> findByTenantIdAndCode(UUID tenantId, String code);
    boolean existsByTenantIdAndId(UUID tenantId, UUID id);
}
