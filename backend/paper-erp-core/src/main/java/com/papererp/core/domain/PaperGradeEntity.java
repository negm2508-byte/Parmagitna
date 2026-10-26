package com.papererp.core.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "paper_grades", uniqueConstraints = {
        @UniqueConstraint(name = "uk_paper_grades_tenant_id", columnNames = {"tenant_id", "id"}),
        @UniqueConstraint(name = "uk_paper_grades_tenant_code", columnNames = {"tenant_id", "code"})
})
public class PaperGradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "name_ar", nullable = false)
    private String nameAr;

    @Column(name = "gsm_target", nullable = false, precision = 8, scale = 2)
    private BigDecimal gsmTarget;

    @Column(name = "gsm_min", nullable = false, precision = 8, scale = 2)
    private BigDecimal gsmMin;

    @Column(name = "gsm_max", nullable = false, precision = 8, scale = 2)
    private BigDecimal gsmMax;

    @Column(name = "caliper_target_um", nullable = false, precision = 8, scale = 2)
    private BigDecimal caliperTargetUm;

    @Column(name = "moisture_target_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal moistureTargetPct;

    @Column(name = "burst_index_target", nullable = false, precision = 8, scale = 2)
    private BigDecimal burstIndexTarget;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public PaperGradeEntity() {}

    public PaperGradeEntity(UUID tenantId, String code, String nameAr, BigDecimal gsmTarget,
                            BigDecimal gsmMin, BigDecimal gsmMax, BigDecimal caliperTargetUm,
                            BigDecimal moistureTargetPct, BigDecimal burstIndexTarget) {
        this.tenantId = tenantId;
        this.code = code;
        this.nameAr = nameAr;
        this.gsmTarget = gsmTarget;
        this.gsmMin = gsmMin;
        this.gsmMax = gsmMax;
        this.caliperTargetUm = caliperTargetUm;
        this.moistureTargetPct = moistureTargetPct;
        this.burstIndexTarget = burstIndexTarget;
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
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public BigDecimal getGsmTarget() { return gsmTarget; }
    public void setGsmTarget(BigDecimal gsmTarget) { this.gsmTarget = gsmTarget; }
    public BigDecimal getGsmMin() { return gsmMin; }
    public void setGsmMin(BigDecimal gsmMin) { this.gsmMin = gsmMin; }
    public BigDecimal getGsmMax() { return gsmMax; }
    public void setGsmMax(BigDecimal gsmMax) { this.gsmMax = gsmMax; }
    public BigDecimal getCaliperTargetUm() { return caliperTargetUm; }
    public void setCaliperTargetUm(BigDecimal caliperTargetUm) { this.caliperTargetUm = caliperTargetUm; }
    public BigDecimal getMoistureTargetPct() { return moistureTargetPct; }
    public void setMoistureTargetPct(BigDecimal moistureTargetPct) { this.moistureTargetPct = moistureTargetPct; }
    public BigDecimal getBurstIndexTarget() { return burstIndexTarget; }
    public void setBurstIndexTarget(BigDecimal burstIndexTarget) { this.burstIndexTarget = burstIndexTarget; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
