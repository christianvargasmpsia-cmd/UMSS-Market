package bo.umss.market.umss_market_api.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.model.Order;

public interface OrderRepositoryPort {

    Order save(Order order);

    List<Order> findAll();

    Optional<Order> findById(UUID id);

    List<Order> findByUserId(UUID userId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);

    void deleteById(UUID id);
}