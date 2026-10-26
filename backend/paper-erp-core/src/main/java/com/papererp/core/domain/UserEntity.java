package com.papererp.core.domain;

import com.papererp.common.security.SystemRole;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_tenant_id", columnNames = {"tenant_id", "id"}),
        @UniqueConstraint(name = "uk_users_tenant_username", columnNames = {"tenant_id", "username"}),
        @UniqueConstraint(name = "uk_users_tenant_email", columnNames = {"tenant_id", "email"})
})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "default_plant_id")
    private UUID defaultPlantId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name_ar", nullable = false)
    private String fullNameAr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SystemRole role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UserEntity() {}

    public UserEntity(UUID tenantId, UUID defaultPlantId, String username, String email,
                      String passwordHash, String fullNameAr, SystemRole role) {
        this.tenantId = tenantId;
        this.defaultPlantId = defaultPlantId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullNameAr = fullNameAr;
        this.role = role;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getDefaultPlantId() { return defaultPlantId; }
    public void setDefaultPlantId(UUID defaultPlantId) { this.defaultPlantId = defaultPlantId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullNameAr() { return fullNameAr; }
    public void setFullNameAr(String fullNameAr) { this.fullNameAr = fullNameAr; }
    public SystemRole getRole() { return role; }
    public void setRole(SystemRole role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
