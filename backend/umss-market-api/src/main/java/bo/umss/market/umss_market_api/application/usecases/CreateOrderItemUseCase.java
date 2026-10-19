package bo.umss.market.umss_market_api.application.usecases;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.application.dto.CreateOrderItemRequest;
import bo.umss.market.umss_market_api.application.services.InventoryReservationService;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.exceptions.InvalidOrderStateException;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateOrderItemUseCase {
    private final OrderItemRepositoryPort items;
    private final OrderRepositoryPort orders;
    private final PublicationRepositoryPort publications;
    private final InventoryReservationService reservations;

    @Transactional
    public OrderItem execute(UUID orderId, CreateOrderItemRequest request) {
        Order order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new InvalidOrderStateException("Solo se pueden editar pedidos PENDIENTES");
        }
        if (request == null || request.getPublicationId() == null ||
                request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Publicacion y cantidad positiva obligatorias");
        }
        Publication publication = publications.findById(request.getPublicationId())
                .orElseThrow(() -> new IllegalArgumentException("Publicacion no encontrada"));
        boolean stockReserved = reservations.reserve(publication, request.getQuantity());
        BigDecimal subtotal = publication.getPrecio().multiply(BigDecimal.valueOf(request.getQuantity()));
        OrderItem item = OrderItem.builder().id(UUID.randomUUID()).orderId(orderId)
                .publicationId(publication.getId()).quantity(request.getQuantity())
                .unitPrice(publication.getPrecio()).subtotal(subtotal)
                .stockReserved(stockReserved).build();
        OrderItem saved = items.save(item);
        order.setTotal(order.getTotal().add(subtotal));
        order.setUpdatedAt(LocalDateTime.now());
        orders.save(order);
        return saved;
    }
}
