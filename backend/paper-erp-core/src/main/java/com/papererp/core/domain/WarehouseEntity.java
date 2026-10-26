package com.papererp.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "warehouses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_warehouses_tenant_id", columnNames = {"tenant_id", "id"}),
        @UniqueConstraint(name = "uk_warehouses_tenant_code", columnNames = {"tenant_id", "plant_id", "code"})
})
public class WarehouseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plant_id", nullable = false)
    private UUID plantId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "name_ar", nullable = false)
    private String nameAr;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public WarehouseEntity() {}

    public WarehouseEntity(UUID tenantId, UUID plantId, String code, String nameAr) {
        this.tenantId = tenantId;
        this.plantId = plantId;
        this.code = code;
        this.nameAr = nameAr;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getPlantId() { return plantId; }
    public void setPlantId(UUID plantId) { this.plantId = plantId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
