package com.papererp.common.context;

import com.papererp.common.exception.TenantAccessDeniedException;

import java.util.Optional;
import java.util.UUID;

/**
 * ThreadLocal-based request-scoped tenant context.
 * Strict cleanup in finally blocks is mandatory.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantDetails> CONTEXT = new ThreadLocal<>();

    private TenantContext() {
        // Prevent instantiation
    }

    public static void set(TenantDetails details) {
        if (details == null) {
            clear();
        } else {
            CONTEXT.set(details);
        }
    }

    public static Optional<TenantDetails> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static UUID getRequiredTenantId() {
        return get()
                .map(TenantDetails::tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Tenant context is required but not established"));
    }

    public static UUID getRequiredUserId() {
        return get()
                .map(TenantDetails::userId)
                .orElseThrow(() -> new TenantAccessDeniedException("Authenticated user context is required but not established"));
    }

    public static Optional<UUID> getDefaultPlantId() {
        return get().map(TenantDetails::defaultPlantId);
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
