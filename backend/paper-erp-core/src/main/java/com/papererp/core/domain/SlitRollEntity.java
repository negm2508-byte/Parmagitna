package com.papererp.core.domain;

import com.papererp.common.domain.QualityDisposition;
import com.papererp.common.domain.RollStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "slit_rolls", uniqueConstraints = {
        @UniqueConstraint(name = "uk_slit_rolls_tenant_id", columnNames = {"tenant_id", "id"}),
        @UniqueConstraint(name = "uk_slit_rolls_tenant_serial", columnNames = {"tenant_id", "serial_number"}),
        @UniqueConstraint(name = "uk_slit_rolls_tenant_barcode", columnNames = {"tenant_id", "barcode"})
})
public class SlitRollEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plant_id", nullable = false)
    private UUID plantId;

    @Column(name = "parent_jumbo_id", nullable = false)
    private UUID parentJumboId;

    @Column(name = "slitting_run_id", nullable = false)
    private UUID slittingRunId;

    @Column(name = "grade_id", nullable = false)
    private UUID gradeId;

    @Column(name = "core_spec_id")
    private UUID coreSpecId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "serial_number", nullable = false, length = 100)
    private String serialNumber;

    @Column(nullable = false, length = 100)
    private String barcode;

    @Column(name = "cut_sequence_number", nullable = false)
    private int cutSequenceNumber;

    @Column(name = "cut_width_mm", nullable = false, precision = 10, scale = 2)
    private BigDecimal cutWidthMm;

    @Column(name = "diameter_mm", precision = 10, scale = 2)
    private BigDecimal diameterMm;

    @Column(name = "gross_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossWeightKg;

    @Column(name = "tare_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal tareWeightKg;

    @Column(name = "net_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal netWeightKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RollStatus status = RollStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_disposition", nullable = false, length = 50)
    private QualityDisposition qualityDisposition = QualityDisposition.PENDING;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public SlitRollEntity() {}

    public SlitRollEntity(UUID tenantId, UUID plantId, UUID parentJumboId, UUID slittingRunId,
                          UUID gradeId, UUID coreSpecId, String serialNumber, String barcode,
                          int cutSequenceNumber, BigDecimal cutWidthMm, BigDecimal diameterMm,
                          BigDecimal grossWeightKg, BigDecimal tareWeightKg, BigDecimal netWeightKg,
                          UUID createdBy) {
        this.tenantId = tenantId;
        this.plantId = plantId;
        this.parentJumboId = parentJumboId;
        this.slittingRunId = slittingRunId;
        this.gradeId = gradeId;
        this.coreSpecId = coreSpecId;
        this.serialNumber = serialNumber;
        this.barcode = barcode;
        this.cutSequenceNumber = cutSequenceNumber;
        this.cutWidthMm = cutWidthMm;
        this.diameterMm = diameterMm;
        this.grossWeightKg = grossWeightKg;
        this.tareWeightKg = tareWeightKg;
        this.netWeightKg = netWeightKg;
        this.createdBy = createdBy;
        this.status = RollStatus.AVAILABLE;
        this.qualityDisposition = QualityDisposition.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getPlantId() { return plantId; }
    public void setPlantId(UUID plantId) { this.plantId = plantId; }
    public UUID getParentJumboId() { return parentJumboId; }
    public void setParentJumboId(UUID parentJumboId) { this.parentJumboId = parentJumboId; }
    public UUID getSlittingRunId() { return slittingRunId; }
    public void setSlittingRunId(UUID slittingRunId) { this.slittingRunId = slittingRunId; }
    public UUID getGradeId() { return gradeId; }
    public void setGradeId(UUID gradeId) { this.gradeId = gradeId; }
    public UUID getCoreSpecId() { return coreSpecId; }
    public void setCoreSpecId(UUID coreSpecId) { this.coreSpecId = coreSpecId; }
    public UUID getLocationId() { return locationId; }
    public void setLocationId(UUID locationId) { this.locationId = locationId; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public int getCutSequenceNumber() { return cutSequenceNumber; }
    public void setCutSequenceNumber(int cutSequenceNumber) { this.cutSequenceNumber = cutSequenceNumber; }
    public BigDecimal getCutWidthMm() { return cutWidthMm; }
    public void setCutWidthMm(BigDecimal cutWidthMm) { this.cutWidthMm = cutWidthMm; }
    public BigDecimal getDiameterMm() { return diameterMm; }
    public void setDiameterMm(BigDecimal diameterMm) { this.diameterMm = diameterMm; }
    public BigDecimal getGrossWeightKg() { return grossWeightKg; }
    public void setGrossWeightKg(BigDecimal grossWeightKg) { this.grossWeightKg = grossWeightKg; }
    public BigDecimal getTareWeightKg() { return tareWeightKg; }
    public void setTareWeightKg(BigDecimal tareWeightKg) { this.tareWeightKg = tareWeightKg; }
    public BigDecimal getNetWeightKg() { return netWeightKg; }
    public void setNetWeightKg(BigDecimal netWeightKg) { this.netWeightKg = netWeightKg; }
    public RollStatus getStatus() { return status; }
    public void setStatus(RollStatus status) { this.status = status; }
    public QualityDisposition getQualityDisposition() { return qualityDisposition; }
    public void setQualityDisposition(QualityDisposition qualityDisposition) { this.qualityDisposition = qualityDisposition; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
}
