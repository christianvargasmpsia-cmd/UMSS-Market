package bo.umss.market.umss_market_api.infrastructure.persistence.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.OrderEntity;

@Repository
public interface JpaOrderRepository extends JpaRepository<OrderEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id")
    java.util.Optional<OrderEntity> findByIdForUpdate(@Param("id") UUID id);

    List<OrderEntity> findByUserId(UUID userId);

    List<OrderEntity> findByStatus(OrderStatus status);

    List<OrderEntity> findByUserIdAndStatus(UUID userId, OrderStatus status);
}