package bo.umss.market.umss_market_api.application.usecases;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.application.dto.UpdateOrderStatusRequest;
import bo.umss.market.umss_market_api.application.services.InventoryReservationService;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.exceptions.InvalidOrderStateException;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {
    private final OrderRepositoryPort orders;
    private final OrderItemRepositoryPort items;
    private final InventoryReservationService reservations;

    @Transactional
    public Order execute(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("Estado obligatorio");
        }
        OrderStatus target = request.getStatus();
        OrderStatus previous = order.getStatus();
        if (previous == target) return order; // no liberar dos veces
        if (previous == OrderStatus.CANCELADO ||
                (previous == OrderStatus.CONFIRMADO && target == OrderStatus.PENDIENTE)) {
            throw new InvalidOrderStateException("Transicion de estado no permitida");
        }
        if (target == OrderStatus.CANCELADO) {
            for (OrderItem item : items.findByOrderId(orderId)) {
                if (Boolean.TRUE.equals(item.getStockReserved())) {
                    reservations.release(item.getPublicationId(), item.getQuantity(), true);
                    item.setStockReserved(false);
                    items.save(item);
                }
            }
        }
        order.setStatus(target);
        order.setUpdatedAt(LocalDateTime.now());
        return orders.save(order);
    }
}
