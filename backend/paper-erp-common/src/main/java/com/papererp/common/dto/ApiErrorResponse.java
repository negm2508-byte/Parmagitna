package com.papererp.common.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String correlationId,
        Map<String, String> validationErrors
) {
    public static ApiErrorResponse of(
            int status,
            String errorCode,
            String message,
            String path,
            String correlationId) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                errorCode,
                message,
                path,
                correlationId,
                Collections.emptyMap()
        );
    }

    public static ApiErrorResponse ofValidation(
            int status,
            String message,
            String path,
            String correlationId,
            Map<String, String> validationErrors) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                "VALIDATION_FAILED",
                message,
                path,
                correlationId,
                validationErrors != null ? Collections.unmodifiableMap(validationErrors) : Collections.emptyMap()
        );
    }
}
