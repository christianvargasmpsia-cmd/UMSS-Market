package bo.umss.market.umss_market_api.application.usecases;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.application.dto.CreateOrderItemRequest;
import bo.umss.market.umss_market_api.application.dto.CreateOrderRequest;
import bo.umss.market.umss_market_api.application.dto.CreateOrderResponse;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OrderItemRepositoryPort orderItemRepository;
    private final PublicationRepositoryPort publicationRepository;

    @Transactional
    public CreateOrderResponse execute(CreateOrderRequest request) {

        if (request.getUserId() == null) {
            throw new RuntimeException(
                    "El usuario es obligatorio");
        }

        if (request.getItems() == null ||
                request.getItems().isEmpty()) {

            throw new RuntimeException(
                    "El pedido debe contener al menos un producto");
        }

        UUID orderId = UUID.randomUUID();

        BigDecimal total = BigDecimal.ZERO;

        List<OrderItem> items = new ArrayList<>();

        for (CreateOrderItemRequest itemRequest : request.getItems()) {

            if (itemRequest.getPublicationId() == null) {
                throw new RuntimeException(
                        "La publicación es obligatoria");
            }

            if (itemRequest.getQuantity() == null ||
                    itemRequest.getQuantity() < 1) {

                throw new RuntimeException(
                        "La cantidad debe ser mayor a cero");
            }

            Publication publication =
                    publicationRepository
                            .findById(itemRequest.getPublicationId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Publicación no encontrada: "
                                                    + itemRequest.getPublicationId()));

            if (Boolean.FALSE.equals(publication.getActiva())) {
                throw new RuntimeException(
                        "La publicación no está activa: "
                                + publication.getNombre());
            }

            if (publication.getStock() != null &&
                    publication.getStock() < itemRequest.getQuantity()) {

                throw new RuntimeException(
                        "Stock insuficiente para: "
                                + publication.getNombre());
            }

            BigDecimal unitPrice = publication.getPrecio();

            BigDecimal subtotal = unitPrice.multiply(
                    BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .publicationId(publication.getId())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            items.add(orderItem);

            total = total.add(subtotal);
        }

        LocalDateTime now = LocalDateTime.now();

        Order order = Order.builder()
                .id(orderId)
                .userId(request.getUserId())
                .total(total)
                .status(OrderStatus.PENDIENTE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        orderRepository.save(order);

        for (OrderItem item : items) {
            orderItemRepository.save(item);
        }

        return CreateOrderResponse.builder()
                .success(true)
                .message("Pedido creado correctamente")
                .orderId(orderId)
                .build();
    }
}