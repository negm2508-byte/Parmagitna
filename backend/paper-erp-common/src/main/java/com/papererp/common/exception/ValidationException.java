package com.papererp.common.exception;

import java.util.Collections;
import java.util.Map;

public class ValidationException extends DomainException {

    private final Map<String, String> errors;

    public ValidationException(String message) {
        this(message, Collections.emptyMap());
    }

    public ValidationException(String message, Map<String, String> errors) {
        super("VALIDATION_FAILED", message, 422);
        this.errors = errors != null ? Collections.unmodifiableMap(errors) : Collections.emptyMap();
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
