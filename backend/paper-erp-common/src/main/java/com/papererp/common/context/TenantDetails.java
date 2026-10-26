package com.papererp.common.context;

import com.papererp.common.security.SystemRole;

import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record TenantDetails(
        UUID tenantId,
        UUID defaultPlantId,
        UUID userId,
        String email,
        Set<SystemRole> roles
) implements Serializable {

    public TenantDetails {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        roles = roles == null ? Collections.emptySet() : Collections.unmodifiableSet(roles);
    }

    public boolean hasRole(SystemRole role) {
        return roles != null && roles.contains(role);
    }
}
