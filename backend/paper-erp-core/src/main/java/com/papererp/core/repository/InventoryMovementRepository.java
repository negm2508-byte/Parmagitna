package com.papererp.core.repository;

import com.papererp.core.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
    List<InventoryMovement> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<InventoryMovement> findByTenantIdAndJumboRollId(UUID tenantId, UUID jumboRollId);
    List<InventoryMovement> findByTenantIdAndSlitRollId(UUID tenantId, UUID slitRollId);
}
