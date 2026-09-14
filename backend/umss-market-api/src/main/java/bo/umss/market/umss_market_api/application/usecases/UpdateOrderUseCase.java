package bo.umss.market.umss_market_api.application.usecases;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import bo.umss.market.umss_market_api.application.dto.UpdateOrderRequest;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateOrderUseCase {

    private final OrderRepositoryPort orderRepository;

    public Order execute(
            UUID orderId,
            UpdateOrderRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Pedido no encontrado: " + orderId));

        if (request.getTotal() != null) {
            order.setTotal(request.getTotal());
        }

        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }
}