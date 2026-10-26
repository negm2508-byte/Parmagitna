package com.papererp.core.repository;

import com.papererp.core.domain.WarehouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseRepository extends JpaRepository<WarehouseEntity, UUID> {
    List<WarehouseEntity> findAllByTenantIdAndPlantId(UUID tenantId, UUID plantId);
    Optional<WarehouseEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<WarehouseEntity> findByTenantIdAndPlantIdAndCode(UUID tenantId, UUID plantId, String code);
}
