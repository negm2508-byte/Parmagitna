package com.papererp.core.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "slitting_runs")
public class SlittingRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "parent_jumbo_id", nullable = false)
    private UUID parentJumboId;

    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Column(name = "input_jumbo_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal inputJumboWeightKg;

    @Column(name = "total_child_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalChildWeightKg;

    @Column(name = "trim_waste_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal trimWasteKg = BigDecimal.ZERO;

    @Column(name = "scale_variance_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal scaleVarianceKg = BigDecimal.ZERO;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime completedAt = OffsetDateTime.now();

    @Column(nullable = false, length = 30)
    private String status = "COMPLETED"; // COMPLETED, ABORTED

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public SlittingRun() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getParentJumboId() { return parentJumboId; }
    public void setParentJumboId(UUID parentJumboId) { this.parentJumboId = parentJumboId; }

    public UUID getMachineId() { return machineId; }
    public void setMachineId(UUID machineId) { this.machineId = machineId; }

    public UUID getOperatorId() { return operatorId; }
    public void setOperatorId(UUID operatorId) { this.operatorId = operatorId; }

    public BigDecimal getInputJumboWeightKg() { return inputJumboWeightKg; }
    public void setInputJumboWeightKg(BigDecimal inputJumboWeightKg) { this.inputJumboWeightKg = inputJumboWeightKg; }

    public BigDecimal getTotalChildWeightKg() { return totalChildWeightKg; }
    public void setTotalChildWeightKg(BigDecimal totalChildWeightKg) { this.totalChildWeightKg = totalChildWeightKg; }

    public BigDecimal getTrimWasteKg() { return trimWasteKg; }
    public void setTrimWasteKg(BigDecimal trimWasteKg) { this.trimWasteKg = trimWasteKg; }

    public BigDecimal getScaleVarianceKg() { return scaleVarianceKg; }
    public void setScaleVarianceKg(BigDecimal scaleVarianceKg) { this.scaleVarianceKg = scaleVarianceKg; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
