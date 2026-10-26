package com.papererp.core.repository;

import com.papererp.core.domain.QualityInspectionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualityInspectionRepository extends JpaRepository<QualityInspectionEntity, UUID> {
    Optional<QualityInspectionEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    List<QualityInspectionEntity> findAllByTenantIdAndJumboRollIdOrderByInspectionTimestampDesc(UUID tenantId, UUID jumboRollId);
    List<QualityInspectionEntity> findAllByTenantIdAndSlitRollIdOrderByInspectionTimestampDesc(UUID tenantId, UUID slitRollId);
    Page<QualityInspectionEntity> findAllByTenantIdAndPlantId(UUID tenantId, UUID plantId, Pageable pageable);
}
