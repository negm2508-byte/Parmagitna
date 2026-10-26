package com.papererp.api.exception;

import com.papererp.api.filter.CorrelationIdFilter;
import com.papererp.common.dto.ApiErrorResponse;
import com.papererp.common.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(DomainException ex, HttpServletRequest request) {
        log.warn("Domain exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ApiErrorResponse err = ApiErrorResponse.of(
                ex.getHttpStatus(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                getCorrelationId(request)
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(err);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.info("Entity not found: {}", ex.getMessage());
        ApiErrorResponse err = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                getCorrelationId(request)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleTenantAccessDenied(TenantAccessDeniedException ex, HttpServletRequest request) {
        log.warn("Tenant access denied: {}", ex.getMessage());
        ApiErrorResponse err = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                ex.getErrorCode(),
                "Access to the requested tenant or plant resource is denied",
                request.getRequestURI(),
                getCorrelationId(request)
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.info("Request validation failed on {}: {} errors", request.getRequestURI(), fieldErrors.size());
        ApiErrorResponse err = ApiErrorResponse.ofValidation(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Validation failed for one or more fields",
                request.getRequestURI(),
                getCorrelationId(request),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(err);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse err = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "You do not possess the required role or authority to perform this operation",
                request.getRequestURI(),
                getCorrelationId(request)
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
    }

    @ExceptionHandler({
            org.springframework.dao.OptimisticLockingFailureException.class,
            org.springframework.dao.PessimisticLockingFailureException.class,
            org.springframework.dao.CannotAcquireLockException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConcurrencyLockingException(Exception ex, HttpServletRequest request) {
        log.warn("Database concurrency conflict on {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse err = ApiErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "CONCURRENCY_CONFLICT",
                "The requested resource was modified or locked by another concurrent transaction. Please retry your request.",
                request.getRequestURI(),
                getCorrelationId(request)
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        // Redact internal stack traces, topology, and database specifics
        log.error("Unhandled internal exception on {} [Correlation: {}]",
                request.getRequestURI(), getCorrelationId(request), ex);

        ApiErrorResponse err = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred. Please contact system administration with the correlation ID.",
                request.getRequestURI(),
                getCorrelationId(request)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }

    private String getCorrelationId(HttpServletRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        }
        return correlationId != null ? correlationId : "N/A";
    }
}
