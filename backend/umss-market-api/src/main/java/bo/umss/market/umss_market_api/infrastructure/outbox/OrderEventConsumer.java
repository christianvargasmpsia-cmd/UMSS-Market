package bo.umss.market.umss_market_api.infrastructure.outbox;

import bo.umss.market.umss_market_api.infrastructure.persistence.entities.OutboxEventEntity;

/** Punto de extension para efectos internos o integracion con terceros. */
public interface OrderEventConsumer {
    void accept(OutboxEventEntity event);
}
