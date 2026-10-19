package bo.umss.market.umss_market_api.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.application.services.InventoryReservationService;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteOrderUseCase {
    private final OrderRepositoryPort orders;
    private final OrderItemRepositoryPort items;
    private final InventoryReservationService reservations;

    @Transactional
    public void execute(UUID orderId) {
        Order order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        for (OrderItem item : items.findByOrderId(order.getId())) {
            reservations.release(item.getPublicationId(), item.getQuantity(),
                    Boolean.TRUE.equals(item.getStockReserved()));
        }
        items.deleteByOrderId(orderId);
        orders.deleteById(orderId);
    }
}
