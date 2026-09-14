package bo.umss.market.umss_market_api.application.usecases;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.application.dto.CreateOrderItemRequest;
import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateOrderItemUseCase {

    private final OrderItemRepositoryPort orderItemRepository;
    private final OrderRepositoryPort orderRepository;
    private final PublicationRepositoryPort publicationRepository;

    @Transactional
    public OrderItem execute(
            UUID orderId,
            CreateOrderItemRequest request) {

        orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Pedido no encontrado: " + orderId));

        if (request.getPublicationId() == null) {
            throw new RuntimeException(
                    "La publicación es obligatoria");
        }

        if (request.getQuantity() == null ||
                request.getQuantity() < 1) {

            throw new RuntimeException(
                    "La cantidad debe ser mayor a cero");
        }

        Publication publication =
                publicationRepository
                        .findById(request.getPublicationId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Publicación no encontrada: "
                                                + request.getPublicationId()));

        if (Boolean.FALSE.equals(publication.getActiva())) {
            throw new RuntimeException(
                    "La publicación no está activa: "
                            + publication.getNombre());
        }

        if (publication.getStock() != null &&
                publication.getStock() < request.getQuantity()) {

            throw new RuntimeException(
                    "Stock insuficiente para: "
                            + publication.getNombre());
        }

        BigDecimal unitPrice = publication.getPrecio();

        BigDecimal subtotal = unitPrice.multiply(
                BigDecimal.valueOf(request.getQuantity()));

        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .publicationId(publication.getId())
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .build();

        return orderItemRepository.save(item);
    }
}