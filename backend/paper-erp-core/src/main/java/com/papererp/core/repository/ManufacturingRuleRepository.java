package com.papererp.core.repository;

import com.papererp.core.domain.ManufacturingRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManufacturingRuleRepository extends JpaRepository<ManufacturingRuleEntity, UUID> {
    Optional<ManufacturingRuleEntity> findByTenantIdAndPlantIdAndRuleKey(UUID tenantId, UUID plantId, String ruleKey);
    Optional<ManufacturingRuleEntity> findByTenantIdAndPlantIdIsNullAndRuleKey(UUID tenantId, String ruleKey);
}
