package com.papererp.core.service;

import com.papererp.common.exception.ConcurrencyConflictException;
import com.papererp.common.exception.DomainException;
import com.papererp.core.domain.IdempotencyRecordEntity;
import com.papererp.core.repository.IdempotencyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    public IdempotencyService(IdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    @Transactional
    public Optional<IdempotencyRecordEntity> checkOrStart(UUID tenantId, String idempotencyKey, String payload) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        String requestHash = hashPayload(payload);
        var existing = idempotencyRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecordEntity record = existing.get();

            // 1. Payload Mismatch Check: Enforce that reusing a key with different payload fails with 409 Conflict
            if (!record.getRequestHash().equals(requestHash)) {
                throw new DomainException(
                        "IDEMPOTENCY_PAYLOAD_MISMATCH",
                        "Idempotency key '" + idempotencyKey + "' was already used with a different request payload.",
                        409
                );
            }

            // 2. Completed: Return replayed response
            if ("COMPLETED".equals(record.getStatus())) {
                return Optional.of(record);
            }

            // 3. In-Progress: Reject concurrent duplicate execution
            if ("PENDING".equals(record.getStatus())) {
                throw new ConcurrencyConflictException(
                        "An operation with idempotency key '" + idempotencyKey + "' is already in progress. Please retry shortly."
                );
            }

            // 4. Failed: Allow retry by resetting status to PENDING
            if ("FAILED".equals(record.getStatus())) {
                record.setStatus("PENDING");
                record.setResponseCode(null);
                record.setResponseBody(null);
                record.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
                idempotencyRepository.save(record);
                return Optional.empty();
            }
        }

        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        IdempotencyRecordEntity newRecord = new IdempotencyRecordEntity(tenantId, idempotencyKey, requestHash, expiresAt);
        try {
            idempotencyRepository.saveAndFlush(newRecord);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent race condition: Another instance just inserted the same key
            throw new ConcurrencyConflictException(
                    "An operation with idempotency key '" + idempotencyKey + "' is already in progress. Please retry shortly."
            );
        }
        return Optional.empty();
    }

    @Transactional
    public void complete(UUID tenantId, String idempotencyKey, int statusCode, String responseBody) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        idempotencyRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).ifPresent(record -> {
            record.setStatus("COMPLETED");
            record.setResponseCode(statusCode);
            record.setResponseBody(responseBody);
            idempotencyRepository.save(record);
        });
    }

    @Transactional
    public void fail(UUID tenantId, String idempotencyKey, int statusCode, String errorMessage) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        idempotencyRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).ifPresent(record -> {
            record.setStatus("FAILED");
            record.setResponseCode(statusCode);
            record.setResponseBody(errorMessage);
            idempotencyRepository.save(record);
        });
    }

    private String hashPayload(String payload) {
        if (payload == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(payload.hashCode());
        }
    }
}
