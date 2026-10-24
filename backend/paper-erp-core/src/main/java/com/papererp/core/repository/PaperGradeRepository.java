package com.papererp.core.repository;

import com.papererp.core.entity.PaperGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaperGradeRepository extends JpaRepository<PaperGrade, UUID> {
    Optional<PaperGrade> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<PaperGrade> findByTenantIdAndCode(UUID tenantId, String code);
    List<PaperGrade> findAllByTenantIdAndActiveTrue(UUID tenantId);
}
