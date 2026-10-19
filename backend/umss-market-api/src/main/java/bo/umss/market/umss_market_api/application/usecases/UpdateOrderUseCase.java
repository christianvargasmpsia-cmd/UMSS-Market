package bo.umss.market.umss_market_api.application.usecases;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.application.dto.UpdateOrderRequest;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateOrderUseCase {

    private final OrderRepositoryPort orderRepository;

    @Transactional
    public Order execute(
            UUID orderId,
            UpdateOrderRequest request) {

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Pedido no encontrado: " + orderId));

        // El monto debe resultar de los items, nunca del body del cliente.
        if (request != null && request.getTotal() != null &&
                order.getTotal().compareTo(request.getTotal()) != 0) {
            throw new IllegalArgumentException("El total se calcula a partir de los items");
        }

        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }
}