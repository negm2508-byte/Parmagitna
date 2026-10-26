package com.papererp.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "warehouse_locations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wh_locations_tenant_id", columnNames = {"tenant_id", "id"}),
        @UniqueConstraint(name = "uk_wh_locations_code", columnNames = {"tenant_id", "warehouse_id", "code"})
})
public class WarehouseLocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(length = 20)
    private String aisle;

    @Column(name = "row_number", length = 20)
    private String rowNumber;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public WarehouseLocationEntity() {}

    public WarehouseLocationEntity(UUID tenantId, UUID warehouseId, String code, String aisle, String rowNumber) {
        this.tenantId = tenantId;
        this.warehouseId = warehouseId;
        this.code = code;
        this.aisle = aisle;
        this.rowNumber = rowNumber;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getAisle() { return aisle; }
    public void setAisle(String aisle) { this.aisle = aisle; }
    public String getRowNumber() { return rowNumber; }
    public void setRowNumber(String rowNumber) { this.rowNumber = rowNumber; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
