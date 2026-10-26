package com.papererp.core.repository;

import com.papererp.core.domain.InventoryLedgerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryLedgerRepository extends JpaRepository<InventoryLedgerEntity, UUID> {

    Optional<InventoryLedgerEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<InventoryLedgerEntity> findAllByTenantIdAndJumboRollIdOrderByMovementTimestampDesc(UUID tenantId, UUID jumboRollId);

    List<InventoryLedgerEntity> findAllByTenantIdAndSlitRollIdOrderByMovementTimestampDesc(UUID tenantId, UUID slitRollId);

    Page<InventoryLedgerEntity> findAllByTenantIdAndPlantIdOrderByMovementTimestampDesc(UUID tenantId, UUID plantId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(l.quantityChange), 0) FROM InventoryLedgerEntity l WHERE l.tenantId = :tenantId AND l.jumboRollId = :jumboRollId")
    BigDecimal sumQuantityByJumboRollId(@Param("tenantId") UUID tenantId, @Param("jumboRollId") UUID jumboRollId);

    @Query("SELECT COALESCE(SUM(l.quantityChange), 0) FROM InventoryLedgerEntity l WHERE l.tenantId = :tenantId AND l.slitRollId = :slitRollId")
    BigDecimal sumQuantityBySlitRollId(@Param("tenantId") UUID tenantId, @Param("slitRollId") UUID slitRollId);

    List<InventoryLedgerEntity> findByTenantIdAndPlantIdAndMovementTimestampLessThanOrderByMovementTimestampDesc(
            UUID tenantId, UUID plantId, java.time.Instant before, Pageable pageable);
}
