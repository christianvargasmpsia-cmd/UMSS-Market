package bo.umss.market.umss_market_api.application.usecases;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import bo.umss.market.umss_market_api.application.dto.OrderItemResponse;
import bo.umss.market.umss_market_api.application.dto.OrderResponse;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetOrdersUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OrderItemRepositoryPort orderItemRepository;
    private final PublicationRepositoryPort publicationRepository;

    public List<OrderResponse> execute(UUID userId) {

        List<Order> orders = orderRepository.findByUserId(userId);

        return orders.stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderResponse toResponse(Order order) {

        List<OrderItem> items =
                orderItemRepository.findByOrderId(order.getId());

        List<OrderItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .total(order.getTotal())
                .estado(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {

        String producto = publicationRepository
                .findById(item.getPublicationId())
                .map(Publication::getNombre)
                .orElse("Producto no disponible");

        return OrderItemResponse.builder()
                .id(item.getId())
                .orderId(item.getOrderId())
                .publicationId(item.getPublicationId())
                .producto(producto)
                .cantidad(item.getQuantity())
                .precioUnitario(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }
}