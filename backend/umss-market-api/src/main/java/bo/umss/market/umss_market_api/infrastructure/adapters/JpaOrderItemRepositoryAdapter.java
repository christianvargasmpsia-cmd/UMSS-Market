package bo.umss.market.umss_market_api.infrastructure.adapters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.mappers.OrderItemMapper;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaOrderItemRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaOrderItemRepositoryAdapter
        implements OrderItemRepositoryPort {

    private final JpaOrderItemRepository repository;

    @Override
    public OrderItem save(OrderItem orderItem) {

        return OrderItemMapper.toDomain(
                repository.save(
                        OrderItemMapper.toEntity(orderItem)));
    }

    @Override
    public List<OrderItem> findAll() {

        return repository.findAll()
                .stream()
                .map(OrderItemMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<OrderItem> findById(UUID id) {

        return repository.findById(id)
                .map(OrderItemMapper::toDomain);
    }

    @Override
    public List<OrderItem> findByOrderId(UUID orderId) {

        return repository.findByOrderId(orderId)
                .stream()
                .map(OrderItemMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteByOrderId(UUID orderId) {
        repository.deleteByOrderId(orderId);
    }

    @Override
    public long countByOrderId(UUID orderId) {
        return repository.countByOrderId(orderId);
    }
}