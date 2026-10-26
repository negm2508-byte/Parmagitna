package com.papererp.core.service;

import com.papererp.core.domain.OutboxEventEntity;
import com.papererp.core.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;

    public OutboxService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public OutboxEventEntity recordEvent(UUID tenantId, String aggregateType, UUID aggregateId, String eventType, String payload) {
        OutboxEventEntity event = new OutboxEventEntity(tenantId, aggregateType, aggregateId, eventType, payload);
        return outboxEventRepository.save(event);
    }

    @Transactional
    public List<OutboxEventEntity> claimBatch(int batchSize) {
        Instant staleThreshold = Instant.now().minus(5, java.time.temporal.ChronoUnit.MINUTES);
        List<OutboxEventEntity> claimed;
        try {
            claimed = outboxEventRepository.claimPendingEventsWithLock(staleThreshold, batchSize);
        } catch (Exception ex) {
            log.debug("claimPendingEventsWithLock fallback: {}", ex.getMessage());
            claimed = outboxEventRepository.findPendingEvents(PageRequest.of(0, batchSize));
        }

        if (claimed.isEmpty()) {
            return List.of();
        }

        for (OutboxEventEntity event : claimed) {
            event.setStatus("PROCESSING");
            event.setLockedAt(Instant.now());
            outboxEventRepository.save(event);
        }
        return claimed;
    }

    @Scheduled(fixedDelayString = "${papererp.outbox.poll-interval-ms:10000}")
    public void processPendingEvents() {
        List<OutboxEventEntity> batch = claimBatch(50);
        if (batch.isEmpty()) {
            return;
        }

        for (OutboxEventEntity event : batch) {
            publishEvent(event.getId());
        }
    }

    @Transactional
    public void publishEvent(UUID eventId) {
        var opt = outboxEventRepository.findById(eventId);
        if (opt.isEmpty()) {
            return;
        }

        OutboxEventEntity event = opt.get();
        try {
            log.info("Processing outbox event: id={}, type={}, aggregate={}/{}",
                    event.getId(), event.getEventType(), event.getAggregateType(), event.getAggregateId());
            event.setStatus("PUBLISHED");
            event.setPublishedAt(Instant.now());
            event.setErrorMessage(null);
        } catch (Exception ex) {
            log.error("Failed to publish outbox event id={}: {}", event.getId(), ex.getMessage());
            int newRetry = event.getRetryCount() + 1;
            event.setRetryCount(newRetry);
            event.setErrorMessage(ex.getMessage());
            if (newRetry >= 5) {
                event.setStatus("FAILED");
            } else {
                event.setStatus("PENDING");
            }
        }
        outboxEventRepository.save(event);
    }
}
