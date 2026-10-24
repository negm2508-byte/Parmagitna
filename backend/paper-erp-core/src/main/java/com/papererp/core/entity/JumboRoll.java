package com.papererp.core.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "jumbo_rolls")
public class JumboRoll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plant_id", nullable = false)
    private UUID plantId;

    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Column(name = "paper_grade_id", nullable = false)
    private UUID paperGradeId;

    @Column(name = "serial_number", nullable = false, length = 50)
    private String serialNumber;

    @Column(name = "target_gsm", nullable = false, precision = 6, scale = 2)
    private BigDecimal targetGsm;

    @Column(name = "deckle_width_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal deckleWidthMm;

    @Column(name = "gross_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossWeightKg;

    @Column(name = "net_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal netWeightKg;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE_WIP"; // ACTIVE_WIP, FULLY_SLIT, SCRAPPED

    @Column(name = "manufactured_at", nullable = false)
    private OffsetDateTime manufacturedAt = OffsetDateTime.now();

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public JumboRoll() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getPlantId() { return plantId; }
    public void setPlantId(UUID plantId) { this.plantId = plantId; }

    public UUID getMachineId() { return machineId; }
    public void setMachineId(UUID machineId) { this.machineId = machineId; }

    public UUID getPaperGradeId() { return paperGradeId; }
    public void setPaperGradeId(UUID paperGradeId) { this.paperGradeId = paperGradeId; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public BigDecimal getTargetGsm() { return targetGsm; }
    public void setTargetGsm(BigDecimal targetGsm) { this.targetGsm = targetGsm; }

    public BigDecimal getDeckleWidthMm() { return deckleWidthMm; }
    public void setDeckleWidthMm(BigDecimal deckleWidthMm) { this.deckleWidthMm = deckleWidthMm; }

    public BigDecimal getGrossWeightKg() { return grossWeightKg; }
    public void setGrossWeightKg(BigDecimal grossWeightKg) { this.grossWeightKg = grossWeightKg; }

    public BigDecimal getNetWeightKg() { return netWeightKg; }
    public void setNetWeightKg(BigDecimal netWeightKg) { this.netWeightKg = netWeightKg; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getManufacturedAt() { return manufacturedAt; }
    public void setManufacturedAt(OffsetDateTime manufacturedAt) { this.manufacturedAt = manufacturedAt; }

    public UUID getOperatorId() { return operatorId; }
    public void setOperatorId(UUID operatorId) { this.operatorId = operatorId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
