package bo.umss.market.umss_market_api.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import bo.umss.market.umss_market_api.domain.model.OrderItem;

public interface OrderItemRepositoryPort {

    OrderItem save(OrderItem orderItem);

    List<OrderItem> findAll();

    Optional<OrderItem> findById(UUID id);

    List<OrderItem> findByOrderId(UUID orderId);

    void deleteById(UUID id);

    void deleteByOrderId(UUID orderId);

    long countByOrderId(UUID orderId);
}