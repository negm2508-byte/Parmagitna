package com.papererp.core.repository;

import com.papererp.core.domain.PaperGradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaperGradeRepository extends JpaRepository<PaperGradeEntity, UUID> {
    List<PaperGradeEntity> findAllByTenantId(UUID tenantId);
    Optional<PaperGradeEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<PaperGradeEntity> findByTenantIdAndCode(UUID tenantId, String code);
    boolean existsByTenantIdAndId(UUID tenantId, UUID id);
}
