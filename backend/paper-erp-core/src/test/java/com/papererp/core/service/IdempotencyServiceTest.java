package com.papererp.core.service;

import com.papererp.common.exception.ConcurrencyConflictException;
import com.papererp.common.exception.DomainException;
import com.papererp.core.domain.IdempotencyRecordEntity;
import com.papererp.core.repository.IdempotencyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Idempotency Engine & Request Hashing Unit Tests")
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final String idempotencyKey = "order-tx-12345";
    private final String payloadJson = "{\"parentJumboId\":\"" + UUID.randomUUID() + "\",\"cuts\":[{\"width\":500}]}";

    @Test
    @DisplayName("1. First Execution: Saves new PENDING record and returns empty optional")
    void firstExecution_savesPendingRecord() {
        when(idempotencyRepository.findByTenantIdAndIdempotencyKey(tenantA, idempotencyKey))
                .thenReturn(Optional.empty());

        var result = idempotencyService.checkOrStart(tenantA, idempotencyKey, payloadJson);

        assertTrue(result.isEmpty());
        verify(idempotencyRepository, times(1)).saveAndFlush(any(IdempotencyRecordEntity.class));
    }

    @Test
    @DisplayName("2. Repeated Request: Same key and identical payload returns completed cached record")
    void repeatedRequest_samePayload_returnsCompletedRecord() {
        // Calculate expected hash
        IdempotencyRecordEntity completedRecord = new IdempotencyRecordEntity(
                tenantA, idempotencyKey, "dummyHash", Instant.now().plus(24, ChronoUnit.HOURS)
        );
        completedRecord.setStatus("COMPLETED");
        completedRecord.setResponseCode(200);
        completedRecord.setResponseBody("{\"status\":\"SUCCESS\"}");

        // Using service's hash computation by checking against the same payload
        when(idempotencyRepository.findByTenantIdAndIdempotencyKey(tenantA, idempotencyKey))
                .thenAnswer(inv -> {
                    // Inject real hash so payload check passes
                    completedRecord.setRequestHash(hashOf(payloadJson));
                    return Optional.of(completedRecord);
                });

        var result = idempotencyService.checkOrStart(tenantA, idempotencyKey, payloadJson);

        assertTrue(result.isPresent());
        assertEquals("COMPLETED", result.get().getStatus());
        assertEquals(200, result.get().getResponseCode());
        assertEquals("{\"status\":\"SUCCESS\"}", result.get().getResponseBody());
        verify(idempotencyRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("3. Payload Mismatch: Same key with different payload throws 409 Conflict")
    void sameKey_differentPayload_throwsConflict() {
        IdempotencyRecordEntity existing = new IdempotencyRecordEntity(
                tenantA, idempotencyKey, hashOf(payloadJson), Instant.now().plus(24, ChronoUnit.HOURS)
        );
        existing.setStatus("COMPLETED");

        when(idempotencyRepository.findByTenantIdAndIdempotencyKey(tenantA, idempotencyKey))
                .thenReturn(Optional.of(existing));

        String alteredPayload = "{\"parentJumboId\":\"" + UUID.randomUUID() + "\",\"cuts\":[{\"width\":999}]}";

        DomainException ex = assertThrows(DomainException.class, () ->
                idempotencyService.checkOrStart(tenantA, idempotencyKey, alteredPayload)
        );

        assertEquals(409, ex.getHttpStatus());
        assertEquals("IDEMPOTENCY_PAYLOAD_MISMATCH", ex.getErrorCode());
    }

    @Test
    @DisplayName("4. In-Progress Request: Throws ConcurrencyConflictException (409)")
    void inProgressRequest_throwsConflict() {
        IdempotencyRecordEntity pending = new IdempotencyRecordEntity(
                tenantA, idempotencyKey, hashOf(payloadJson), Instant.now().plus(24, ChronoUnit.HOURS)
        );
        pending.setStatus("PENDING");

        when(idempotencyRepository.findByTenantIdAndIdempotencyKey(tenantA, idempotencyKey))
                .thenReturn(Optional.of(pending));

        assertThrows(ConcurrencyConflictException.class, () ->
                idempotencyService.checkOrStart(tenantA, idempotencyKey, payloadJson)
        );
    }

    @Test
    @DisplayName("5. Concurrent Insert Race Condition: Handled via DataIntegrityViolationException")
    void concurrentInsert_raceCondition_throwsConcurrencyConflict() {
        when(idempotencyRepository.findByTenantIdAndIdempotencyKey(tenantA, idempotencyKey))
                .thenReturn(Optional.empty());
        doThrow(new DataIntegrityViolationException("uk_idempotency_tenant_key violation"))
                .when(idempotencyRepository).saveAndFlush(any(IdempotencyRecordEntity.class));

        assertThrows(ConcurrencyConflictException.class, () ->
                idempotencyService.checkOrStart(tenantA, idempotencyKey, payloadJson)
        );
    }

    @Test
    @DisplayName("6. Failed Request Retry: Status reset to PENDING allowing retry")
    void failedRequest_retry_resetsToPending() {
        IdempotencyRecordEntity failed = new IdempotencyRecordEntity(
                tenantA, idempotencyKey, hashOf(payloadJson), Instant.now().plus(24, ChronoUnit.HOURS)
        );
        failed.setStatus("FAILED");
        failed.setResponseCode(500);
        failed.setResponseBody("Transient network timeout");

        when(idempotencyRepository.findByTenantIdAndIdempotencyKey(tenantA, idempotencyKey))
                .thenReturn(Optional.of(failed));

        var result = idempotencyService.checkOrStart(tenantA, idempotencyKey, payloadJson);

        assertTrue(result.isEmpty());
        assertEquals("PENDING", failed.getStatus());
        assertNull(failed.getResponseCode());
        assertNull(failed.getResponseBody());
        verify(idempotencyRepository, times(1)).save(failed);
    }

    @Test
    @DisplayName("7. Multi-Tenant Key Isolation: Separate tenants can reuse the same key independently")
    void separateTenants_canUseSameKey() {
        when(idempotencyRepository.findByTenantIdAndIdempotencyKey(tenantB, idempotencyKey))
                .thenReturn(Optional.empty());

        var result = idempotencyService.checkOrStart(tenantB, idempotencyKey, payloadJson);

        assertTrue(result.isEmpty());
        verify(idempotencyRepository, times(1)).saveAndFlush(argThat(entity ->
                entity.getTenantId().equals(tenantB) && entity.getIdempotencyKey().equals(idempotencyKey)
        ));
    }

    private String hashOf(String payload) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
