package bo.umss.market.umss_market_api.infrastructure.outbox;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.*;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaConsumedOrderEventRepository;

/** Efecto demostrable/idempotente: registrar una recepcion durable por eventId. */
@Component
public class InboxOrderEventConsumer implements OrderEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(InboxOrderEventConsumer.class);
    private final JpaConsumedOrderEventRepository receipts;

    public InboxOrderEventConsumer(JpaConsumedOrderEventRepository receipts) {
        this.receipts = receipts;
    }

    @Override
    public void accept(OutboxEventEntity event) {
        if (receipts.existsById(event.getId())) {
            log.info("OUTBOX DUPLICATE eventId={} orderId={}", event.getId(), event.getOrderId());
            return;
        }
        ConsumedOrderEventEntity receipt = new ConsumedOrderEventEntity();
        receipt.setEventId(event.getId());
        receipt.setOrderId(event.getOrderId());
        receipt.setEventType(event.getEventType().name());
        receipt.setConsumedAt(LocalDateTime.now());
        receipts.saveAndFlush(receipt);
        log.info("OUTBOX CONSUMED eventId={} orderId={} type={}", event.getId(),
                event.getOrderId(), event.getEventType());
    }
}
