package com.papererp.common.exception;

public class TenantAccessDeniedException extends DomainException {

    public TenantAccessDeniedException(String message) {
        super("TENANT_ACCESS_DENIED", message, 403);
    }
}
