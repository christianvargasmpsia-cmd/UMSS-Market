package bo.umss.market.umss_market_api.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.OutboxEventEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaOutboxEventRepository;

/** Un evento por transaccion; lock de fila evita despachos simultaneos. */
@Service
public class OutboxEventProcessor {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);
    private final JpaOutboxEventRepository events;
    private final OrderEventConsumer consumer;
    private final int maxAttempts;

    public OutboxEventProcessor(JpaOutboxEventRepository events, OrderEventConsumer consumer,
                                @Value("${outbox.max-attempts:5}") int maxAttempts) {
        this.events = events;
        this.consumer = consumer;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Transactional
    public boolean process(UUID eventId) {
        OutboxEventEntity event = events.findByIdForUpdate(eventId).orElse(null);
        if (event == null) return false;
        LocalDateTime now = LocalDateTime.now();
        if ((event.getStatus() != OutboxStatus.PENDING && event.getStatus() != OutboxStatus.RETRY)
                || event.getAvailableAt().isAfter(now)) return false;
        event.setStatus(OutboxStatus.PROCESSING);
        events.saveAndFlush(event);
        try {
            consumer.accept(event);
            event.setStatus(OutboxStatus.PROCESSED);
            event.setProcessedAt(LocalDateTime.now());
            event.setLastError(null);
            events.save(event);
            log.info("OUTBOX PROCESSED eventId={} orderId={} attempts={}", eventId,
                    event.getOrderId(), event.getAttemptCount());
            return true;
        } catch (RuntimeException ex) {
            int attempts = event.getAttemptCount() + 1;
            event.setAttemptCount(attempts);
            event.setLastError(ex.getClass().getSimpleName() + ": " +
                    String.valueOf(ex.getMessage()).substring(0,
                        Math.min(430, String.valueOf(ex.getMessage()).length())));
            if (attempts >= maxAttempts) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("OUTBOX FAILED eventId={} attempts={}", eventId, attempts, ex);
            } else {
                event.setStatus(OutboxStatus.RETRY);
                event.setAvailableAt(LocalDateTime.now().plusSeconds(Math.min(300L, 1L << Math.min(attempts, 8))));
                log.warn("OUTBOX RETRY eventId={} attempt={} nextAt={}", eventId,
                        attempts, event.getAvailableAt(), ex);
            }
            events.save(event);
            return false;
        }
    }
}
