package bo.umss.market.umss_market_api.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaOutboxEventRepository;

/** Polling separado de transacciones de pedidos; procesa cada id en su propia TX. */
@Component
@ConditionalOnProperty(name = "outbox.processor.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private final JpaOutboxEventRepository events;
    private final OutboxEventProcessor processor;

    public OutboxDispatcher(JpaOutboxEventRepository events, OutboxEventProcessor processor) {
        this.events = events;
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-delay-ms:5000}")
    public void dispatch() {
        List<UUID> due = events.findDueIds(List.of(OutboxStatus.PENDING, OutboxStatus.RETRY),
                LocalDateTime.now(), PageRequest.of(0, 20));
        for (UUID id : due) {
            try {
                processor.process(id);  // proxy @Transactional distinto del scheduler
            } catch (RuntimeException ex) {
                log.error("OUTBOX WORKER ERROR eventId={}", id, ex);
            }
        }
    }
}
