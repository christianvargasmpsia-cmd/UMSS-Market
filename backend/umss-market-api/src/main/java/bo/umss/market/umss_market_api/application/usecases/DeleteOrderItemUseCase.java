package bo.umss.market.umss_market_api.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteOrderItemUseCase {

    private final OrderItemRepositoryPort orderItemRepository;

    @Transactional
    public void execute(UUID orderId, UUID itemId) {

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Item del pedido no encontrado: " + itemId));

        if (!orderId.equals(item.getOrderId())) {
            throw new RuntimeException(
                    "El item no pertenece al pedido indicado");
        }

        orderItemRepository.deleteById(itemId);
    }
}