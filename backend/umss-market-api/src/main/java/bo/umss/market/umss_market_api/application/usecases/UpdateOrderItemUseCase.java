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
public class UpdateOrderItemUseCase {
    private final OrderItemRepositoryPort items;
    private final OrderRepositoryPort orders;
    private final PublicationRepositoryPort publications;
    private final InventoryReservationService reservations;

    @Transactional
    public OrderItem execute(UUID orderId, UUID itemId, CreateOrderItemRequest request) {
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
        if (request == null || request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Cantidad positiva obligatoria");
        }
        UUID nextId = request.getPublicationId() == null ? item.getPublicationId() : request.getPublicationId();
        Publication next = publications.findById(nextId)
                .orElseThrow(() -> new IllegalArgumentException("Publicacion no encontrada"));
        BigDecimal oldSubtotal = item.getSubtotal();
        boolean oldReserved = Boolean.TRUE.equals(item.getStockReserved());
        boolean newReserved;
        if (nextId.equals(item.getPublicationId())) {
            int delta = request.getQuantity() - item.getQuantity();
            if (oldReserved) {
                if (delta > 0) reservations.reserve(next, delta);
                if (delta < 0) reservations.release(item.getPublicationId(), -delta, true);
                newReserved = true;
            } else {
                // Item anterior sin descuento: al modificarlo pasa a reserva real.
                newReserved = reservations.reserve(next, request.getQuantity());
            }
        } else {
            // En fallo de la nueva reserva, se revierte tambien la devolucion anterior.
            reservations.release(item.getPublicationId(), item.getQuantity(), oldReserved);
            newReserved = reservations.reserve(next, request.getQuantity());
        }
        BigDecimal subtotal = next.getPrecio().multiply(BigDecimal.valueOf(request.getQuantity()));
        item.setPublicationId(next.getId());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(next.getPrecio());
        item.setSubtotal(subtotal);
        item.setStockReserved(newReserved);
        OrderItem saved = items.save(item);
        order.setTotal(order.getTotal().subtract(oldSubtotal).add(subtotal));
        order.setUpdatedAt(LocalDateTime.now());
        orders.save(order);
        return saved;
    }
}
