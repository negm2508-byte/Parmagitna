package com.papererp.core.repository;

import com.papererp.core.entity.JumboRoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JumboRollRepository extends JpaRepository<JumboRoll, UUID> {
    Optional<JumboRoll> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<JumboRoll> findByTenantIdAndSerialNumber(UUID tenantId, String serialNumber);
    List<JumboRoll> findByTenantIdAndStatus(UUID tenantId, String status);
    List<JumboRoll> findAllByTenantIdOrderByManufacturedAtDesc(UUID tenantId);
}
