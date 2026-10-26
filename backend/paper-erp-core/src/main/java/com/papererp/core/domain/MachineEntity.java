package com.papererp.core.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machines", uniqueConstraints = {
        @UniqueConstraint(name = "uk_machines_tenant_id", columnNames = {"tenant_id", "id"}),
        @UniqueConstraint(name = "uk_machines_tenant_code", columnNames = {"tenant_id", "code"})
})
public class MachineEntity {

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

    @Column(name = "machine_type", nullable = false, length = 50)
    private String machineType; // PAPER_MACHINE, SLITTER_REWINDER

    @Column(name = "max_deckle_width_mm", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxDeckleWidthMm;

    @Column(name = "min_trim_width_mm", nullable = false, precision = 10, scale = 2)
    private BigDecimal minTrimWidthMm;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public MachineEntity() {}

    public MachineEntity(UUID tenantId, UUID plantId, String code, String nameAr, String machineType,
                         BigDecimal maxDeckleWidthMm, BigDecimal minTrimWidthMm) {
        this.tenantId = tenantId;
        this.plantId = plantId;
        this.code = code;
        this.nameAr = nameAr;
        this.machineType = machineType;
        this.maxDeckleWidthMm = maxDeckleWidthMm;
        this.minTrimWidthMm = minTrimWidthMm;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
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
    public String getMachineType() { return machineType; }
    public void setMachineType(String machineType) { this.machineType = machineType; }
    public BigDecimal getMaxDeckleWidthMm() { return maxDeckleWidthMm; }
    public void setMaxDeckleWidthMm(BigDecimal maxDeckleWidthMm) { this.maxDeckleWidthMm = maxDeckleWidthMm; }
    public BigDecimal getMinTrimWidthMm() { return minTrimWidthMm; }
    public void setMinTrimWidthMm(BigDecimal minTrimWidthMm) { this.minTrimWidthMm = minTrimWidthMm; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
