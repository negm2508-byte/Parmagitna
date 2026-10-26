package com.papererp.core.repository;

import com.papererp.core.domain.WarehouseLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocationEntity, UUID> {
    List<WarehouseLocationEntity> findAllByTenantIdAndWarehouseId(UUID tenantId, UUID warehouseId);
    Optional<WarehouseLocationEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<WarehouseLocationEntity> findByTenantIdAndWarehouseIdAndCode(UUID tenantId, UUID warehouseId, String code);
}
