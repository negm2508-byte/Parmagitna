package com.papererp.core.domain;

import com.papererp.common.domain.QualityDisposition;
import com.papererp.common.domain.RollStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jumbo_rolls", uniqueConstraints = {
        @UniqueConstraint(name = "uk_jumbo_rolls_tenant_id", columnNames = {"tenant_id", "id"}),
        @UniqueConstraint(name = "uk_jumbo_rolls_tenant_serial", columnNames = {"tenant_id", "serial_number"}),
        @UniqueConstraint(name = "uk_jumbo_rolls_tenant_barcode", columnNames = {"tenant_id", "barcode"})
})
public class JumboRollEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plant_id", nullable = false)
    private UUID plantId;

    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

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

    @Column(name = "gross_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossWeightKg;

    @Column(name = "tare_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal tareWeightKg;

    @Column(name = "net_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal netWeightKg;

    @Column(name = "deckle_width_mm", nullable = false, precision = 10, scale = 2)
    private BigDecimal deckleWidthMm;

    @Column(name = "diameter_mm", precision = 10, scale = 2)
    private BigDecimal diameterMm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RollStatus status = RollStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_disposition", nullable = false, length = 50)
    private QualityDisposition qualityDisposition = QualityDisposition.PENDING;

    @Column(name = "production_timestamp", nullable = false)
    private Instant productionTimestamp = Instant.now();

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public JumboRollEntity() {}

    public JumboRollEntity(UUID tenantId, UUID plantId, UUID machineId, UUID gradeId, UUID coreSpecId,
                           String serialNumber, String barcode, BigDecimal grossWeightKg,
                           BigDecimal tareWeightKg, BigDecimal netWeightKg, BigDecimal deckleWidthMm,
                           BigDecimal diameterMm, UUID createdBy) {
        this.tenantId = tenantId;
        this.plantId = plantId;
        this.machineId = machineId;
        this.gradeId = gradeId;
        this.coreSpecId = coreSpecId;
        this.serialNumber = serialNumber;
        this.barcode = barcode;
        this.grossWeightKg = grossWeightKg;
        this.tareWeightKg = tareWeightKg;
        this.netWeightKg = netWeightKg;
        this.deckleWidthMm = deckleWidthMm;
        this.diameterMm = diameterMm;
        this.createdBy = createdBy;
        this.status = RollStatus.AVAILABLE;
        this.qualityDisposition = QualityDisposition.PENDING;
        this.productionTimestamp = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getPlantId() { return plantId; }
    public void setPlantId(UUID plantId) { this.plantId = plantId; }
    public UUID getMachineId() { return machineId; }
    public void setMachineId(UUID machineId) { this.machineId = machineId; }
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
    public BigDecimal getGrossWeightKg() { return grossWeightKg; }
    public void setGrossWeightKg(BigDecimal grossWeightKg) { this.grossWeightKg = grossWeightKg; }
    public BigDecimal getTareWeightKg() { return tareWeightKg; }
    public void setTareWeightKg(BigDecimal tareWeightKg) { this.tareWeightKg = tareWeightKg; }
    public BigDecimal getNetWeightKg() { return netWeightKg; }
    public void setNetWeightKg(BigDecimal netWeightKg) { this.netWeightKg = netWeightKg; }
    public BigDecimal getDeckleWidthMm() { return deckleWidthMm; }
    public void setDeckleWidthMm(BigDecimal deckleWidthMm) { this.deckleWidthMm = deckleWidthMm; }
    public BigDecimal getDiameterMm() { return diameterMm; }
    public void setDiameterMm(BigDecimal diameterMm) { this.diameterMm = diameterMm; }
    public RollStatus getStatus() { return status; }
    public void setStatus(RollStatus status) { this.status = status; }
    public QualityDisposition getQualityDisposition() { return qualityDisposition; }
    public void setQualityDisposition(QualityDisposition qualityDisposition) { this.qualityDisposition = qualityDisposition; }
    public Instant getProductionTimestamp() { return productionTimestamp; }
    public void setProductionTimestamp(Instant productionTimestamp) { this.productionTimestamp = productionTimestamp; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
}
