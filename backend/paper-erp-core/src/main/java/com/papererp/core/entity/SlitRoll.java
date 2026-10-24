package com.papererp.core.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "slit_rolls")
public class SlitRoll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "parent_jumbo_id", nullable = false)
    private UUID parentJumboId;

    @Column(name = "paper_grade_id", nullable = false)
    private UUID paperGradeId;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "serial_number", nullable = false, length = 50)
    private String serialNumber;

    @Column(nullable = false, length = 100)
    private String barcode;

    @Column(name = "cut_sequence", nullable = false)
    private Integer cutSequence;

    @Column(name = "width_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal widthMm;

    @Column(name = "diameter_mm", precision = 8, scale = 2)
    private BigDecimal diameterMm;

    @Column(name = "core_diameter_mm", nullable = false, precision = 6, scale = 2)
    private BigDecimal coreDiameterMm = new BigDecimal("76.0");

    @Column(name = "measured_gsm", nullable = false, precision = 6, scale = 2)
    private BigDecimal measuredGsm;

    @Column(name = "gross_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossWeightKg;

    @Column(name = "tare_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal tareWeightKg = new BigDecimal("2.00");

    @Column(name = "net_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal netWeightKg;

    @Column(name = "quality_status", nullable = false, length = 30)
    private String qualityStatus = "PENDING_INSPECTION"; // PENDING_INSPECTION, RELEASED, QUARANTINE, REJECTED, DOWNGRADED

    @Column(name = "inventory_status", nullable = false, length = 30)
    private String inventoryStatus = "IN_STOCK"; // IN_STOCK, RESERVED, DISPATCHED, CONSUMED_REPROCESS, SCRAPPED

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "slitted_at", nullable = false)
    private OffsetDateTime slittedAt = OffsetDateTime.now();

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Column(name = "slitting_run_id")
    private UUID slittingRunId;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public SlitRoll() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getParentJumboId() { return parentJumboId; }
    public void setParentJumboId(UUID parentJumboId) { this.parentJumboId = parentJumboId; }

    public UUID getPaperGradeId() { return paperGradeId; }
    public void setPaperGradeId(UUID paperGradeId) { this.paperGradeId = paperGradeId; }

    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }

    public UUID getLocationId() { return locationId; }
    public void setLocationId(UUID locationId) { this.locationId = locationId; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public Integer getCutSequence() { return cutSequence; }
    public void setCutSequence(Integer cutSequence) { this.cutSequence = cutSequence; }

    public BigDecimal getWidthMm() { return widthMm; }
    public void setWidthMm(BigDecimal widthMm) { this.widthMm = widthMm; }

    public BigDecimal getDiameterMm() { return diameterMm; }
    public void setDiameterMm(BigDecimal diameterMm) { this.diameterMm = diameterMm; }

    public BigDecimal getCoreDiameterMm() { return coreDiameterMm; }
    public void setCoreDiameterMm(BigDecimal coreDiameterMm) { this.coreDiameterMm = coreDiameterMm; }

    public BigDecimal getMeasuredGsm() { return measuredGsm; }
    public void setMeasuredGsm(BigDecimal measuredGsm) { this.measuredGsm = measuredGsm; }

    public BigDecimal getGrossWeightKg() { return grossWeightKg; }
    public void setGrossWeightKg(BigDecimal grossWeightKg) { this.grossWeightKg = grossWeightKg; }

    public BigDecimal getTareWeightKg() { return tareWeightKg; }
    public void setTareWeightKg(BigDecimal tareWeightKg) { this.tareWeightKg = tareWeightKg; }

    public BigDecimal getNetWeightKg() { return netWeightKg; }
    public void setNetWeightKg(BigDecimal netWeightKg) { this.netWeightKg = netWeightKg; }

    public String getQualityStatus() { return qualityStatus; }
    public void setQualityStatus(String qualityStatus) { this.qualityStatus = qualityStatus; }

    public String getInventoryStatus() { return inventoryStatus; }
    public void setInventoryStatus(String inventoryStatus) { this.inventoryStatus = inventoryStatus; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public OffsetDateTime getSlittedAt() { return slittedAt; }
    public void setSlittedAt(OffsetDateTime slittedAt) { this.slittedAt = slittedAt; }

    public UUID getOperatorId() { return operatorId; }
    public void setOperatorId(UUID operatorId) { this.operatorId = operatorId; }

    public UUID getSlittingRunId() { return slittingRunId; }
    public void setSlittingRunId(UUID slittingRunId) { this.slittingRunId = slittingRunId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
