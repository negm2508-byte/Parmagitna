package com.papererp.core.repository;

import com.papererp.core.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MachineRepository extends JpaRepository<Machine, UUID> {
    Optional<Machine> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<Machine> findByTenantIdAndCode(UUID tenantId, String code);
    List<Machine> findAllByTenantIdAndActiveTrue(UUID tenantId);
    List<Machine> findAllByTenantIdAndType(UUID tenantId, String type);
}
