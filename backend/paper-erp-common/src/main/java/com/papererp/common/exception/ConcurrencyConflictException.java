package com.papererp.common.exception;

public class ConcurrencyConflictException extends DomainException {

    public ConcurrencyConflictException(String message) {
        super("CONCURRENCY_CONFLICT", message, 409);
    }
}
