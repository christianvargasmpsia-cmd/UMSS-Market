package bo.umss.market.umss_market_api.application.usecases;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import bo.umss.market.umss_market_api.application.dto.UpdateOrderStatusRequest;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {

    private final OrderRepositoryPort orderRepository;

    public Order execute(
            UUID orderId,
            UpdateOrderStatusRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Pedido no encontrado: " + orderId));

        if (request.getStatus() == null) {
            throw new RuntimeException(
                    "El estado del pedido es obligatorio");
        }

        order.setStatus(request.getStatus());
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }
}