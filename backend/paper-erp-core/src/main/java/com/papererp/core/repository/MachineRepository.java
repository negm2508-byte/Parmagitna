package com.papererp.core.repository;

import com.papererp.core.domain.MachineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MachineRepository extends JpaRepository<MachineEntity, UUID> {
    List<MachineEntity> findAllByTenantId(UUID tenantId);
    List<MachineEntity> findAllByTenantIdAndPlantId(UUID tenantId, UUID plantId);
    Optional<MachineEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<MachineEntity> findByTenantIdAndPlantIdAndId(UUID tenantId, UUID plantId, UUID id);
    Optional<MachineEntity> findByTenantIdAndCode(UUID tenantId, String code);
}
