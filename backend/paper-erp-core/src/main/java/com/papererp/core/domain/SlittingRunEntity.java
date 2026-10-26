package com.papererp.core.domain;

import com.papererp.common.domain.MassBalanceStatus;
import com.papererp.common.domain.SlittingRunStatus;
import com.papererp.common.domain.WasteDisposition;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "slitting_runs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_slitting_runs_tenant_id", columnNames = {"tenant_id", "id"}),
        @UniqueConstraint(name = "uk_slitting_runs_tenant_idempotency", columnNames = {"tenant_id", "idempotency_key"})
})
public class SlittingRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plant_id", nullable = false)
    private UUID plantId;

    @Column(name = "parent_jumbo_id", nullable = false)
    private UUID parentJumboId;

    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "input_gross_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal inputGrossWeightKg;

    @Column(name = "input_tare_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal inputTareWeightKg;

    @Column(name = "input_net_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal inputNetWeightKg;

    @Column(name = "input_deckle_width_mm", nullable = false, precision = 10, scale = 2)
    private BigDecimal inputDeckleWidthMm;

    @Column(name = "child_rolls_total_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal childRollsTotalWeightKg;

    @Column(name = "recorded_trim_waste_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal recordedTrimWasteKg;

    @Column(name = "scale_variance_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal scaleVarianceKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "mass_balance_status", nullable = false, length = 50)
    private MassBalanceStatus massBalanceStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "waste_disposition", nullable = false, length = 50)
    private WasteDisposition wasteDisposition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SlittingRunStatus status = SlittingRunStatus.COMPLETED;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SlittingRunEntity() {}

    public SlittingRunEntity(UUID tenantId, UUID plantId, UUID parentJumboId, UUID machineId,
                             UUID operatorId, String idempotencyKey, BigDecimal inputGrossWeightKg,
                             BigDecimal inputTareWeightKg, BigDecimal inputNetWeightKg,
                             BigDecimal inputDeckleWidthMm, BigDecimal childRollsTotalWeightKg,
                             BigDecimal recordedTrimWasteKg, BigDecimal scaleVarianceKg,
                             MassBalanceStatus massBalanceStatus, WasteDisposition wasteDisposition) {
        this.tenantId = tenantId;
        this.plantId = plantId;
        this.parentJumboId = parentJumboId;
        this.machineId = machineId;
        this.operatorId = operatorId;
        this.idempotencyKey = idempotencyKey;
        this.inputGrossWeightKg = inputGrossWeightKg;
        this.inputTareWeightKg = inputTareWeightKg;
        this.inputNetWeightKg = inputNetWeightKg;
        this.inputDeckleWidthMm = inputDeckleWidthMm;
        this.childRollsTotalWeightKg = childRollsTotalWeightKg;
        this.recordedTrimWasteKg = recordedTrimWasteKg;
        this.scaleVarianceKg = scaleVarianceKg;
        this.massBalanceStatus = massBalanceStatus;
        this.wasteDisposition = wasteDisposition;
        this.status = SlittingRunStatus.COMPLETED;
        this.startedAt = Instant.now();
        this.completedAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getPlantId() { return plantId; }
    public void setPlantId(UUID plantId) { this.plantId = plantId; }
    public UUID getParentJumboId() { return parentJumboId; }
    public void setParentJumboId(UUID parentJumboId) { this.parentJumboId = parentJumboId; }
    public UUID getMachineId() { return machineId; }
    public void setMachineId(UUID machineId) { this.machineId = machineId; }
    public UUID getOperatorId() { return operatorId; }
    public void setOperatorId(UUID operatorId) { this.operatorId = operatorId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public BigDecimal getInputGrossWeightKg() { return inputGrossWeightKg; }
    public void setInputGrossWeightKg(BigDecimal inputGrossWeightKg) { this.inputGrossWeightKg = inputGrossWeightKg; }
    public BigDecimal getInputTareWeightKg() { return inputTareWeightKg; }
    public void setInputTareWeightKg(BigDecimal inputTareWeightKg) { this.inputTareWeightKg = inputTareWeightKg; }
    public BigDecimal getInputNetWeightKg() { return inputNetWeightKg; }
    public void setInputNetWeightKg(BigDecimal inputNetWeightKg) { this.inputNetWeightKg = inputNetWeightKg; }
    public BigDecimal getInputDeckleWidthMm() { return inputDeckleWidthMm; }
    public void setInputDeckleWidthMm(BigDecimal inputDeckleWidthMm) { this.inputDeckleWidthMm = inputDeckleWidthMm; }
    public BigDecimal getChildRollsTotalWeightKg() { return childRollsTotalWeightKg; }
    public void setChildRollsTotalWeightKg(BigDecimal childRollsTotalWeightKg) { this.childRollsTotalWeightKg = childRollsTotalWeightKg; }
    public BigDecimal getRecordedTrimWasteKg() { return recordedTrimWasteKg; }
    public void setRecordedTrimWasteKg(BigDecimal recordedTrimWasteKg) { this.recordedTrimWasteKg = recordedTrimWasteKg; }
    public BigDecimal getScaleVarianceKg() { return scaleVarianceKg; }
    public void setScaleVarianceKg(BigDecimal scaleVarianceKg) { this.scaleVarianceKg = scaleVarianceKg; }
    public MassBalanceStatus getMassBalanceStatus() { return massBalanceStatus; }
    public void setMassBalanceStatus(MassBalanceStatus massBalanceStatus) { this.massBalanceStatus = massBalanceStatus; }
    public WasteDisposition getWasteDisposition() { return wasteDisposition; }
    public void setWasteDisposition(WasteDisposition wasteDisposition) { this.wasteDisposition = wasteDisposition; }
    public SlittingRunStatus getStatus() { return status; }
    public void setStatus(SlittingRunStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
