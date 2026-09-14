package bo.umss.market.umss_market_api.infrastructure.adapters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.mappers.OrderMapper;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaOrderRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaOrderRepositoryAdapter implements OrderRepositoryPort {

    private final JpaOrderRepository repository;

    @Override
    public Order save(Order order) {

        return OrderMapper.toDomain(
                repository.save(
                        OrderMapper.toEntity(order)));
    }

    @Override
    public List<Order> findAll() {

        return repository.findAll()
                .stream()
                .map(OrderMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Order> findById(UUID id) {

        return repository.findById(id)
                .map(OrderMapper::toDomain);
    }

    @Override
    public List<Order> findByUserId(UUID userId) {

        return repository.findByUserId(userId)
                .stream()
                .map(OrderMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {

        return repository.findByStatus(status)
                .stream()
                .map(OrderMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByUserIdAndStatus(
            UUID userId,
            OrderStatus status) {

        return repository.findByUserIdAndStatus(userId, status)
                .stream()
                .map(OrderMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}