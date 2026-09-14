package bo.umss.market.umss_market_api.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteOrderUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OrderItemRepositoryPort orderItemRepository;

    @Transactional
    public void execute(UUID orderId) {

        orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Pedido no encontrado: " + orderId));

        orderItemRepository.deleteByOrderId(orderId);
        orderRepository.deleteById(orderId);
    }
}