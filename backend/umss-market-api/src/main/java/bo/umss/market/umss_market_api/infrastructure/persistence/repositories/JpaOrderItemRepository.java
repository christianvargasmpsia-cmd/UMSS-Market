package bo.umss.market.umss_market_api.infrastructure.persistence.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bo.umss.market.umss_market_api.infrastructure.persistence.entities.OrderItemEntity;

@Repository
public interface JpaOrderItemRepository extends JpaRepository<OrderItemEntity, UUID> {

    List<OrderItemEntity> findByOrderId(UUID orderId);

    void deleteByOrderId(UUID orderId);

    long countByOrderId(UUID orderId);
}