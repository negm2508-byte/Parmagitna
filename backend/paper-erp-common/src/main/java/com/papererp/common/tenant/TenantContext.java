package com.papererp.common.tenant;

import java.util.UUID;

/**
 * ThreadLocal holder for the current request's Tenant and Plant context.
 * Enforces multi-tenant data isolation across all service and repository layers.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_PLANT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getPlantId() {
        return CURRENT_PLANT.get();
    }

    public static void setPlantId(UUID plantId) {
        CURRENT_PLANT.set(plantId);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_PLANT.remove();
    }
}
