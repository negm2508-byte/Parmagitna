package com.papererp.core.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "manufacturing_rules", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mfg_rules_tenant_key", columnNames = {"tenant_id", "plant_id", "rule_key"})
})
public class ManufacturingRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plant_id")
    private UUID plantId;

    @Column(name = "rule_key", nullable = false, length = 100)
    private String ruleKey;

    @Column(name = "rule_value_numeric", precision = 12, scale = 4)
    private BigDecimal ruleValueNumeric;

    @Column(name = "rule_value_text")
    private String ruleValueText;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ManufacturingRuleEntity() {}

    public ManufacturingRuleEntity(UUID tenantId, UUID plantId, String ruleKey,
                                   BigDecimal ruleValueNumeric, String ruleValueText, String description) {
        this.tenantId = tenantId;
        this.plantId = plantId;
        this.ruleKey = ruleKey;
        this.ruleValueNumeric = ruleValueNumeric;
        this.ruleValueText = ruleValueText;
        this.description = description;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getPlantId() { return plantId; }
    public void setPlantId(UUID plantId) { this.plantId = plantId; }
    public String getRuleKey() { return ruleKey; }
    public void setRuleKey(String ruleKey) { this.ruleKey = ruleKey; }
    public BigDecimal getRuleValueNumeric() { return ruleValueNumeric; }
    public void setRuleValueNumeric(BigDecimal ruleValueNumeric) { this.ruleValueNumeric = ruleValueNumeric; }
    public String getRuleValueText() { return ruleValueText; }
    public void setRuleValueText(String ruleValueText) { this.ruleValueText = ruleValueText; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
