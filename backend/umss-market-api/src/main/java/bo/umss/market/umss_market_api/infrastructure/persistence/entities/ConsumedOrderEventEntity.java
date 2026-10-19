package bo.umss.market.umss_market_api.infrastructure.persistence.entities;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;

/** Inbox local idempotente: una sola fila por evento consumido. */
@Entity
@Table(name = "consumed_order_events")
public class ConsumedOrderEventEntity {
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "consumed_at", nullable = false)
    private LocalDateTime consumedAt;

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
}
