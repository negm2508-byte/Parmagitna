package com.papererp.core.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "paper_grades")
public class PaperGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(name = "name_ar", nullable = false, length = 100)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(nullable = false, length = 30)
    private String type; // FLUTING, TESTLINER, KRAFT, DUPLEX, OTHER

    @Column(name = "default_gsm_min", nullable = false, precision = 6, scale = 2)
    private BigDecimal defaultGsmMin;

    @Column(name = "default_gsm_max", nullable = false, precision = 6, scale = 2)
    private BigDecimal defaultGsmMax;

    @Column(name = "burst_factor_min", precision = 6, scale = 2)
    private BigDecimal burstFactorMin;

    @Column(name = "cobb_60_max", precision = 6, scale = 2)
    private BigDecimal cobb60Max;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public PaperGrade() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getDefaultGsmMin() { return defaultGsmMin; }
    public void setDefaultGsmMin(BigDecimal defaultGsmMin) { this.defaultGsmMin = defaultGsmMin; }

    public BigDecimal getDefaultGsmMax() { return defaultGsmMax; }
    public void setDefaultGsmMax(BigDecimal defaultGsmMax) { this.defaultGsmMax = defaultGsmMax; }

    public BigDecimal getBurstFactorMin() { return burstFactorMin; }
    public void setBurstFactorMin(BigDecimal burstFactorMin) { this.burstFactorMin = burstFactorMin; }

    public BigDecimal getCobb60Max() { return cobb60Max; }
    public void setCobb60Max(BigDecimal cobb60Max) { this.cobb60Max = cobb60Max; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
