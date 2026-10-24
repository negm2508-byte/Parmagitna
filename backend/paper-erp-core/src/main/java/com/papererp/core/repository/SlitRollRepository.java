package com.papererp.core.repository;

import com.papererp.core.entity.SlitRoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlitRollRepository extends JpaRepository<SlitRoll, UUID> {
    Optional<SlitRoll> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<SlitRoll> findByTenantIdAndBarcode(UUID tenantId, String barcode);
    Optional<SlitRoll> findByTenantIdAndSerialNumber(UUID tenantId, String serialNumber);
    List<SlitRoll> findByTenantIdAndSlittingRunId(UUID tenantId, UUID slittingRunId);
    List<SlitRoll> findByTenantIdAndParentJumboId(UUID tenantId, UUID parentJumboId);
    List<SlitRoll> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
