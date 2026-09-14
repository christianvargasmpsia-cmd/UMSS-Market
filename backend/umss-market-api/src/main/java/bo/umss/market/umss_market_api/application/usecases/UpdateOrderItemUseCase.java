package bo.umss.market.umss_market_api.application.usecases;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.application.dto.CreateOrderItemRequest;
import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateOrderItemUseCase {

    private final OrderItemRepositoryPort orderItemRepository;
    private final PublicationRepositoryPort publicationRepository;

    @Transactional
    public OrderItem execute(
            UUID orderId,
            UUID itemId,
            CreateOrderItemRequest request) {

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Item del pedido no encontrado: " + itemId));

        if (!orderId.equals(item.getOrderId())) {
            throw new RuntimeException(
                    "El item no pertenece al pedido indicado");
        }

        if (request.getQuantity() == null ||
                request.getQuantity() < 1) {

            throw new RuntimeException(
                    "La cantidad debe ser mayor a cero");
        }

        Publication publication =
                publicationRepository
                        .findById(item.getPublicationId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Publicación no encontrada: "
                                                + item.getPublicationId()));

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

        item.setQuantity(request.getQuantity());
        item.setUnitPrice(unitPrice);
        item.setSubtotal(subtotal);

        return orderItemRepository.save(item);
    }
}