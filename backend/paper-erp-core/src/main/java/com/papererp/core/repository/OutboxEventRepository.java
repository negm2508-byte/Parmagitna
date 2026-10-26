package com.papererp.core.repository;

import com.papererp.core.domain.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    
    @Query("SELECT o FROM OutboxEventEntity o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<OutboxEventEntity> findPendingEvents(Pageable pageable);

    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING'
               OR (status = 'PROCESSING' AND (locked_at IS NULL OR locked_at < :staleThreshold))
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventEntity> claimPendingEventsWithLock(
            @Param("staleThreshold") Instant staleThreshold,
            @Param("limit") int limit
    );

    List<OutboxEventEntity> findByTenantIdAndAggregateId(UUID tenantId, UUID aggregateId);
}
