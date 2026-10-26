package com.papererp.core.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "core_specifications", uniqueConstraints = {
        @UniqueConstraint(name = "uk_core_specs_tenant_id", columnNames = {"tenant_id", "id"}),
        @UniqueConstraint(name = "uk_core_specs_tenant_code", columnNames = {"tenant_id", "code"})
})
public class CoreSpecificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "diameter_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal diameterMm;

    @Column(name = "tare_weight_per_m_kg", nullable = false, precision = 8, scale = 4)
    private BigDecimal tareWeightPerMKg;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CoreSpecificationEntity() {}

    public CoreSpecificationEntity(UUID tenantId, String code, BigDecimal diameterMm, BigDecimal tareWeightPerMKg) {
        this.tenantId = tenantId;
        this.code = code;
        this.diameterMm = diameterMm;
        this.tareWeightPerMKg = tareWeightPerMKg;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BigDecimal getDiameterMm() { return diameterMm; }
    public void setDiameterMm(BigDecimal diameterMm) { this.diameterMm = diameterMm; }
    public BigDecimal getTareWeightPerMKg() { return tareWeightPerMKg; }
    public void setTareWeightPerMKg(BigDecimal tareWeightPerMKg) { this.tareWeightPerMKg = tareWeightPerMKg; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
