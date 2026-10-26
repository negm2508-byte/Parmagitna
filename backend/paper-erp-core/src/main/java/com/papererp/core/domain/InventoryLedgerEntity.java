package com.papererp.core.domain;

import com.papererp.common.domain.InventoryMovementType;
import com.papererp.common.domain.RollType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_ledger", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inv_ledger_tenant_id", columnNames = {"tenant_id", "id"})
})
public class InventoryLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plant_id", nullable = false)
    private UUID plantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private InventoryMovementType movementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "roll_type", nullable = false, length = 50)
    private RollType rollType;

    @Column(name = "jumbo_roll_id")
    private UUID jumboRollId;

    @Column(name = "slit_roll_id")
    private UUID slitRollId;

    @Column(name = "source_location_id")
    private UUID sourceLocationId;

    @Column(name = "destination_location_id")
    private UUID destinationLocationId;

    @Column(name = "quantity_change", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityChange;

    @Column(name = "weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(length = 255)
    private String reason;

    @Column(name = "movement_timestamp", nullable = false)
    private Instant movementTimestamp = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public InventoryLedgerEntity() {}

    public static InventoryLedgerEntity forJumbo(
            UUID tenantId, UUID plantId, InventoryMovementType movementType, UUID jumboRollId,
            UUID sourceLocationId, UUID destinationLocationId, BigDecimal quantityChange,
            BigDecimal weightKg, UUID actorId, String referenceType, UUID referenceId, String reason) {
        InventoryLedgerEntity entry = new InventoryLedgerEntity();
        entry.tenantId = tenantId;
        entry.plantId = plantId;
        entry.movementType = movementType;
        entry.rollType = RollType.JUMBO_ROLL;
        entry.jumboRollId = jumboRollId;
        entry.slitRollId = null;
        entry.sourceLocationId = sourceLocationId;
        entry.destinationLocationId = destinationLocationId;
        entry.quantityChange = quantityChange;
        entry.weightKg = weightKg;
        entry.actorId = actorId;
        entry.referenceType = referenceType;
        entry.referenceId = referenceId;
        entry.reason = reason;
        entry.movementTimestamp = Instant.now();
        entry.createdAt = Instant.now();
        return entry;
    }

    public static InventoryLedgerEntity forSlit(
            UUID tenantId, UUID plantId, InventoryMovementType movementType, UUID slitRollId,
            UUID sourceLocationId, UUID destinationLocationId, BigDecimal quantityChange,
            BigDecimal weightKg, UUID actorId, String referenceType, UUID referenceId, String reason) {
        InventoryLedgerEntity entry = new InventoryLedgerEntity();
        entry.tenantId = tenantId;
        entry.plantId = plantId;
        entry.movementType = movementType;
        entry.rollType = RollType.SLIT_ROLL;
        entry.jumboRollId = null;
        entry.slitRollId = slitRollId;
        entry.sourceLocationId = sourceLocationId;
        entry.destinationLocationId = destinationLocationId;
        entry.quantityChange = quantityChange;
        entry.weightKg = weightKg;
        entry.actorId = actorId;
        entry.referenceType = referenceType;
        entry.referenceId = referenceId;
        entry.reason = reason;
        entry.movementTimestamp = Instant.now();
        entry.createdAt = Instant.now();
        return entry;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getPlantId() { return plantId; }
    public InventoryMovementType getMovementType() { return movementType; }
    public RollType getRollType() { return rollType; }
    public UUID getJumboRollId() { return jumboRollId; }
    public UUID getSlitRollId() { return slitRollId; }
    public UUID getSourceLocationId() { return sourceLocationId; }
    public UUID getDestinationLocationId() { return destinationLocationId; }
    public BigDecimal getQuantityChange() { return quantityChange; }
    public BigDecimal getWeightKg() { return weightKg; }
    public UUID getActorId() { return actorId; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public String getReason() { return reason; }
    public Instant getMovementTimestamp() { return movementTimestamp; }
    public Instant getCreatedAt() { return createdAt; }
}
