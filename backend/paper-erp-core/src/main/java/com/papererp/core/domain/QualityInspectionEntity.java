package com.papererp.core.domain;

import com.papererp.common.domain.QualityDisposition;
import com.papererp.common.domain.RollType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quality_inspections", uniqueConstraints = {
        @UniqueConstraint(name = "uk_quality_insp_tenant_id", columnNames = {"tenant_id", "id"})
})
public class QualityInspectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plant_id", nullable = false)
    private UUID plantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "roll_type", nullable = false, length = 50)
    private RollType rollType;

    @Column(name = "jumbo_roll_id")
    private UUID jumboRollId;

    @Column(name = "slit_roll_id")
    private UUID slitRollId;

    @Column(name = "inspector_id", nullable = false)
    private UUID inspectorId;

    @Column(name = "measured_gsm", precision = 8, scale = 2)
    private BigDecimal measuredGsm;

    @Column(name = "measured_moisture_pct", precision = 5, scale = 2)
    private BigDecimal measuredMoisturePct;

    @Column(name = "measured_caliper_um", precision = 8, scale = 2)
    private BigDecimal measuredCaliperUm;

    @Column(name = "measured_burst_strength", precision = 8, scale = 2)
    private BigDecimal measuredBurstStrength;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private QualityDisposition disposition;

    @Column(length = 500)
    private String notes;

    @Column(name = "inspection_timestamp", nullable = false)
    private Instant inspectionTimestamp = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public QualityInspectionEntity() {}

    public QualityInspectionEntity(UUID tenantId, UUID plantId, RollType rollType, UUID jumboRollId,
                                   UUID slitRollId, UUID inspectorId, BigDecimal measuredGsm,
                                   BigDecimal measuredMoisturePct, BigDecimal measuredCaliperUm,
                                   BigDecimal measuredBurstStrength, QualityDisposition disposition, String notes) {
        this.tenantId = tenantId;
        this.plantId = plantId;
        this.rollType = rollType;
        this.jumboRollId = jumboRollId;
        this.slitRollId = slitRollId;
        this.inspectorId = inspectorId;
        this.measuredGsm = measuredGsm;
        this.measuredMoisturePct = measuredMoisturePct;
        this.measuredCaliperUm = measuredCaliperUm;
        this.measuredBurstStrength = measuredBurstStrength;
        this.disposition = disposition;
        this.notes = notes;
        this.inspectionTimestamp = Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getPlantId() { return plantId; }
    public void setPlantId(UUID plantId) { this.plantId = plantId; }
    public RollType getRollType() { return rollType; }
    public void setRollType(RollType rollType) { this.rollType = rollType; }
    public UUID getJumboRollId() { return jumboRollId; }
    public void setJumboRollId(UUID jumboRollId) { this.jumboRollId = jumboRollId; }
    public UUID getSlitRollId() { return slitRollId; }
    public void setSlitRollId(UUID slitRollId) { this.slitRollId = slitRollId; }
    public UUID getInspectorId() { return inspectorId; }
    public void setInspectorId(UUID inspectorId) { this.inspectorId = inspectorId; }
    public BigDecimal getMeasuredGsm() { return measuredGsm; }
    public void setMeasuredGsm(BigDecimal measuredGsm) { this.measuredGsm = measuredGsm; }
    public BigDecimal getMeasuredMoisturePct() { return measuredMoisturePct; }
    public void setMeasuredMoisturePct(BigDecimal measuredMoisturePct) { this.measuredMoisturePct = measuredMoisturePct; }
    public BigDecimal getMeasuredCaliperUm() { return measuredCaliperUm; }
    public void setMeasuredCaliperUm(BigDecimal measuredCaliperUm) { this.measuredCaliperUm = measuredCaliperUm; }
    public BigDecimal getMeasuredBurstStrength() { return measuredBurstStrength; }
    public void setMeasuredBurstStrength(BigDecimal measuredBurstStrength) { this.measuredBurstStrength = measuredBurstStrength; }
    public QualityDisposition getDisposition() { return disposition; }
    public void setDisposition(QualityDisposition disposition) { this.disposition = disposition; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getInspectionTimestamp() { return inspectionTimestamp; }
    public Instant getCreatedAt() { return createdAt; }
}
