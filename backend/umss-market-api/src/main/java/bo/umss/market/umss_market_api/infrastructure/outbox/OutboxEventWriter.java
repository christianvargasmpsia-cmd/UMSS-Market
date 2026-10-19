package bo.umss.market.umss_market_api.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.enums.OrderEventType;
import bo.umss.market.umss_market_api.domain.ports.OrderOutboxPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.OutboxEventEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaOutboxEventRepository;

@Service
public class OutboxEventWriter implements OrderOutboxPort {
    private final JpaOutboxEventRepository repository;

    public OutboxEventWriter(JpaOutboxEventRepository repository) {
        this.repository = repository;
    }

    /** MANDATORY impide escribir eventos fuera de la transaccion de pedidos. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID record(UUID orderId, OrderEventType eventType, OrderStatus status) {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(status, "status");
        LocalDateTime now = LocalDateTime.now();
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(UUID.randomUUID());
        event.setOrderId(orderId);
        event.setEventType(eventType);
        // Valores UUID/enum provienen del servidor: JSON construido sin campos del cliente.
        event.setPayload("{\"schemaVersion\":1,\"orderId\":\"" + orderId
                + "\",\"status\":\"" + status.name() + "\"}");
        event.setStatus(OutboxStatus.PENDING);
        event.setAvailableAt(now);
        event.setCreatedAt(now);
        event.setAttemptCount(0);
        repository.save(event);
        return event.getId();
    }
}
