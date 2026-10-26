package com.papererp.core.repository;

import com.papererp.common.domain.RollStatus;
import com.papererp.core.domain.JumboRollEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JumboRollRepository extends JpaRepository<JumboRollEntity, UUID> {

    Optional<JumboRollEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM JumboRollEntity j WHERE j.tenantId = :tenantId AND j.id = :id")
    Optional<JumboRollEntity> findWithLockByTenantIdAndId(@Param("tenantId") UUID tenantId, @Param("id") UUID id);

    Optional<JumboRollEntity> findByTenantIdAndSerialNumber(UUID tenantId, String serialNumber);

    Optional<JumboRollEntity> findByTenantIdAndBarcode(UUID tenantId, String barcode);

    boolean existsByTenantIdAndSerialNumber(UUID tenantId, String serialNumber);

    boolean existsByTenantIdAndBarcode(UUID tenantId, String barcode);

    Page<JumboRollEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    Page<JumboRollEntity> findAllByTenantIdAndPlantId(UUID tenantId, UUID plantId, Pageable pageable);

    Page<JumboRollEntity> findAllByTenantIdAndPlantIdAndStatus(UUID tenantId, UUID plantId, RollStatus status, Pageable pageable);
}
