package bo.umss.market.umss_market_api.application.usecases;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetOrderItemsUseCase {

    private final OrderItemRepositoryPort orderItemRepository;

    public List<OrderItem> execute(UUID orderId) {

        return orderItemRepository.findByOrderId(orderId);
    }
}