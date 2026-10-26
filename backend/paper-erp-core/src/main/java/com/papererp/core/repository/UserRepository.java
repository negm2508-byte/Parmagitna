package com.papererp.core.repository;

import com.papererp.core.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByTenantIdAndUsername(UUID tenantId, String username);
    Optional<UserEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    boolean existsByTenantIdAndUsername(UUID tenantId, String username);
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}
