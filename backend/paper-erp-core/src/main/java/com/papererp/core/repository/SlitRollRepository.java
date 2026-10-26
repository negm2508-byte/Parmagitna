package com.papererp.core.repository;

import com.papererp.common.domain.RollStatus;
import com.papererp.core.domain.SlitRollEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlitRollRepository extends JpaRepository<SlitRollEntity, UUID> {
    Optional<SlitRollEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<SlitRollEntity> findByTenantIdAndSerialNumber(UUID tenantId, String serialNumber);
    Optional<SlitRollEntity> findByTenantIdAndBarcode(UUID tenantId, String barcode);
    List<SlitRollEntity> findAllByTenantIdAndSlittingRunId(UUID tenantId, UUID slittingRunId);
    List<SlitRollEntity> findAllByTenantIdAndParentJumboId(UUID tenantId, UUID parentJumboId);
    boolean existsByTenantIdAndSerialNumber(UUID tenantId, String serialNumber);
    boolean existsByTenantIdAndBarcode(UUID tenantId, String barcode);
    Page<SlitRollEntity> findAllByTenantIdAndPlantId(UUID tenantId, UUID plantId, Pageable pageable);
    Page<SlitRollEntity> findAllByTenantIdAndPlantIdAndStatus(UUID tenantId, UUID plantId, RollStatus status, Pageable pageable);
}
