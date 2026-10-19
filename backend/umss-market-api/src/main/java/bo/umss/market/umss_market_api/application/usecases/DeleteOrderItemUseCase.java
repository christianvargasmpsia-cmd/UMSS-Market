package bo.umss.market.umss_market_api.application.usecases;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class DeleteOrderItemUseCase {
    private final OrderItemRepositoryPort items;
    private final OrderRepositoryPort orders;
    private final InventoryReservationService reservations;

    @Transactional
    public void execute(UUID orderId, UUID itemId) {
        Order order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new InvalidOrderStateException("Solo se pueden editar pedidos PENDIENTES");
        }
        OrderItem item = items.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item no encontrado"));
        if (!orderId.equals(item.getOrderId())) {
            throw new IllegalArgumentException("El item no pertenece al pedido");
        }
        reservations.release(item.getPublicationId(), item.getQuantity(),
                Boolean.TRUE.equals(item.getStockReserved()));
        items.deleteById(itemId);
        order.setTotal(order.getTotal().subtract(item.getSubtotal()));
        order.setUpdatedAt(LocalDateTime.now());
        orders.save(order);
    }
}
