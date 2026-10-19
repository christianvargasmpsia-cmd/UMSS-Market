package bo.umss.market.umss_market_api.domain.ports;

import java.util.UUID;
import bo.umss.market.umss_market_api.domain.enums.OrderEventType;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;

/** Contrato de eventos de negocio: el adaptador guarda en la TX del pedido. */
public interface OrderOutboxPort {
    UUID record(UUID orderId, OrderEventType type, OrderStatus status);
}
