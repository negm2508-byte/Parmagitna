package com.papererp.core.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "slit_roll_id")
    private UUID slitRollId;

    @Column(name = "jumbo_roll_id")
    private UUID jumboRollId;

    @Column(name = "movement_type", nullable = false, length = 30)
    private String movementType; // PRODUCTION_RECEIPT, LOCATION_TRANSFER, STATUS_CHANGE, DISPATCH, RETURN, SCRAP

    @Column(name = "from_warehouse_id")
    private UUID fromWarehouseId;

    @Column(name = "to_warehouse_id")
    private UUID toWarehouseId;

    @Column(name = "from_location_id")
    private UUID fromLocationId;

    @Column(name = "to_location_id")
    private UUID toLocationId;

    @Column(name = "from_quality_status", length = 30)
    private String fromQualityStatus;

    @Column(name = "to_quality_status", length = 30)
    private String toQualityStatus;

    @Column(name = "net_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal netWeightKg;

    @Column(name = "reference_document", length = 50)
    private String referenceDocument;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public InventoryMovement() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getSlitRollId() { return slitRollId; }
    public void setSlitRollId(UUID slitRollId) { this.slitRollId = slitRollId; }

    public UUID getJumboRollId() { return jumboRollId; }
    public void setJumboRollId(UUID jumboRollId) { this.jumboRollId = jumboRollId; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public UUID getFromWarehouseId() { return fromWarehouseId; }
    public void setFromWarehouseId(UUID fromWarehouseId) { this.fromWarehouseId = fromWarehouseId; }

    public UUID getToWarehouseId() { return toWarehouseId; }
    public void setToWarehouseId(UUID toWarehouseId) { this.toWarehouseId = toWarehouseId; }

    public UUID getFromLocationId() { return fromLocationId; }
    public void setFromLocationId(UUID fromLocationId) { this.fromLocationId = fromLocationId; }

    public UUID getToLocationId() { return toLocationId; }
    public void setToLocationId(UUID toLocationId) { this.toLocationId = toLocationId; }

    public String getFromQualityStatus() { return fromQualityStatus; }
    public void setFromQualityStatus(String fromQualityStatus) { this.fromQualityStatus = fromQualityStatus; }

    public String getToQualityStatus() { return toQualityStatus; }
    public void setToQualityStatus(String toQualityStatus) { this.toQualityStatus = toQualityStatus; }

    public BigDecimal getNetWeightKg() { return netWeightKg; }
    public void setNetWeightKg(BigDecimal netWeightKg) { this.netWeightKg = netWeightKg; }

    public String getReferenceDocument() { return referenceDocument; }
    public void setReferenceDocument(String referenceDocument) { this.referenceDocument = referenceDocument; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
